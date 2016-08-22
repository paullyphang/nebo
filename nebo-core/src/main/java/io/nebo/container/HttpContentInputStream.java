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

package io.nebo.container;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;


public class HttpContentInputStream extends ServletInputStream {

    private final Log logger = LogFactory.getLog(getClass());
    private AtomicBoolean closed;
    private final BlockingQueue<HttpContent> queue;
    private HttpContent current;

    public HttpContentInputStream() {
        this.closed = new AtomicBoolean();
        queue = new LinkedBlockingQueue<>();
    }


    public void addContent(HttpContent httpContent) {
        checkNotClosed();
        this.queue.offer(httpContent.retain());
    }

    @Override
    public boolean isFinished() {
        return current instanceof LastHttpContent && current.content().readableBytes() == 0;
    }

    @Override
    public boolean isReady() {
        return !queue.isEmpty() && current != null && current.content().readableBytes() > 0;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        logger.info(readListener + (readListener != null ? readListener.getClass() + "" : ""));
    }

    @Override
    public int read() throws IOException {
        if (isFinished()) {
            return -1;
        }
        if (current != null && current.content().readableBytes() > 0) {
            return current.content().readUnsignedByte();
        }
        current = queue.poll();
        return read();
    }

    private void checkNotClosed() {
        if (closed.get()) {
            throw new IllegalStateException("Stream is closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            queue.clear();
            current = null;
        }
    }


}
