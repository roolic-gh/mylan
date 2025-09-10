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
package local.transport.netty.smb.handler.codec;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import local.transport.netty.smb.protocol.ProtocolVersion;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbHeader;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.SmbResponse;
import local.transport.netty.smb.protocol.cifs.CifsSmbHeader;
import local.transport.netty.smb.protocol.smb2.Smb2Header;

public final class CodecUtils {

    private CodecUtils() {
        // utility class
    }

    public static SmbRequest decodeRequest(final ByteBuf byteBuf, final SmbDialect dialect) {
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), false, byteBuf.readerIndex());
        final var header = decodeHeader(byteBuf, ctx);
        // todo validate the message is request
        final var message = header.protocolVersion() == ProtocolVersion.SMB2
            ? Smb2CodecUtils.decodeRequestMessage(byteBuf, header.command(), ctx)
            : CifsCodecUtils.decodeRequestMessage(byteBuf, header.command());
        return new SmbRequest(header, message);
    }

    public static void encodeRequest(final SmbRequest request, final ByteBuf byteBuf, final SmbDialect dialect) {
        requireNonNull(request);
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), false, byteBuf.writerIndex());
        if (request.header() instanceof CifsSmbHeader header) {
            CifsCodecUtils.encodeHeader(byteBuf, header);
            CifsCodecUtils.encodeRequest(byteBuf, request.message());
        } else if (request.header() instanceof Smb2Header header) {
            Smb2CodecUtils.encodeHeader(byteBuf, header, ctx);
            Smb2CodecUtils.encodeRequest(byteBuf, request.message(), ctx);
        }
    }

    public static SmbResponse decodeResponse(final ByteBuf byteBuf, final SmbDialect dialect) {
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), true, byteBuf.readerIndex());
        final var header = decodeHeader(byteBuf, ctx);
        // todo validate the message is request
        final var message = header.protocolVersion() == ProtocolVersion.SMB2
            ? Smb2CodecUtils.decodeResponseMessage(byteBuf, header.command(), ctx)
            : CifsCodecUtils.decodeResponseMessage(byteBuf, header.command());
        return new SmbResponse(header, message);
    }

    public static void encodeResponse(SmbResponse response, final ByteBuf byteBuf, final SmbDialect dialect) {
        requireNonNull(response);
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), true, byteBuf.writerIndex());

    }

    private static SmbDialect nonNullDialect(final SmbDialect dialect) {
        return dialect == null ? SmbDialect.Unknown : dialect;
    }

    private static SmbHeader decodeHeader(final ByteBuf byteBuf, final CodecContext ctx) {
        final var protocolVer = ProtocolVersion.fromCode(byteBuf.readIntLE());
        return protocolVer == ProtocolVersion.SMB2 ?
            Smb2CodecUtils.decodeHeader(byteBuf, ctx) : CifsCodecUtils.decodeHeader(byteBuf);
    }

}
