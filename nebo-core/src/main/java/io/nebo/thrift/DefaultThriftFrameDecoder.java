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
package io.nebo.thrift;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import org.apache.log4j.Logger;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.protocol.TProtocolUtil;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TTransportException;

import java.util.List;

public class DefaultThriftFrameDecoder extends ByteToMessageDecoder {
    private final static Logger logger = Logger.getLogger(DefaultThriftFrameDecoder.class);
    public static final int MESSAGE_FRAME_SIZE = 4;
    private final TProtocolFactory inputProtocolFactory;
    private final int maxFrameSize = 64*1024*1024;
    public DefaultThriftFrameDecoder(TProtocolFactory inputProtocolFactory) {
        this.inputProtocolFactory = inputProtocolFactory;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out)
            throws Exception {
        Channel channel   = ctx.channel();
          if(buffer.readableBytes() <= 0){
            return ;
        }

        short firstByte = buffer.getUnsignedByte(0);
        if (firstByte >= 0x80) {
            ByteBuf messageBuffer = tryDecodeUnframedMessage(ctx, channel, buffer, inputProtocolFactory);

            if (messageBuffer == null) {
                return ;
        }

            // A non-zero MSB for the first byte of the message implies the message starts with a
            // protocol id (and thus it is unframed).
            out.add(new ThriftMessage(messageBuffer, ThriftTransportType.UNFRAMED));
        } else if (buffer.readableBytes() < MESSAGE_FRAME_SIZE) {
            // Expecting a framed message, but not enough bytes available to read the frame size
            return ;
        } else {
            ByteBuf messageBuffer = tryDecodeFramedMessage(ctx, channel, buffer, true);

            if (messageBuffer == null) {
                return ;
            }

            // Messages with a zero MSB in the first byte are framed messages
            out.add(new ThriftMessage(messageBuffer, ThriftTransportType.FRAMED));
        }
    }

    protected ByteBuf tryDecodeFramedMessage(ChannelHandlerContext ctx,
                                                   Channel channel,
                                                   ByteBuf buffer,
                                                   boolean stripFraming) {
        // Framed messages are prefixed by the size of the frame (which doesn't include the
        // framing itself).

        int messageStartReaderIndex = buffer.readerIndex();
        int messageContentsOffset;

        if (stripFraming) {
            messageContentsOffset = messageStartReaderIndex + MESSAGE_FRAME_SIZE;
        } else {
            messageContentsOffset = messageStartReaderIndex;
        }

        // The full message is larger by the size of the frame size prefix
        int messageLength = buffer.getInt(messageStartReaderIndex) + MESSAGE_FRAME_SIZE;
        int messageContentsLength = messageStartReaderIndex + messageLength - messageContentsOffset;

        if (messageContentsLength > maxFrameSize) {
            throw new TooLongFrameException("Maximum frame size of " + maxFrameSize +
                    " exceeded");
        }

        if (messageLength == 0) {
            // Zero-sized frame: just ignore it and return nothing
            buffer.readerIndex(messageContentsOffset);
            return null;
        } else if (buffer.readableBytes() < messageLength) {
            // Full message isn't available yet, return nothing for now
            return null;
        } else {
            // Full message is available, return it
            ByteBuf messageBuffer = extractFrame(buffer,
                    messageContentsOffset,
                    messageContentsLength);
            buffer.readerIndex(messageStartReaderIndex + messageLength);
            return messageBuffer;
        }
    }

    protected ByteBuf tryDecodeUnframedMessage(ChannelHandlerContext ctx,
                                                     Channel channel,
                                                     ByteBuf buffer,
                                                     TProtocolFactory inputProtocolFactory)
            throws TException {
        // Perform a trial decode, skipping through
        // the fields, to see whether we have an entire message available.

        int messageLength = 0;
        int messageStartReaderIndex = buffer.readerIndex();

        try {
            TNiftyTransport decodeAttemptTransport =
                    new TNiftyTransport(channel, buffer, ThriftTransportType.UNFRAMED);
            int initialReadBytes = decodeAttemptTransport.getReadByteCount();
            TProtocol inputProtocol =
                    inputProtocolFactory.getProtocol(decodeAttemptTransport);

            // Skip through the message
            inputProtocol.readMessageBegin();
            TProtocolUtil.skip(inputProtocol, TType.STRUCT);
            inputProtocol.readMessageEnd();

            messageLength = decodeAttemptTransport.getReadByteCount() - initialReadBytes;
        } catch (TTransportException | IndexOutOfBoundsException e) {
            // No complete message was decoded: ran out of bytes
            return null;
        } finally {
            if (buffer.readerIndex() - messageStartReaderIndex > maxFrameSize) {
                    throw new  TooLongFrameException("Maximum frame size of " + maxFrameSize + " exceeded");
            }

            buffer.readerIndex(messageStartReaderIndex);
        }

        if (messageLength <= 0) {
            return null;
        }

        // We have a full message in the read buffer, slice it off
        ByteBuf messageBuffer =
                extractFrame(buffer, messageStartReaderIndex, messageLength);
        buffer.readerIndex(messageStartReaderIndex + messageLength);
        return messageBuffer;
    }

    protected ByteBuf extractFrame(ByteBuf buffer, int index, int length) {
        // Slice should be sufficient here (and avoids the copy in LengthFieldBasedFrameDecoder)
        // because we know no one is going to modify the contents in the read buffers.
        return buffer.slice(index, length);
    }
}
