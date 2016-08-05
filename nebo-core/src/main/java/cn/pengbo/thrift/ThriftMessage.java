/*
 * Copyright (C) 2012-2013 Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.pengbo.thrift;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class ThriftMessage
{
    private final ByteBuf buffer;
    private final ThriftTransportType transportType;
    private long processStartTimeMillis;

    public ThriftMessage(ByteBuf buffer, ThriftTransportType transportType)
    {
        ByteBuf parent = Unpooled.buffer();
        parent.writeBytes(buffer);
        this.buffer = parent;
        this.transportType = transportType;
    }

    public ByteBuf getBuffer()
    {
        return buffer;
    }

    public ThriftTransportType getTransportType()
    {
        return transportType;
    }


    public Factory getMessageFactory()
    {
        return new Factory()
        {
            @Override
            public ThriftMessage create(ByteBuf messageBuffer)
            {
                return new ThriftMessage(messageBuffer, getTransportType());
            }
        };
    }

    /**
     * Standard Thrift clients require ordered responses, so even though Nifty can run multiple
     * requests from the same client at the same time, the responses have to be held until all
     * previous responses are ready and have been written. However, through the use of extended
     * protocols and codecs, a request can indicate that the client understands
     * out-of-order responses.
     *
     * @return {@code true} if ordered responses are required
     */
    public boolean isOrderedResponsesRequired()
    {
        return true;
    }

    public long getProcessStartTimeMillis() { return processStartTimeMillis; }

    public void setProcessStartTimeMillis(long processStartTimeMillis)
    {
        this.processStartTimeMillis = processStartTimeMillis;
    }

    public static interface Factory
    {
        public ThriftMessage create(ByteBuf messageBuffer);
    }
}
