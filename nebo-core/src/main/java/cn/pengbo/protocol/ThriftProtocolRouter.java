package cn.pengbo.protocol;

import cn.pengbo.container.NettyEmbeddedContext;
import cn.pengbo.thrift.DefaultThriftFrameDecoder;
import cn.pengbo.thrift.ThriftInboundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import org.apache.thrift.protocol.TBinaryProtocol;

/**
 * Created by pengbo on 2016/8/8.
 */
public class ThriftProtocolRouter implements ProtocolRouter {

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
}
