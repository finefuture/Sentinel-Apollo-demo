package com.example.demo.configuration;

import com.alibaba.csp.sentinel.datasource.AbstractDataSource;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.log.RecordLog;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.example.demo.enums.ConfigChangeType;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import java.util.Optional;

import static com.example.demo.configuration.SentinelConfigConstant.APOLLO_INIT_OPERATOR;
import static com.example.demo.configuration.SentinelConfigConstant.APOLLO_OPERATOR_KEY;

/**
 * A read-only {@code DataSource} with <a href="http://github.com/ctripcorp/apollo">Apollo</a> as its configuration
 * source.
 * <br />
 * When the rule is changed in Apollo, it will take effect in real time.
 *
 * @author Jason Song
 */
public class ApolloDataSource<T> extends AbstractDataSource<String, T> {

    private final Config config;
    private final String rulesKey;
    private final String defaultFlowRuleValue;
    private final SentinelConfigChangeSender configChangeSender;
    private final ConfigChangeType changeType;
    private ConfigChangeListener configChangeListener;

    /**
     * Constructs the Apollo data source
     *
     * @param namespaceName        the namespace name in Apollo, should not be null or empty
     * @param rulesKey         the flow rules key in the namespace, should not be null or empty
     * @param defaultFlowRuleValue the default flow rules value when the flow rules key is not found or any error
     *                             occurred
     * @param parser               the parser to transform string configuration to actual flow rules
     */
    public ApolloDataSource(String namespaceName, String rulesKey, String defaultFlowRuleValue,
                            Converter<String, T> parser, ConfigChangeType changeType) {
        super(parser);

        Preconditions.checkArgument(!Strings.isNullOrEmpty(namespaceName), "Namespace name could not be null or empty");
        Preconditions.checkArgument(!Strings.isNullOrEmpty(rulesKey), "FlowRuleKey could not be null or empty!");

        this.rulesKey = rulesKey;
        this.defaultFlowRuleValue = defaultFlowRuleValue;
        this.changeType = changeType;

        this.config = ConfigService.getConfig(namespaceName);
        this.configChangeSender = new SentinelConfigChangeSender();
        // TODO need retry?
        configChangeSender.sendChangeRequest(changeType, APOLLO_INIT_OPERATOR);
        initialize();

        RecordLog.info(String.format("Initialized rule for namespace: %s, flow rules key: %s",
            namespaceName, rulesKey));
    }

    private void initialize() {
        initializeConfigChangeListener();
        loadAndUpdateRules();
    }

    private void loadAndUpdateRules() {
        try {
            T newValue = loadConfig();
            if (newValue == null) {
                RecordLog.warn("[ApolloDataSource] WARN: rule config is null, you may have to check your data source");
            }
            getProperty().updateValue(newValue);
        } catch (Exception ex) {
            RecordLog.warn("[ApolloDataSource] Error when loading rule config", ex);
        }
    }

    private void initializeConfigChangeListener() {
        configChangeListener = changeEvent -> {
            ConfigChange change = changeEvent.getChange(rulesKey);
            //change is never null because the listener will only notify for this key
            if (change != null) {
                RecordLog.info("[ApolloDataSource] Received config changes: " + change.toString());
            }
            // TODO need retry?
            Optional.ofNullable(change.getOperator())
                    .filter(operator -> !config.getProperty(APOLLO_OPERATOR_KEY, "longqiang").equals(operator))
                    .ifPresent(operator -> configChangeSender.sendChangeRequest(changeType, operator));
            loadAndUpdateRules();
        };
        config.addChangeListener(configChangeListener, Sets.newHashSet(rulesKey));
    }

    @Override
    public String readSource() throws Exception {
        return config.getProperty(rulesKey, defaultFlowRuleValue);
    }

    @Override
    public void close() throws Exception {
        config.removeChangeListener(configChangeListener);
    }

}
