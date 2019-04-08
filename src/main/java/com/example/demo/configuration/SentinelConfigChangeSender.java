package com.example.demo.configuration;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.example.demo.enums.ConfigChangeType;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;

/**
 * Sentinel-Apollo 配置变化发送http请求至监听类
 *
 * @author longqiang
 */
public class SentinelConfigChangeSender extends SentinelHttpCommon {

    public boolean sendChangeRequest(ConfigChangeType changeType, String operator) throws Exception {
        if (StringUtil.isEmpty(consoleHost)) {
            return false;
        }
        RecordLog.info(String.format("[ApolloChangeRequest] Sending apolloChangeRequest to %s:%d", consoleHost, consolePort));

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost(consoleHost).setPort(consolePort)
                .setPath(String.format("/configChange/%s", changeType.getType()))
                .setParameter("app", AppNameUtil.getAppName())
                .setParameter("ip", TransportConfig.getHeartbeatClientIp())
                .setParameter("port", TransportConfig.getPort())
                .setParameter("operator", operator);

        HttpPut request = new HttpPut(uriBuilder.build());
        // Send heartbeat request.
        CloseableHttpResponse response = execute(request);
        response.close();
        return true;
    }

}
