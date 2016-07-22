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
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object obj)
            throws Exception {
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

}