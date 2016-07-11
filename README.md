# nebo
1.以netty为内核，基于springboot 的web container
2.支持单端口多协议（http thrift）


# 如何使用 

1.在项目中引入nebo
2.编写配置类，通知springboot 使用自定义servlet container

@Configuration
public class ContainerConfiguration {

    @Bean
    public NettyEmbeddedServletContainerFactory nettyEmbeddedServletContainerFactory() {
        return new NettyEmbeddedServletContainerFactory();
    }
}

3.支持thrift协议，把thrift的iface实现类在实现iface同时实现thriftendoint接口即可











