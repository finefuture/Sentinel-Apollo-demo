/*
 * Copyright 1999-2018 Alibaba Group Holding Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.demo.configuration;

import com.alibaba.csp.sentinel.Constants;
import com.alibaba.csp.sentinel.spi.SpiOrder;
import com.alibaba.csp.sentinel.transport.HeartbeatSender;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.HostNameUtil;
import com.alibaba.csp.sentinel.util.PidUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.fastjson.JSONObject;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.internals.ConfigManager;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.example.demo.configuration.SentinelConfigConstant.APOLLO_CONNECTION_TIMEOUT_KEY;
import static com.example.demo.configuration.SentinelConfigConstant.APOLLO_OPERATOR_KEY;
import static com.example.demo.configuration.SentinelConfigConstant.APOLLO_PORTAL_URL_KEY;
import static com.example.demo.configuration.SentinelConfigConstant.APOLLO_READ_TIMEOUT_KEY;
import static com.example.demo.configuration.SentinelConfigConstant.APOLLO_TOKEN_KEY;
import static com.example.demo.configuration.SentinelConfigConstant.CONFIG_NAMESPACE;
import static com.example.demo.configuration.SentinelConfigConstant.NAMESPACE;

/**
 * @author Eric Zhao
 * @author leyou
 */
@SpiOrder(SpiOrder.LOWEST_PRECEDENCE - 101)
public class HttpHeartbeatSender extends SentinelHttpCommon implements HeartbeatSender {

    private static final Logger logger = LoggerFactory.getLogger(SentinelConfigChangeSender.class);
    private final ConfigUtil configUtil;
    private final Config config;

    public HttpHeartbeatSender() {
        this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        this.config = ApolloInjector.getInstance(ConfigManager.class).getConfig(CONFIG_NAMESPACE);
        logger.info("[HttpHeartbeatSender] Sending first heartbeat to {}:{}", consoleHost, consolePort);
        try {
            sendHeartbeat();
        } catch (Exception e) {
            logger.error("[HttpHeartbeatSender] Sending first heartbeat error:{}", e);
        }
    }

    @Override
    public boolean sendHeartbeat() throws Exception {
        if (StringUtil.isEmpty(consoleHost)) {
            return false;
        }
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost(consoleHost).setPort(consolePort)
                .setPath("/registryV2/machine")
                .setParameter("v", Constants.SENTINEL_VERSION)
                .setParameter("info", generateParam());
        HttpGet request = new HttpGet(uriBuilder.build());
        // Send heartbeat request.
        CloseableHttpResponse response = execute(request);
        response.close();
        return true;
    }

    private String generateParam() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("app",  AppNameUtil.getAppName());
        jsonObject.put("version", String.valueOf(System.currentTimeMillis()));
        jsonObject.put("hostname", HostNameUtil.getHostName());
        jsonObject.put("ip", TransportConfig.getHeartbeatClientIp());
        jsonObject.put("port", TransportConfig.getPort());
        jsonObject.put("pid", String.valueOf(PidUtil.getPid()));
        jsonObject.put("namespace", NAMESPACE);
        jsonObject.put("env", configUtil.getApolloEnv().name());
        jsonObject.put("appId", configUtil.getAppId());
        jsonObject.put("clusterName", configUtil.getCluster());
        jsonObject.put("portalUrl", config.getProperty(APOLLO_PORTAL_URL_KEY, "http://localhost:10006"));
        jsonObject.put("token", config.getProperty(APOLLO_TOKEN_KEY, "7ab4d40bd0a4cd332a747dbddb2a0b47c82fcdf4"));
        jsonObject.put("connectTimeout", config.getProperty(APOLLO_CONNECTION_TIMEOUT_KEY, "1000"));
        jsonObject.put("readTimeout", config.getProperty(APOLLO_READ_TIMEOUT_KEY, "5000"));
        jsonObject.put("degradeRulesKey", RulesKeyUtils.getDegradeRulesKey());
        jsonObject.put("flowRulesKey", RulesKeyUtils.getFlowRulesKey());
        jsonObject.put("authorityRulesKey", RulesKeyUtils.getAuthorityRulesKey());
        jsonObject.put("systemRulesKey", RulesKeyUtils.getSystemRulesKey());
        jsonObject.put("paramFlowRulesKey", RulesKeyUtils.getParamFlowRulesKey());
        jsonObject.put("operator", config.getProperty(APOLLO_OPERATOR_KEY, "longqiang"));
        jsonObject.put("dataSourceType", SentinelConfigConstant.DATASOURCE_APOLLO);
        return jsonObject.toJSONString();
    }

    @Override
    public long intervalMs() {
        return 5000;
    }
}
