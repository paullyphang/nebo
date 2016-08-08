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

package cn.pengbo.container;

import com.google.common.base.Optional;
import com.google.common.net.MediaType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.util.concurrent.FastThreadLocal;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.base.Preconditions.checkState;
import static io.netty.handler.codec.http.HttpHeaders.Names.LOCATION;

/**
 * {@link javax.servlet.http.HttpServletResponse} wrapper for Netty's {@link io.netty.handler.codec.http.HttpResponse}.
 *
 * @author Danny Thomas
 */
public class NettyHttpServletResponse implements HttpServletResponse {
    private static final FastThreadLocal<DateFormat> FORMAT = new FastThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z");
        }
    };

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";

    private final NettyEmbeddedContext servletContext;
    private HttpResponse response;
    private HttpResponseOutputStream outputStream;
    private boolean usingOutputStream;
    private PrintWriter writer;
    private boolean committed;
    private List<Cookie> cookies;
    private String contentType;
    private String characterEncoding = DEFAULT_CHARACTER_ENCODING;
    private static  String DEFAULT_CONTENT_TYPE = "text/html;charset=" + DEFAULT_CHARACTER_ENCODING;
    private Locale locale;

    NettyHttpServletResponse(ChannelHandlerContext ctx, NettyEmbeddedContext servletContext, HttpResponse response) {
        this.servletContext = servletContext;
        this.response = response;
        this.outputStream = new HttpResponseOutputStream(ctx, this);
        this.writer = new PrintWriter(outputStream);
        cookies = new ArrayList<>();
    }

    /**
     * Get a Netty {@link io.netty.handler.codec.http.HttpResponse}, committing the {@link javax.servlet.http.HttpServletResponse}.
     */
    public HttpResponse getNettyResponse() {
        if (committed) {
            return response;
        }
        committed = true;
        HttpHeaders headers = response.headers();
        if (null != contentType) {
            String value = null == characterEncoding ? contentType : contentType + "; charset=" + characterEncoding;
            headers.set(HttpHeaders.Names.CONTENT_TYPE, value);
        }else {
            headers.set(HttpHeaders.Names.CONTENT_TYPE,
                    DEFAULT_CONTENT_TYPE);
        }

        headers.set(HttpHeaders.Names.DATE, new Date());
        headers.set(HttpHeaders.Names.SERVER, servletContext.getServerInfoAscii());

        for(Cookie ck : cookies) {
            io.netty.handler.codec.http.cookie.Cookie  nettyCookie = new DefaultCookie(ck.getName(),ck.getValue());
            nettyCookie.setDomain(ck.getDomain());
            nettyCookie.setPath(ck.getPath());
            if( ck.getMaxAge()  > 0) {
                nettyCookie.setMaxAge(ck.getMaxAge());
            }
//            response.headers().add("Set-Cookie", nettyCookie);
           response.headers().add("Set-Cookie", ServerCookieEncoder.STRICT.encode(nettyCookie));
        }
        return response;
    }

    @Override
    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    @Override
    public boolean containsHeader(String name) {
        return response.headers().contains(name);
    }

    @Override
    public String encodeUrl(String url) {
        try {
            return URLEncoder.encode(url,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public String encodeURL(String url) {
        return encodeUrl(url);
    }

    @Override
    public String encodeRedirectURL(String url) {
        return encodeUrl(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        checkNotCommitted();
        response.setStatus(new HttpResponseStatus(sc, msg));
    }

    @Override
    public void sendError(int sc) throws IOException {
        checkNotCommitted();
        response.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    public void sendRedirect(String location) throws IOException {
//        checkNotCommitted();
        // TODO implement
        setStatus(SC_FOUND);
        setHeader(LOCATION, location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        response.headers().set(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        response.headers().add(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        if (setHeaderField(name, value)) {
            return;
        }
        response.headers().set(name, value);
    }

    private boolean setHeaderField(String name, String value) {
        // Handle headers that shouldn't be set directly, and are instance fields
        char c = name.charAt(0);
        if ('C' == c || 'c' == c) {
            if (HttpHeaders.Names.CONTENT_TYPE.equalsIgnoreCase(name)) {
                setContentType(value);
                return true;
            }
            // TODO Content-Language?
        }
        return false;
    }

    @Override
    public void addHeader(String name, String value) {
        if (setHeaderField(name, value)) {
            return;
        }
        response.headers().add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        response.headers().set(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        response.headers().add(name, value);
    }

    @Override
    public void setStatus(int sc) {
        response.setStatus(HttpResponseStatus.valueOf(sc));
    }

    @Override
    public void setStatus(int sc, String sm) {
        response.setStatus(new HttpResponseStatus(sc, sm));
    }

    @Override
    public int getStatus() {
        return response.status().code();
    }

    @Override
    public String getHeader(String name) {
        return response.headers().get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return response.headers().getAll(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return response.headers().names();
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
//        checkState(!hasWriter(), "getWriter has already been called for this response");
//        usingOutputStream = true;
        return outputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
////        checkState(!usingOutputStream, "getOutputStream has already been called for this response");
//        if (!hasWriter()) {
//            writer = new PrintWriter(outputStream);
//        }
        return writer;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        if (hasWriter()) {
            return;
        }
        characterEncoding = charset;
    }

    @Override
    public void setContentType(String type) {
        if (isCommitted()) {
            return;
        }
//        if (hasWriter()) {
//            return;
//        }
        if (null == type) {
            contentType = null;
            return;
        }
        MediaType mediaType = MediaType.parse(type);
        Optional<Charset> charset = mediaType.charset();
        if (charset.isPresent()) {
            setCharacterEncoding(charset.get().name());
        }
        contentType = mediaType.type() + '/' + mediaType.subtype();
    }

    private boolean hasWriter() {
        return null != writer;
    }

    @Override
    public void setContentLength(int len) {
        HttpHeaders.setContentLength(response, len);
    }

    @Override
    public void setContentLengthLong(long len) {
        HttpHeaders.setContentLength(response, len);
    }

    @Override
    public void setBufferSize(int size) {
        checkNotCommitted();
        outputStream.setBufferSize(size);
    }

    @Override
    public int getBufferSize() {
        return outputStream.getBufferSize();
    }

    @Override
    public void flushBuffer() throws IOException {
        checkNotCommitted();
        outputStream.flush();
    }

    @Override
    public void resetBuffer() {
        checkNotCommitted();
        outputStream.resetBuffer();
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    void checkNotCommitted() {
        checkState(!committed, "Cannot perform this operation after response has been committed");
    }

    @Override
    public void reset() {
        resetBuffer();
        usingOutputStream = false;
        writer = null;
    }

    @Override
    public void setLocale(Locale loc) {
        locale = loc;
    }

    @Override
    public Locale getLocale() {
        return null == locale ? DEFAULT_LOCALE : locale;
    }
}
