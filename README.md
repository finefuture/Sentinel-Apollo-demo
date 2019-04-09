# Sentinel-Apollo-demo

##  概述

Sentinel-Apollo-demo 是我fork的Sentinel分支dashboard/integration-ApolloConfig重新构造了一套适配阿波罗配置中心规则变更的推送、拉取的demo示例，在 Sentinel 控制台上，我们可以配置规则并实时查看效果。

##  重写的类
1、ApolloDataSource :

主要添加了一个ApolloDataSource初始化时强制刷新控制台该app配置在apollo的规则，存储在InMemoryStore的Map的里面，还有一个apollo的config change事件时通过operator判断是否由Sentinel控制台产生的，如果不是那么证明是通过Apollo控制台强行修改的，这样的话需要触发Sentinel控制台的规则强制刷新(注:想使用这个功能需要修改apollo源码，我fork了apollo 1.4版本的源码进行了修改，分支是feature-addOperator，需要自行打包构建，项目中的话需要将改造的apollo-client和apollo-core放入maven私库引入或者直接jar包引入)。

2、HeartBeatSenderInitFunc :

修改了ServiceLoader加载的HeartBeatSender，改为由本地重写的HttpHeartBeatSender，这个是需要配置在META-INF.services的com.alibaba.csp.sentinel.init.InitFunc文件的

3、HttpHeartBeatSender :
    
修改了发送的机器信息的内容（MachineInfo），添加了Apollo的信息比如appId，env，portalUrl等，这些信息是为了让Sentinel控制台在修改规则时能通过ApolloClientOpenApi发送请求修改apollo的相关配置项

##  必须的类
1、RulesKeyUtils :

这个类的主要作用是获取每个应用专属的对应在Apollo配置中心的唯一key，可以通过启动时添加系统变量比如-Dsentinel.apollo.flowRules(详见：[RulesKeyUtils](https://github.com/finefuture/Sentinel-Apollo-demo/blob/master/src/main/java/com/example/demo/configuration/RulesKeyUtils.java))来设置，它默认的是取比如appName+ip+port+flowsKey(port需要通过-Dserver.port来配置)

2、SentinelConfigChangeSender :

这个类就是发送强制刷新规则请求的Http Send类

3、SentinelConfigConstant

通用常量

4、SentinelHttpCommon

HttpHeartBeatSender和SentinelConfigChangeSender抽取出来的公共类

5、ConfigChangeType

规则变化类型枚举

##   jar包说明
因为这个demo涉及到apollo源码的修改，因此提供了三个jar包

1、apollo-client jar和apollo-core jar是直接引入到项目中的

2、apollo-configservice jar是apollo启动的jar

##   demo启动说明
启动参数 ： -Ddev_meta=(your apollo dev metaserver address)  -Denv=DEV -Dapp.id=(application apollo appId) -Dserver.port=(your application server port)

apollo配置：需要建立一个公共配置，namespace为Sentinel-Common，配置如下：

portalUrl = (your portal url)<br>
apolloApiClientToken = (your third party client token)<br>
apolloApiClientConnectTimeout = 2000<br>
apolloApiClientReadTimeout = 8000<br>
sentinelOperator = longqiang<br>

sentinelOperator和apolloApiClientToken需要在apollo portal页面的管理员工具目录下的开放平台授权页面进行设置<br>
注：sentinelOperator应该与[ApolloDataSource](https://github.com/finefuture/Sentinel-Apollo-demo/blob/master/src/main/java/com/example/demo/configuration/ApolloDataSource.java) 里面的<br>
```java
String operator = change.getOperator();
if (!config.getProperty(APOLLO_OPERATOR_KEY, "longqiang").equals(operator)) {
    try {
        configChangeSender.sendChangeRequest(changeType, operator);
    } catch (Exception e) {
        RecordLog.warn("[ApolloDataSource] Error when sendChangeRequest", e);
    }
}
```
这个代码中的默认值"longqiang"一致<br>
以及[HttpHeartbeatSender](https://github.com/finefuture/Sentinel-Apollo-demo/blob/master/src/main/java/com/example/demo/configuration/HttpHeartbeatSender.java)里面的<br>
```java
uriBuilder.setScheme("http").setHost(consoleHost).setPort(consolePort)
            .setPath("/registryV2/machine") 
            .setParameter("app", AppNameUtil.getAppName())
            .setParameter("v", Constants.SENTINEL_VERSION)
            .setParameter("version", String.valueOf(System.currentTimeMillis()))
            .setParameter("hostname", HostNameUtil.getHostName())
            .setParameter("ip", TransportConfig.getHeartbeatClientIp())
            .setParameter("port", TransportConfig.getPort())
            .setParameter("pid", String.valueOf(PidUtil.getPid()))
            .setParameter("namespace", NAMESPACE)
            .setParameter("env", configUtil.getApolloEnv().name())
            .setParameter("appId", configUtil.getAppId())
            .setParameter("clusterName", configUtil.getCluster())
            .setParameter("portalUrl", config.getProperty(APOLLO_PORTAL_URL_KEY, "your portal url"))
            .setParameter("token", config.getProperty(APOLLO_TOKEN_KEY, "your third party token"))
            .setParameter("connectTimeout", config.getProperty(APOLLO_CONNECTION_TIMEOUT_KEY, "1000"))
            .setParameter("readTimeout", config.getProperty(APOLLO_READ_TIMEOUT_KEY, "5000"))
            .setParameter("degradeRulesKey", RulesKeyUtils.getDegradeRulesKey())
            .setParameter("flowRulesKey", RulesKeyUtils.getFlowRulesKey())
            .setParameter("authorityRulesKey", RulesKeyUtils.getAuthorityRulesKey())
            .setParameter("systemRulesKey", RulesKeyUtils.getSystemRulesKey())
            .setParameter("paramFlowRulesKey", RulesKeyUtils.getParamFlowRulesKey())
            .setParameter("operator", config.getProperty(APOLLO_OPERATOR_KEY, "longqiang"));
```
默认配置应该与上述一致

##   操作图示

![apollo rules配置展示图](/home/picture/1.png)
![apollo Sentinel-Common配置展示图](/home/picture/2.png)
![sentinel dashboard rules规则展示图](/home/picture/3.png)
说明：<br>
强制刷新按钮会从数据源直接拉取最新的规则配置存储在控制台所在程序的内存中，而刷新按钮的话是从内存中读取配置
![apollo Sentinel-Common配置展示图](/home/picture/4.png)
说明：<br>
当点击保存的时候，sentinel-dashboard程序会做以下几件事：<br>
1、将规则数据存储到内存中<br>
2、将数据推送到数据源<br>
而应用程序会做以下几件事：<br>
1、通过apollo的ConfigChangeListener监听到了配置变化事件
2、对比operator，如果是apollo portal页面引发的配置变化，那么发送通知给sentinel-dashboard，dashboard就将apollo拉取最新的规则配置
3、更新本地规则
