package com.example.demo.configuration;

import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.util.AppNameUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.example.demo.enums.ConfigChangeType;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Sentinel-Apollo 配置变化发送http请求至监听类
 *
 * @author longqiang
 */
public class SentinelConfigChangeSender extends SentinelHttpCommon {

    private static final Logger logger = LoggerFactory.getLogger(SentinelConfigChangeSender.class);

    public boolean sendChangeRequest(ConfigChangeType changeType, String operator) {
        if (StringUtil.isEmpty(consoleHost)) {
            return false;
        }
        logger.info("[ApolloChangeRequest] Sending apolloChangeRequest to {}:{}", consoleHost, consolePort);

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http").setHost(consoleHost).setPort(consolePort)
                    .setPath(String.format("/configChange/%s", changeType.getType()))
                    .setParameter("app", AppNameUtil.getAppName())
                    .setParameter("ip", TransportConfig.getHeartbeatClientIp())
                    .setParameter("port", TransportConfig.getPort())
                    .setParameter("operator", operator);

        HttpPut request;
        try {
            request = new HttpPut(uriBuilder.build());
            // Send heartbeat request.
            CloseableHttpResponse response = execute(request);
            response.close();
        } catch (URISyntaxException | IOException e) {
            logger.error("Error when sendChangeRequest, {}", e);
            return false;
        }
        return true;
    }

}
