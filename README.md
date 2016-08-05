
## nebo ##
1. 支持多协议springboot  container

2. 支持单端口多协议（http thrift hessian）


### 如何使用  ###
1. 在项目中引入nebo

2. 编写配置类定义NettyEmbeddedServletContainerFactory，用于通知springboot启用自定义servlet container （默认支持http协议）

3. 发布thrift服务，把iface实现类同时实现ThriftEndpoint接口即可 （支持thrift协议）

4. 发布hessian服务，把需要暴露的类使用HessianEndpoint修饰即可 （支持hessian协议）


 
### 性能  ###

测试工具 JMETER

1. HTTP协议 ，简单ECHO测试

   200并发下，nebo吞吐量约为10000   tomcat吞吐量约为12000

   400并发下，nebo吞吐量约为11000   tomcat吞吐量约为10000

 


### 接下来计划  ###
1. 优化项目结构，抽象模块，整理第三方协议通用接口
2. 优化nebo性能
