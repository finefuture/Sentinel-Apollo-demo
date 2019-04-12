package com.example.demo.configuration;

import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.init.InitFunc;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRule;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.example.demo.enums.ConfigChangeType;

import java.util.List;

/**
 * init sentinel DataSource
 *
 * @author longqiang
 */
public class DataSourceInitFunc implements InitFunc {

    @Override
    public void init() {
        final String defaultFlowRules = "[]";

        //flow rule
        ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new ApolloDataSource<>(SentinelConfigConstant.NAMESPACE,
                RulesKeyUtils.getFlowRulesKey(), defaultFlowRules, source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
        }), ConfigChangeType.FLOW_RULE);
        FlowRuleManager.register2Property(flowRuleDataSource.getProperty());

        //system rule
        ReadableDataSource<String, List<SystemRule>> systemRuleDataSource = new ApolloDataSource<>(SentinelConfigConstant.NAMESPACE,
                RulesKeyUtils.getSystemRulesKey(), defaultFlowRules, source -> JSON.parseObject(source, new TypeReference<List<SystemRule>>() {
        }), ConfigChangeType.SYSTEM_RULE);
        SystemRuleManager.register2Property(systemRuleDataSource.getProperty());

        //authority rule
        ReadableDataSource<String, List<AuthorityRule>> authorityRuleDataSource = new ApolloDataSource<>(SentinelConfigConstant.NAMESPACE,
                RulesKeyUtils.getAuthorityRulesKey(), defaultFlowRules, source -> JSON.parseObject(source, new TypeReference<List<AuthorityRule>>() {
        }), ConfigChangeType.AUTHORITY_RULE);
        AuthorityRuleManager.register2Property(authorityRuleDataSource.getProperty());

        //param flow rule
        ReadableDataSource<String, List<ParamFlowRule>> paramFlowRuleDataSource = new ApolloDataSource<>(SentinelConfigConstant.NAMESPACE,
                RulesKeyUtils.getParamFlowRulesKey(), defaultFlowRules, source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {
        }), ConfigChangeType.PARAM_FLOW_RULE);
        ParamFlowRuleManager.register2Property(paramFlowRuleDataSource.getProperty());

        //degrade rule
        ReadableDataSource<String, List<DegradeRule>> degradeRuleDataSource = new ApolloDataSource<>(SentinelConfigConstant.NAMESPACE,
                RulesKeyUtils.getDegradeRulesKey(), defaultFlowRules, source -> JSON.parseObject(source, new TypeReference<List<DegradeRule>>() {
        }), ConfigChangeType.DEGRADE_RULE);
        DegradeRuleManager.register2Property(degradeRuleDataSource.getProperty());
    }

}
