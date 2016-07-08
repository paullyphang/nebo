package cn.fabao;



import cn.fabao.http.ServletContentHandler;
import cn.fabao.http.NettyEmbeddedContext;
import cn.fabao.http.RequestDispatcherHandler;
import cn.fabao.thrift.DefaultThriftFrameDecoder;
import cn.fabao.thrift.ThriftInboundHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.thrift.protocol.TBinaryProtocol;
import java.net.InetSocketAddress;

/**
 * Created by pengbo on 2016/6/28.
 */
public class DispatcherInbound extends ChannelInboundHandlerAdapter {

    private final InetSocketAddress address;
    private final NettyEmbeddedContext context;
    private final RequestDispatcherHandler requestDispatcherHandler;
    public DispatcherInbound(InetSocketAddress address, NettyEmbeddedContext context) {
        this.address = address;
        this.context = context;
        this.requestDispatcherHandler = new RequestDispatcherHandler(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buffer = (ByteBuf) msg;
        final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
        final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);
        if(isHttp(magic1,magic2)) {
            switchToHttp(ctx);
        }else {
            switchToThrift(ctx);
        }
        ctx.fireChannelRead(msg);
    }

    private void switchToHttp(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast("codec", new HttpServerCodec(4096, 8192, 8192, false));
        p.addLast(new HttpObjectAggregator(65536));
        p.addLast(new ChunkedWriteHandler());
        p.addLast("servletInput", new ServletContentHandler(context,ctx.channel()));
        p.addLast(new DefaultEventExecutorGroup(50), "filterChain", requestDispatcherHandler);
        p.remove(this);
    }



    private void switchToThrift(ChannelHandlerContext ctx) {
        ChannelPipeline p = ctx.pipeline();
        p.addLast(new DefaultThriftFrameDecoder(new TBinaryProtocol.Factory(true, true)));
        p.addLast(new ThriftInboundHandler(context));
        p.remove(this);
    }

    private static boolean isHttp(int magic1, int magic2) {
        return
                magic1 == 'G' && magic2 == 'E' || // GET
                        magic1 == 'P' && magic2 == 'O' || // POST
                        magic1 == 'P' && magic2 == 'U' || // PUT
                        magic1 == 'H' && magic2 == 'E' || // HEAD
                        magic1 == 'O' && magic2 == 'P' || // OPTIONS
                        magic1 == 'P' && magic2 == 'A' || // PATCH
                        magic1 == 'D' && magic2 == 'E' || // DELETE
                        magic1 == 'T' && magic2 == 'R' || // TRACE
                        magic1 == 'C' && magic2 == 'O';   // CONNECT
    }

}
