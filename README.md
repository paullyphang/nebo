
## nebo ##
1. 支持多协议springboot  container

2. 支持单端口多协议（http thrift hessian）


### 如何使用  ###
1. 在项目中引入nebo

2. 编写配置类定义NettyEmbeddedServletContainerFactory，用于通知springboot启用自定义servlet container （默认支持http协议）

3. 发布thrift服务，把iface实现类使用@ThriftEndpoint修饰即可 （支持thrift协议）

4. 发布hessian服务，把需要暴露的类使用@HessianEndpoint修饰即可 （支持hessian协议）


 
### 性能  ###
测试工具 JMETER

1. HTTP协议 ，简单ECHO测试

   200并发下，nebo吞吐量约为10000   tomcat吞吐量约为12000

   400并发下，nebo吞吐量约为11000   tomcat吞吐量约为10000



### 接下来2017计划  ###

1. 由于hessina基于http协议,没法通过mic路由处理，重构讲取消hessian, 所以计划在protobuf基础,设计一套nebo协议
   初步协议格式 :    |mic|seqid|size|service|protobuf|
2. 设计服务治理模块和监控模块，熔断模块 (其实应该是打算去研究Netflix那套东西，能整合尽量整合)
3. 开发关于nebo-db模块，简单实现读写分类及客户端水平分平功能，如果可以话希望实现一套基于nio/aio的jdbc实现

待续。。。。。

PS: 年纪不小 不懂的东西还有很多！！！！！！！2017年还是要好好努力
