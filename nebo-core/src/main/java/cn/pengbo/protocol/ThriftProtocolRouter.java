package cn.pengbo.protocol;

import cn.pengbo.container.ServletContentHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

/**
 * Created by pengbo on 2016/8/8.
 */
public class ThriftProtocolRouter implements ProtocolRouter {


}
