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
import java.util.ArrayList;
import java.util.List;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.ProtocolVersion;
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Dialect;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.smb2.Smb2Flags;
import local.transport.netty.smb.protocol.smb2.Smb2LogoffRequest;
import local.transport.netty.smb.protocol.smb2.Smb2LogoffResponse;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateRequest;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateResponse;
import local.transport.netty.smb.protocol.smb2.Smb2SessionSetupRequest;
import local.transport.netty.smb.protocol.smb2.Smb2SessionSetupResponse;

public final class Smb2CodecUtils {

    private Smb2CodecUtils() {
        //utility class
    }

    public static Smb2Request decodeRequest(final ByteBuf byteBuf, final Smb2Dialect dialect) {
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), false, byteBuf.readerIndex());
        final var header = decodeHeader(byteBuf, ctx);
        // todo validate the message is request

        return switch (header.command()) {
            case SMB2_NEGOTIATE -> decodeNegotiateRequest(byteBuf, header, ctx);
            case SMB2_SESSION_SETUP -> decodeSessionSetupRequest(byteBuf, header, ctx);
            case SMB2_LOGOFF -> new Smb2LogoffRequest(header); // constant cotent

            default -> throw new SmbException("no request decoder for command " + header.command());
        };
    }

    public static void encodeRequest(final Smb2Request request, final ByteBuf byteBuf, final Smb2Dialect dialect) {
        requireNonNull(request);
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), false, byteBuf.writerIndex());
        encodeHeader(byteBuf, request.header(), ctx);
        switch (request) {
            case Smb2NegotiateRequest req -> encodeNegotiateRequest(byteBuf, req, ctx);
            case Smb2SessionSetupRequest req -> encodeSessionSetupRequest(byteBuf, req, ctx);
            case Smb2LogoffRequest req -> byteBuf.writeIntLE(4); // 2 byte length + 2 byte zeroes

            default -> throw new SmbException("no request encoder for class " + request.getClass());
        }
    }

    public static Smb2Response decodeResponse(final ByteBuf byteBuf, final Smb2Dialect dialect) {
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), true, byteBuf.readerIndex());
        final var header = decodeHeader(byteBuf, ctx);
        // todo validate the message is request

        return switch (header.command()) {
            case SMB2_NEGOTIATE -> decodeNegotiateResponse(byteBuf, header, ctx);
            case SMB2_SESSION_SETUP -> decodeSessionSetupResponse(byteBuf, header, ctx);
            case SMB2_LOGOFF -> new Smb2LogoffResponse(header); // ignore constant content

            default -> throw new SmbException("no response decoder for command " + header.command());
        };
    }

    public static void encodeResponse(Smb2Response response, final ByteBuf byteBuf, final Smb2Dialect dialect) {
        requireNonNull(response);
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), true, byteBuf.writerIndex());
        encodeHeader(byteBuf, response.header(), ctx);
        switch (response) {
            case Smb2NegotiateResponse resp -> encodeNegotiateResponse(byteBuf, resp, ctx);
            case Smb2SessionSetupResponse resp -> encodeSessionSetupResponse(byteBuf, resp, ctx);
            case Smb2LogoffResponse resp -> byteBuf.writeIntLE(4); //  2 bytes length + 2 bytes zeroes

            default -> throw new SmbException("no response encoder for class " + response.getClass());
        }
    }

    private static Smb2Dialect nonNullDialect(final Smb2Dialect dialect) {
        return dialect == null ? Smb2Dialect.Unknown : dialect;
    }

    // Header (MS-SMB2 2.2.1 SMB2 Packet Header)

    private static Smb2Header decodeHeader(final ByteBuf byteBuf, final CodecContext ctx) {
        final var header = new Smb2Header();
        final var protocolVer = ProtocolVersion.fromCode(byteBuf.readIntLE());
        if (protocolVer != ProtocolVersion.SMB2) {
            throw new SmbException("Unsupported protocol version: " + protocolVer);
        }
        final var length = byteBuf.readUnsignedShortLE(); // todo check expected constant is 64 always
        if (ctx.dialect().equalsOrHigher(Smb2Dialect.SMB2_1)) {
            header.setCreditCharge(byteBuf.readUnsignedShortLE());
        } else {
            byteBuf.skipBytes(2);
        }
        if (ctx.isRequest() && ctx.dialect().equalsOrHigher(Smb2Dialect.SMB3_0)) {
            header.setChannelSequence(byteBuf.readUnsignedShortLE());
            byteBuf.skipBytes(2);
        } else if (ctx.isResponse()) {
            header.setStatus(SmbError.fromCode(byteBuf.readIntLE()));
        } else {
            byteBuf.skipBytes(4);
        }
        header.setCommand(Smb2Command.fromCode(byteBuf.readUnsignedShortLE()));
        if (ctx.isRequest()) {
            header.setCreditRequest(byteBuf.readUnsignedShortLE());
        } else {
            header.setCreditResponse(byteBuf.readUnsignedShortLE());
        }
        header.setFlags(new Flags<>(byteBuf.readIntLE()));
        // validate direction
        final boolean isResponse = header.flags().get(Smb2Flags.SMB2_FLAGS_SERVER_TO_REDIR);
        if (isResponse ^ ctx.isResponse()) {
            throw new SmbException("Unexpected message type: received " + (isResponse ?
                " response (request expected)" : "request (response expected)"));
        }
        header.setNextCommandOffset(byteBuf.readIntLE());
        header.setMessageId(byteBuf.readLongLE());
        if (header.isAsync()) {
            header.setAsyncId(byteBuf.readLongLE());
        } else {
            byteBuf.skipBytes(4);
            header.setTreeId(byteBuf.readIntLE());
        }
        header.setSessionId(byteBuf.readLongLE());
        header.setSignature(Utils.readToByteArray(byteBuf, 16));
        return header;
    }

    private static void encodeHeader(final ByteBuf byteBuf, final Smb2Header header, final CodecContext ctx) {
        byteBuf.writeIntLE(ProtocolVersion.SMB2.code());
        byteBuf.writeShortLE(64); // fixed header length
        byteBuf.writeShortLE(ctx.dialect().equalsOrHigher(Smb2Dialect.SMB2_1) ? header.creditCharge() : 0);
        if (ctx.isRequest() && ctx.dialect().equalsOrHigher(Smb2Dialect.SMB3_0)) {
            byteBuf.writeShortLE(header.channelSequence());
            byteBuf.writeShortLE(0);
        } else {
            byteBuf.writeIntLE(ctx.isResponse() ? header.status().code() : 0);
        }
        byteBuf.writeShortLE(header.command().code());
        byteBuf.writeShortLE(ctx.isRequest() ? header.creditRequest() : header.creditResponse());
        byteBuf.writeIntLE(header.flags().asIntValue());
        byteBuf.writeIntLE(header.nextCommandOffset());
        byteBuf.writeLongLE(header.messageId());
        if (header.isAsync()) {
            byteBuf.writeLongLE(header.asyncId());
        } else {
            byteBuf.writeIntLE(0);
            byteBuf.writeIntLE(header.treeId());
        }
        byteBuf.writeLongLE(header.sessionId());
        byteBuf.writeZero(16); // actual signature is calculated after whole message is in buffer
    }

    // SMB2 NEGOTIATE Request (MS-SMB2 #2.2.3)

    private static Smb2Request decodeNegotiateRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2NegotiateRequest(header);
        final var structureSize = byteBuf.readShortLE(); // TODO validate expected const value = 36
        final var dialectCount = byteBuf.readShortLE();
        request.setSecurityMode(new Flags<>(byteBuf.readUnsignedShortLE()));
        byteBuf.skipBytes(2);
        request.setCapabilities(new Flags<>(byteBuf.readIntLE()));
        request.setClientGuid(Utils.readGuid(byteBuf));
        final var tempBuf = byteBuf.readBytes(8);
        final var dialects = new ArrayList<Smb2Dialect>(dialectCount);
        for (int i = 0; i < dialectCount; i++) {
            final var dialect = Smb2Dialect.fromCode(byteBuf.readUnsignedShortLE());
            if (dialect != Smb2Dialect.Unknown) {
                dialects.add(dialect);
            }
        }
        request.setDialects(List.copyOf(dialects));
        if (byteBuf.readableBytes() > 0 && dialects.contains(Smb2Dialect.SMB3_1_1)) {
            // check negotiation contexts if dialects contains SMB 3.1.1
            final int negCtxOffset = tempBuf.readIntLE();
            final int negCtxCount = tempBuf.readUnsignedShortLE();
            if (negCtxCount > 0 && negCtxOffset > 0) {
                byteBuf.readerIndex(ctx.headerStartPosition() + negCtxOffset);
                // todo read negotiation contexts
            }
        }
        return request;
    }

    private static void encodeNegotiateRequest(final ByteBuf byteBuf, final Smb2NegotiateRequest request,
        final CodecContext ctx) {

        final var msgStartPos = byteBuf.writerIndex();
        byteBuf.writeShortLE(0x24); // constant value = 36
        byteBuf.writeShortLE(request.dialects().size());
        byteBuf.writeShortLE(request.securityMode().asIntValue());
        byteBuf.writeShort(0); // reserved
        byteBuf.writeIntLE(request.capabilities().asIntValue());
        Utils.writeGuid(byteBuf, request.clientGuid());
        final var negCtxInfoPos = byteBuf.writerIndex();
        byteBuf.writeLong(0);
        request.dialects().forEach(dialect -> byteBuf.writeShortLE(dialect.code()));

        if (request.dialects().contains(Smb2Dialect.SMB3_1_1)) {
            // write negotiation contexts if dialects contains SMB 3.1.1
            final var negCtxs = request.negotiateContexts();
            if (negCtxs != null && !negCtxs.isEmpty()) {
                // add padding if necessary to ensure first context is 8 byte aligned;
                final var align = (byteBuf.writerIndex() - msgStartPos) % 8;
                if (align != 0) {
                    byteBuf.writeBytes(new byte[8 - align]);
                }
                byteBuf.setIntLE(negCtxInfoPos, byteBuf.writerIndex() - ctx.headerStartPosition()); // offset
                byteBuf.setShortLE(negCtxInfoPos + 4, negCtxs.size()); // cnt
                // todo encode contexts
            }
        }
    }

    // SMB2 NEGOTIATE Response (MS-SMB2 #2.2.4)

    private static Smb2Response decodeNegotiateResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2NegotiateResponse(header);
        final var structureSize = byteBuf.readUnsignedShortLE(); // todo validate expected const value = 65
        response.setSecurityMode(new Flags<>(byteBuf.readUnsignedShortLE()));
        response.setDialectRevision(Smb2Dialect.fromCode(byteBuf.readUnsignedShortLE()));
        final var negCtxsCount = byteBuf.readUnsignedShortLE();
        response.setServerGuid(Utils.readGuid(byteBuf));
        response.setCapabilities(new Flags<>(byteBuf.readIntLE()));
        response.setMaxTransactSize(byteBuf.readIntLE());
        response.setMaxReadSize(byteBuf.readIntLE());
        response.setMaxWriteSize(byteBuf.readIntLE());
        response.setSystemTime(Utils.unixMillisFromFiletime(byteBuf.readLongLE()));
        response.setServerStartTime(Utils.unixMillisFromFiletime(byteBuf.readLongLE()));
        final var securityBufPos = ctx.headerStartPosition() + byteBuf.readUnsignedShortLE();
        final var securityBuflength = byteBuf.readUnsignedShortLE();
        response.setToken(SpnegoCodecUtils.decodeNegToken(byteBuf.slice(securityBufPos, securityBuflength)));
        if (response.dialectRevision().equalsOrHigher(Smb2Dialect.SMB3_1_1)) {
            final var negCtxsPos = ctx.headerStartPosition() + byteBuf.readIntLE();
            // todo read contexts
        }
        return response;
    }

    private static void encodeNegotiateResponse(final ByteBuf byteBuf, final Smb2NegotiateResponse response,
        final CodecContext ctx) {
    }

    // SESSION_SETUP Request (MS-SMB2 #2.2.5)

    private static Smb2Request decodeSessionSetupRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2SessionSetupRequest(header);
        final var structureSize = byteBuf.readShortLE(); // TODO validate expected constant = 25
        request.setSessionFlags(new Flags<>(byteBuf.readUnsignedByte()));
        request.setSecurityMode(new Flags<>(byteBuf.readUnsignedByte()));
        request.setCapabilities(new Flags<>(byteBuf.readIntLE()));
        byteBuf.skipBytes(4); // channel, reserved and must ignored by spec
        final var bufferPos = ctx.headerStartPosition() + byteBuf.readUnsignedShortLE();
        final var bufferlength = byteBuf.readUnsignedShortLE();
        request.setPreviousSessionId(byteBuf.readLongLE());
        request.setToken(SpnegoCodecUtils.decodeNegToken(byteBuf.slice(bufferPos, bufferlength)));
        return request;
    }

    private static void encodeSessionSetupRequest(final ByteBuf byteBuf, final Smb2SessionSetupRequest request,
        final CodecContext ctx) {

        byteBuf.writeShortLE(25); // structure size, constant value
        byteBuf.writeByte(request.sessionFlags().asIntValue());
        byteBuf.writeByte(request.securityMode().asIntValue());
        byteBuf.writeIntLE(request.capabilities().asIntValue());
        byteBuf.writeIntLE(0); // channel, reserved
        final var bufferRefPos = byteBuf.writerIndex();
        byteBuf.writeIntLE(0); // reserve space for buffer reference data
        byteBuf.writeLongLE(request.previousSessionId());
        final var bufferPos = byteBuf.writerIndex();
        SpnegoCodecUtils.encodeNegToken(byteBuf, request.token());
        byteBuf.setShortLE(bufferRefPos, bufferPos - ctx.headerStartPosition()); // buffer offset
        byteBuf.setShortLE(bufferRefPos + 2, byteBuf.writerIndex() - bufferPos); // buffer length
    }

    // SESSION_SETUP Response (MS-SMB2 #2.2.6)

    private static Smb2Response decodeSessionSetupResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2SessionSetupResponse(header);
        final var structureSize = byteBuf.readShortLE(); // TODO validate expected constant = 9
        response.setSessionFlags(new Flags<>(byteBuf.readUnsignedShortLE()));
        final var bufferPos = ctx.headerStartPosition() + byteBuf.readUnsignedShortLE();
        final var bufferlength = byteBuf.readUnsignedShortLE();
        response.setToken(SpnegoCodecUtils.decodeNegToken(byteBuf.slice(bufferPos, bufferlength)));
        return response;
    }

    private static void encodeSessionSetupResponse(final ByteBuf byteBuf, final Smb2SessionSetupResponse response,
        final CodecContext ctx) {

        byteBuf.writeShortLE(9); // structure size, constant value
        byteBuf.writeShortLE(response.sessionFlags().asIntValue());
        final var bufferRefPos = byteBuf.writerIndex();
        byteBuf.writeIntLE(0); // reserve space for buffer reference data
        final var bufferPos = byteBuf.writerIndex();
        SpnegoCodecUtils.encodeNegToken(byteBuf, response.token());
        byteBuf.setShortLE(bufferRefPos, bufferPos - ctx.headerStartPosition()); // buffer offset
        byteBuf.setShortLE(bufferRefPos + 2, byteBuf.writerIndex() - bufferPos); // buffer length
    }
}
