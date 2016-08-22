package io.nebo.protocol;

import io.nebo.container.NettyEmbeddedContext;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by pengbo on 2016/8/8.
 */
public interface ProtocolRouter {

    public boolean isProtocol(ByteBuf buffer);

    public void setRounter(ChannelHandlerContext ctx,NettyEmbeddedContext context);

}
