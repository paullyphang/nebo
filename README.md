## nebo ##
1. 以netty为内核，基于springboot 的servlet container

2. 支持单端口多协议（http thrift）

### 如何使用  ###
1. 在项目中引入nebo

2. 编写配置类定义NettyEmbeddedServletContainerFactory，用于通知springboot启用自定义servlet container （默认支持http协议）

3. 发布thrift服务，把iface实现类同时实现ThriftEndpoint接口即可 （支持thrift协议）


### 接下来计划  ###
1. 优化项目结构，抽象模块，整理第三方协议通用接口
