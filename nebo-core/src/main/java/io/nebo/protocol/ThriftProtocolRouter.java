package io.nebo.protocol;

import io.nebo.container.NettyEmbeddedContext;
import io.nebo.thrift.DefaultThriftFrameDecoder;
import io.nebo.thrift.ThriftEndpoint;
import io.nebo.thrift.ThriftInboundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Created by pengbo on 2016/8/8.
 */
public class ThriftProtocolRouter implements ProtocolRouter {

    private final Log logger = LogFactory.getLog(getClass());

    @Override
    public boolean isProtocol(ByteBuf buffer) {
        short firstByte = buffer.getUnsignedByte(0);
        return firstByte >= 0x80;
    }

    @Override
    public void setRounter(ChannelHandlerContext ctx,NettyEmbeddedContext context) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast(new DefaultThriftFrameDecoder(new TBinaryProtocol.Factory(true, true)));
        p.addLast(new ThriftInboundHandler(context));
    }

    @Override
    public void init(NettyEmbeddedContext context) {
        TMultiplexedProcessor tProcessor = new TMultiplexedProcessor();
        context.setProcessor(tProcessor);
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.findWebApplicationContext(context);
        String[] strarr = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(webApplicationContext, Object.class);
        for (String s :strarr){
            Object target = webApplicationContext.getBean(s);
            ThriftEndpoint thriftEndpointAnnotation = target.getClass().getAnnotation(ThriftEndpoint.class);
            if(thriftEndpointAnnotation!=null){
                try {
                    Class targetInterface = target.getClass().getInterfaces()[0];
                    Class processorClass = Class.forName(targetInterface.getName().split("\\$")[0] + "$Processor");
                    TProcessor p = (TProcessor) processorClass.getDeclaredConstructors()[0].newInstance(target);
                    if(StringUtils.isNotBlank(thriftEndpointAnnotation.serviceName())){
                        s = thriftEndpointAnnotation.serviceName();
                    }
                    System.out.println(thriftEndpointAnnotation.serviceName());
                    logger.info("registerProcessorName : " + s + " registerProcessorClass: " + p.getClass());
                    tProcessor.registerProcessor(s,p);
                } catch (Exception e) {
                    logger.error("registerProcessor error : " + e.getMessage() , e);
                }
            }

        }
    }
}
