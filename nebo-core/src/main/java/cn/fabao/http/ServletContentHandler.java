/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.fabao.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * {@link io.netty.channel.ChannelInboundHandler} responsible for initial request handling, and getting received
 * {@link io.netty.handler.codec.http.HttpContent} messages to the {@link HttpContentInputStream} for the request.
 */
public class ServletContentHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger log = LoggerFactory.getLogger(ServletContentHandler.class);
    private final NettyEmbeddedContext servletContext;
    private HttpContentInputStream inputStream; // FIXME this feels wonky, need a better approach
    private HttpServletRequest servletRequest;

    public ServletContentHandler(NettyEmbeddedContext servletContext, Channel channel) {
        this.servletContext = servletContext;
        inputStream = new HttpContentInputStream();
    }

    private HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);
    private HttpPostRequestDecoder decoder;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        inputStream = new HttpContentInputStream(ctx.channel());
        log.info("channelActive >>>>>>>>>>>>>>>>>>>>> ");
   }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            log.info("uri" + request.getUri());
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, false);
            NettyHttpServletResponse servletResponse = new NettyHttpServletResponse(ctx, servletContext, response);
            servletRequest = new NettyHttpServletRequest(ctx, servletContext, request, inputStream, servletResponse);
            if (HttpMethod.GET.equals(request.getMethod())) {
                HttpHeaders.setKeepAlive(response, HttpHeaders.isKeepAlive(request));
                if (HttpHeaders.is100ContinueExpected(request)) {
                    ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE), ctx.voidPromise());
                }
                ctx.fireChannelRead(servletRequest);
            } else if (HttpMethod.POST.equals(request.getMethod())) {
                decoder = new HttpPostRequestDecoder(factory, request);
            }
        }


        if (decoder != null && msg instanceof HttpContent) {
            HttpContent chunk = (HttpContent) msg;
            log.info("HttpContent" + chunk.content().readableBytes());
            inputStream.addContent(chunk);
            List<InterfaceHttpData> interfaceHttpDatas = decoder.getBodyHttpDatas();

            for (InterfaceHttpData data : interfaceHttpDatas) {
                try {
                    if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        Attribute attribute = (Attribute) data;
                        Map<String, String[]> params = servletRequest.getParameterMap();
                        HttpRequestUtils.setParamMap(attribute.getName(), attribute.getValue(), params);
                    }
                } finally {
                   // data.release();
                }
            }

        }

        if (decoder != null && msg instanceof LastHttpContent) {
            ctx.fireChannelRead(servletRequest);
            reset();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        inputStream.close();
    }

    private void reset() {
        decoder.destroy();
        decoder = null;
    }
}