package com.example.demo.configuration;

import com.alibaba.csp.sentinel.log.RecordLog;
import com.alibaba.csp.sentinel.transport.config.TransportConfig;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.alibaba.csp.sentinel.util.function.Tuple2;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel-Apollo 配置变化发送http请求至监听类
 *
 * @author longqiang
 */
public class SentinelHttpCommon {

    private static final CloseableHttpClient CLIENT = HttpClients.createDefault();

    private static final int TIMEOUT_MS = 3000;
    private static final RequestConfig REQUEST_CONFIG = RequestConfig.custom()
            .setConnectionRequestTimeout(TIMEOUT_MS)
            .setConnectTimeout(TIMEOUT_MS)
            .setSocketTimeout(TIMEOUT_MS)
            .build();

    protected static String consoleHost;
    protected static int consolePort;

    static {
        List<Tuple2<String, Integer>> dashboardList = parseDashboardList();
        if (dashboardList == null || dashboardList.isEmpty()) {
            RecordLog.info("[NettyHttpHeartbeatSender] No dashboard available");
        } else {
            consoleHost = dashboardList.get(0).r1;
            consolePort = dashboardList.get(0).r2;
            RecordLog.info("[NettyHttpHeartbeatSender] Dashboard address parsed: <" + consoleHost + ':' + consolePort + ">");
        }
    }

    protected static List<Tuple2<String, Integer>> parseDashboardList() {
        List<Tuple2<String, Integer>> list = new ArrayList<>();
        try {
            String ipsStr = TransportConfig.getConsoleServer();
            if (StringUtil.isBlank(ipsStr)) {
                RecordLog.warn("[NettyHttpHeartbeatSender] Dashboard server address is not configured");
                return list;
            }

            for (String ipPortStr : ipsStr.split(",")) {
                if (ipPortStr.trim().length() == 0) {
                    continue;
                }
                ipPortStr = ipPortStr.trim();
                if (ipPortStr.startsWith("http://")) {
                    ipPortStr = ipPortStr.substring(7);
                }
                if (ipPortStr.startsWith(":")) {
                    continue;
                }
                String[] ipPort = ipPortStr.trim().split(":");
                int port = 80;
                if (ipPort.length > 1) {
                    port = Integer.parseInt(ipPort[1].trim());
                }
                list.add(Tuple2.of(ipPort[0].trim(), port));
            }
        } catch (Exception ex) {
            RecordLog.warn("[NettyHttpHeartbeatSender] Parse dashboard list failed, current address list: " + list, ex);
            ex.printStackTrace();
        }
        return list;
    }

    protected CloseableHttpResponse execute(HttpRequestBase request) throws IOException {
        request.setConfig(REQUEST_CONFIG);
        return CLIENT.execute(request);
    }
}
