/*
 * Copyright 2026 Ruslan Kashapov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package local.mylan.transport.smb.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import local.mylan.transport.smb.Utils;
import local.mylan.transport.smb.protocol.SmbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMB transport handler. Addresses MS-SMB & MS-SMB2 (#2.1 Transport).
 */
abstract class Smb2Codec<I, O> extends ByteToMessageCodec<O> {
    private static final Logger LOG = LoggerFactory.getLogger(Smb2Codec.class);
    private final AtomicReference<ByteBuf> aggregateRef = new AtomicReference<>();

    @Override
    protected void encode(final ChannelHandlerContext ctx, final O obj, final ByteBuf byteBuf)
        throws Exception {

        // 4 bytes lead starting with 0
        byteBuf.writeInt(0);
        // remember position, write message
        final int startIdx = byteBuf.writerIndex();
        // encode outbound
        try {
            encode(obj, byteBuf);
        } catch (Exception e) {
            throw new SmbException("Exception encoding obj", e);
        }
        // write actual message length (3 bytes) into lead
        final var msgLength = byteBuf.writerIndex() - startIdx;
        byteBuf.setBytes(startIdx - 3, Utils.toByteArray(msgLength, 3));
    }

    abstract void encode(O outObj, ByteBuf byteBuf);

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf byteBuf, final List<Object> list)
        throws Exception {

        if (aggregateRef.get() != null) {
            final var aggregateBuf = aggregateRef.get();
            if (byteBuf.readableBytes() <= aggregateBuf.writableBytes()) {
                aggregateBuf.writeBytes(byteBuf);
                if (aggregateBuf.writableBytes() == 0) {
                    // aggregation completed
                    list.add(decode(aggregateBuf));
                    aggregateBuf.release();
                    aggregateRef.set(null);
                }
                return;
            }
            LOG.warn("Inbound packet size {} is greater then expected remaining packet fragment {} -> " +
                "DROPPING aggregate buffer", byteBuf.readableBytes(), aggregateBuf.writableBytes());
            aggregateBuf.release();
            aggregateRef.set(null);
        }

        if (byteBuf.readableBytes() > 4) {
            final var type = byteBuf.readByte();
            if (type == 0) {
                final var length = Utils.readToIntValue(byteBuf, 3);
                if (length == 0) {
                    return;
                }
                if (length > byteBuf.readableBytes()) {
                    // packet fragment, starting aggregation in heap
                    aggregateRef.set(ctx.alloc().heapBuffer(length));
                    aggregateRef.get().writeBytes(byteBuf);
                    return;
                }

                final var pos = byteBuf.readerIndex();
                list.add(decode(byteBuf));
                // ensure reader pointer points to the end of message
                byteBuf.readerIndex(pos + length);
                return;
            }
            throw new SmbException("Unexpected packet lead type " + type);
        }
        throw new SmbException("Inbound packet is too short");
    }

    abstract I decode(ByteBuf byteBuf);
}
