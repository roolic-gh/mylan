/*
 * Copyright 2025 Ruslan Kashapov
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
package local.transport.netty.smb.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.SmbException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMB transport handler. Addresses MS-SMB & MS-SMB2 (#2.1 Transport).
 */
abstract class SmbCodec<I, O> extends ByteToMessageCodec<O> {

    private static final Logger LOG = LoggerFactory.getLogger(SmbCodec.class);

    @Override
    protected void encode(final ChannelHandlerContext ctx, final O outObj, final ByteBuf byteBuf)
        throws Exception {

        // 4 bytes lead starting with 0
        byteBuf.writeInt(0);
        // remember position, write message
        final int msgStartIdx = byteBuf.writerIndex();
        // encode outbound
        encode(outObj, byteBuf);
        // write actual message leng into lead
        final var msgLength = byteBuf.writerIndex() - msgStartIdx;
        byteBuf.setBytes(msgStartIdx - 3, Utils.toByteArray(msgLength, 3));
    }

    abstract void encode(O outObj, ByteBuf byteBuf);

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf byteBuf, final List<Object> list)
        throws Exception {

        while (byteBuf.readableBytes() > 4) {
            final var type = byteBuf.readByte();
            if (type == 0) {
                final var length = Utils.readToIntValue(byteBuf, 3);
                if (length > byteBuf.readableBytes()) {
                    throw new SmbException(
                        "Described SMB message length (%d) is greater then remaining packet size (%d)"
                            .formatted(length, byteBuf.readableBytes()));
                }
                list.add(decode(byteBuf));
                continue;
            }
            throw new SmbException("Unexpected packet lead type " + type);
        }
    }

    abstract I decode(ByteBuf byteBuf);
}
