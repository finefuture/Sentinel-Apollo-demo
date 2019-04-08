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

