package cn.fabao.thrift;

import cn.fabao.http.NettyEmbeddedContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.thrift.TMultiplexedProcessor;
import org.apache.thrift.TProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

public class ThriftInboundHandler extends ChannelInboundHandlerAdapter {

    private static Log log = LogFactory.getLog(ThriftInboundHandler.class);
    private final NettyEmbeddedContext context;
    private TMultiplexedProcessor processor;
    public ThriftInboundHandler(NettyEmbeddedContext context) {
        this.context = context;
        this.processor = context.getProcessor();
        registerSrv();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj)
            throws Exception {

//        TProcessor processor1 = new HelloWorld.Processor(new HelloWorldImpl());
//        TProcessor processor2 = new BaseResourcesService.Processor(new BaseResourcesServiceImpl());
//        processor.registerProcessor("helloWorldImpl",processor1);
//        processor.registerProcessor("baseResourcesServiceImpl",processor2);
//        TProcessor tMultiplexedProcessor = new TMultiplexedProcessor
        ThriftMessage msg = (ThriftMessage) obj;
        ByteBuf inBuf = msg.getBuffer();
        TNiftyTransport decodeAttemptTransport =
                new TNiftyTransport(ctx.channel(),inBuf,msg.getTransportType());
        decodeAttemptTransport.setOutputBuffer(ctx.alloc().buffer());
        TProtocol inProtocol = new TBinaryProtocol(decodeAttemptTransport);
        TProtocol outProtocol = new TBinaryProtocol(decodeAttemptTransport);
        boolean flag = processor.process(inProtocol, outProtocol);
        ctx.writeAndFlush(decodeAttemptTransport.getOutputBuffer());
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        log.info("channelReadComplete ....");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(cause.getMessage());
        ctx.close();
    }

//    public static void main(String[] args) {
//        AtomicBoolean b = new AtomicBoolean(true);
//
//        System.out.println(b.compareAndSet(false,true));
//        System.out.println(b.compareAndSet(true,false));
//    }

        private void registerSrv() {
        TMultiplexedProcessor tProcessor = new TMultiplexedProcessor();
        context.setProcessor(tProcessor);
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.findWebApplicationContext(context);
        String[] strarr = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(webApplicationContext,Object.class);
        for (String s :strarr){
           // logger.info("beans >>>>>>>>>>>>>>> " + s);
            Object target = webApplicationContext.getBean(s);
            ThriftEndpoint thriftEndpointAnnotation = target.getClass().getAnnotation(ThriftEndpoint.class);
            if(thriftEndpointAnnotation!=null){
                try {
                    Class targetInterface = target.getClass().getInterfaces()[0];
                    Class processorClass = Class.forName(targetInterface.getName().split("\\$")[0] + "$Processor");
                    TProcessor p = (TProcessor) processorClass.getDeclaredConstructors()[0].newInstance(target);
//                    if(StringUtils.isNotBlank(thriftEndpointAnnotation.serviceName())){
//                        s = thriftEndpointAnnotation.serviceName();
//                    }
                    log.info("registerProcessorName : " + s + " registerProcessorClass: " + p.getClass());
                    tProcessor.registerProcessor(s,p);
                } catch (Exception e) {
                    log.error("registerProcessor error : " + e.getMessage() , e);
                }
            }

        }
    }

}