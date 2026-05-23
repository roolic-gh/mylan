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
package local.mylan.transport.smb.handler.codec;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import local.mylan.transport.smb.Utils;
import local.mylan.transport.smb.protocol.Flags;
import local.mylan.transport.smb.protocol.ProtocolVersion;
import local.mylan.transport.smb.protocol.Smb2Command;
import local.mylan.transport.smb.protocol.Smb2Dialect;
import local.mylan.transport.smb.protocol.Smb2Header;
import local.mylan.transport.smb.protocol.Smb2Request;
import local.mylan.transport.smb.protocol.Smb2Response;
import local.mylan.transport.smb.protocol.SmbError;
import local.mylan.transport.smb.protocol.SmbException;
import local.mylan.transport.smb.protocol.fscc.FileInformationClass;
import local.mylan.transport.smb.protocol.fscc.FsctlCode;
import local.mylan.transport.smb.protocol.smb2.Smb2CloseRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2CloseResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateAction;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateDisposition;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2CreateResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2ErrorResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2Flags;
import local.mylan.transport.smb.protocol.smb2.Smb2ImpersonationLevel;
import local.mylan.transport.smb.protocol.smb2.Smb2IoctlRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2IoctlResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2LogoffRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2LogoffResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2NegotiateRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2NegotiateResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2OpLockLevel;
import local.mylan.transport.smb.protocol.smb2.Smb2QueryDirectoryRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2QueryDirectoryResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2SessionSetupRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2SessionSetupResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2ShareType;
import local.mylan.transport.smb.protocol.smb2.Smb2TreeConnectFlags;
import local.mylan.transport.smb.protocol.smb2.Smb2TreeConnectRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2TreeConnectResponse;
import local.mylan.transport.smb.protocol.smb2.Smb2TreeDisconnectRequest;
import local.mylan.transport.smb.protocol.smb2.Smb2TreeDisconnectResponse;

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
            case SMB2_LOGOFF -> new Smb2LogoffRequest(header); // no content
            case SMB2_TREE_CONNECT -> decodeTreeConnectRequest(byteBuf, header, ctx);
            case SMB2_TREE_DISCONNECT -> new Smb2TreeDisconnectRequest(header); // no content
            case SMB2_CREATE -> decodeCreateRequest(byteBuf, header, ctx);
            case SMB2_CLOSE -> decodeCloseRequest(byteBuf, header, ctx);
            case SMB2_IOCTL -> decodeIoctlRequest(byteBuf, header, ctx);
            case SMB2_QUERY_DIRECTORY -> decodeQueryDirRequest(byteBuf, header, ctx);

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
            case Smb2LogoffRequest req -> encodeEmpty(byteBuf);
            case Smb2TreeConnectRequest req -> encodeTreeConnectRequest(byteBuf, req, ctx);
            case Smb2TreeDisconnectRequest req -> encodeEmpty(byteBuf);
            case Smb2CreateRequest req -> encodeCreateRequest(byteBuf, req, ctx);
            case Smb2CloseRequest req -> encodeCloseRequest(byteBuf, req, ctx);
            case Smb2IoctlRequest req -> encodeIoctlRequest(byteBuf, req, ctx);
            case Smb2QueryDirectoryRequest req -> encodeQueryDirRequest(byteBuf, req, ctx);

            default -> throw new SmbException("no request encoder for class " + request.getClass());
        }
    }

    public static Smb2Response decodeResponse(final ByteBuf byteBuf, final Smb2Dialect dialect) {
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), true, byteBuf.readerIndex());
        final var header = decodeHeader(byteBuf, ctx);
        // todo validate the message is response

//        if (header.status() != SmbError.STATUS_SUCCESS) {
//            return decodeErrorResponse(byteBuf, header, ctx);
//        }
        return switch (header.command()) {
            case SMB2_NEGOTIATE -> decodeNegotiateResponse(byteBuf, header, ctx);
            case SMB2_SESSION_SETUP -> decodeSessionSetupResponse(byteBuf, header, ctx);
            case SMB2_LOGOFF -> new Smb2LogoffResponse(header);
            case SMB2_TREE_CONNECT -> decodeTreeConnectResponse(byteBuf, header, ctx);
            case SMB2_TREE_DISCONNECT -> new Smb2TreeDisconnectResponse(header);
            case SMB2_CREATE -> decodeCreateResponse(byteBuf, header, ctx);
            case SMB2_CLOSE -> decodeCloseResponse(byteBuf, header, ctx);
            case SMB2_IOCTL -> decodeIoctlResponse(byteBuf, header, ctx);
            case SMB2_QUERY_DIRECTORY -> decodeQueryDirResponse(byteBuf, header, ctx);

            default -> throw new SmbException("no response decoder for command " + header.command());
        };
    }

    public static void encodeResponse(Smb2Response response, final ByteBuf byteBuf, final Smb2Dialect dialect) {
        requireNonNull(response);
        requireNonNull(byteBuf);
        final var ctx = new CodecContext(nonNullDialect(dialect), true, byteBuf.writerIndex());
        encodeHeader(byteBuf, response.header(), ctx);
        switch (response) {
            case Smb2ErrorResponse resp -> encodeErrorResponse(byteBuf, resp, ctx);
            case Smb2NegotiateResponse resp -> encodeNegotiateResponse(byteBuf, resp, ctx);
            case Smb2SessionSetupResponse resp -> encodeSessionSetupResponse(byteBuf, resp, ctx);
            case Smb2LogoffResponse resp -> encodeEmpty(byteBuf);
            case Smb2TreeConnectResponse resp -> encodeTreeConnectResponse(byteBuf, resp, ctx);
            case Smb2TreeDisconnectResponse resp -> encodeEmpty(byteBuf);
            case Smb2CreateResponse resp -> encodeCreateResponse(byteBuf, resp, ctx);
            case Smb2CloseResponse resp -> encodeCloseResponse(byteBuf, resp, ctx);
            case Smb2IoctlResponse resp -> encodeIoctlResponse(byteBuf, resp, ctx);
            case Smb2QueryDirectoryResponse resp -> encodeQueryDirResponse(byteBuf, resp, ctx);

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
        readAssertStructSize(byteBuf, 64, "SMB2 Header");
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

    // SMB2 ERROR Response (MS-SMB2 #2.2.2)

    private static Smb2Response decodeErrorResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2ErrorResponse(header);
        readAssertStructSize(byteBuf, 9, "ERROR Response");
        // todo extract error data
        return response;
    }

    private static void encodeErrorResponse(final ByteBuf byteBuf, final Smb2ErrorResponse response,
        final CodecContext ctx) {

        byteBuf.writeShortLE(9); // struct size
        byteBuf.writeZero(6); // 1x ctx count + 1x reserved + 4x dataLength
        // todo encode error data if exists
    }

    // SMB2 NEGOTIATE Request (MS-SMB2 #2.2.3)

    private static Smb2Request decodeNegotiateRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2NegotiateRequest(header);
        readAssertStructSize(byteBuf, 36, "NEGOTIATE Request");
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
                CodecUtils.alignWriter(byteBuf, ctx.headerStartPosition(), 8);
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
        readAssertStructSize(byteBuf, 65, "NEGOTIATE Response");
        response.setSecurityMode(new Flags<>(byteBuf.readUnsignedShortLE()));
        response.setDialectRevision(Smb2Dialect.fromCode(byteBuf.readUnsignedShortLE()));
        final var negCtxsCount = byteBuf.readUnsignedShortLE();
        response.setServerGuid(Utils.readGuid(byteBuf));
        response.setCapabilities(new Flags<>(byteBuf.readIntLE()));
        response.setMaxTransactSize(byteBuf.readIntLE());
        response.setMaxReadSize(byteBuf.readIntLE());
        response.setMaxWriteSize(byteBuf.readIntLE());
        response.setSystemTime(byteBuf.readLongLE());
        response.setServerStartTime(byteBuf.readLongLE());
        response.setToken(readField(byteBuf, RefType.SHORT, ctx, SpnegoCodecUtils::decodeNegToken, null));
        if (response.dialectRevision().equalsOrHigher(Smb2Dialect.SMB3_1_1)) {
            final var negCtxsPos = ctx.headerStartPosition() + byteBuf.readIntLE();
            // todo read contexts
        }
        return response;
    }

    private static void encodeNegotiateResponse(final ByteBuf byteBuf, final Smb2NegotiateResponse response,
        final CodecContext ctx) {

        byteBuf.writeShortLE(65); // struct size constant
        byteBuf.writeShortLE(response.securityMode().asIntValue());
        byteBuf.writeShortLE(response.dialectRevision().code());
        byteBuf.writeZero(2); // todo set context count for SMB 3.1.1
        Utils.writeGuid(byteBuf, response.serverGuid());
        byteBuf.writeIntLE(response.capabilities().asIntValue());
        byteBuf.writeIntLE(response.maxTransactSize());
        byteBuf.writeIntLE(response.maxReadSize());
        byteBuf.writeIntLE(response.maxWriteSize());
        byteBuf.writeLongLE(response.systemTime());
        byteBuf.writeLongLE(response.serverStartTime());

        final var tokenRef = prepareFieldRef(byteBuf, RefType.SHORT, ctx);
        final var negCtxRef = prepareFieldRef(byteBuf, RefType.SHORT, ctx);
        writeField(byteBuf, tokenRef, () -> SpnegoCodecUtils.encodeNegToken(byteBuf, response.token()));
        if (response.dialectRevision().equalsOrHigher(Smb2Dialect.SMB3_1_1)) {
            // todo write negotiate contexts
        }
    }

    // SESSION_SETUP Request (MS-SMB2 #2.2.5)

    private static Smb2Request decodeSessionSetupRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2SessionSetupRequest(header);
        readAssertStructSize(byteBuf, 25, "SESSION_SETUP Request");
        request.setSessionFlags(new Flags<>(byteBuf.readUnsignedByte()));
        request.setSecurityMode(new Flags<>(byteBuf.readUnsignedByte()));
        request.setCapabilities(new Flags<>(byteBuf.readIntLE()));
        byteBuf.skipBytes(4); // channel, reserved and must ignored by spec
        request.setToken(readField(byteBuf, RefType.SHORT, ctx, SpnegoCodecUtils::decodeNegToken, null));
        request.setPreviousSessionId(byteBuf.readLongLE());
        return request;
    }

    private static void encodeSessionSetupRequest(final ByteBuf byteBuf, final Smb2SessionSetupRequest request,
        final CodecContext ctx) {

        byteBuf.writeShortLE(25); // structure size, constant value
        byteBuf.writeByte(request.sessionFlags().asIntValue());
        byteBuf.writeByte(request.securityMode().asIntValue());
        byteBuf.writeIntLE(request.capabilities().asIntValue());
        byteBuf.writeIntLE(0); // channel, reserved
        final var tokenRef = prepareFieldRef(byteBuf, RefType.SHORT, ctx);
        byteBuf.writeLongLE(request.previousSessionId());
        writeField(byteBuf, tokenRef, () -> SpnegoCodecUtils.encodeNegToken(byteBuf, request.token()));
    }

    // SESSION_SETUP Response (MS-SMB2 #2.2.6)

    private static Smb2Response decodeSessionSetupResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2SessionSetupResponse(header);
        readAssertStructSize(byteBuf, 9, "SESSION_SETUP Response");
        response.setSessionFlags(new Flags<>(byteBuf.readUnsignedShortLE()));
        response.setToken(readField(byteBuf, RefType.SHORT, ctx, SpnegoCodecUtils::decodeNegToken, null));
        return response;
    }

    private static void encodeSessionSetupResponse(final ByteBuf byteBuf, final Smb2SessionSetupResponse response,
        final CodecContext ctx) {

        byteBuf.writeShortLE(9); // structure size, constant value
        byteBuf.writeShortLE(response.sessionFlags().asIntValue());
        final var tokenRef = prepareFieldRef(byteBuf, RefType.SHORT, ctx);
        writeField(byteBuf, tokenRef, () -> SpnegoCodecUtils.encodeNegToken(byteBuf, response.token()));
    }

    // SMB2 TREE_CONNECT Request (MS-SMB2 #2.2.9)

    private static Smb2Request decodeTreeConnectRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2TreeConnectRequest(header);
        readAssertStructSize(byteBuf, 9, "TREE_CONNECT Request");
        request.setFlags(new Flags<>(byteBuf.readUnsignedShortLE()));
        if (request.flags().get(Smb2TreeConnectFlags.SMB2_TREE_CONNECT_FLAG_EXTENSION_PRESENT)) {
            // TODO get ext content incl path value
        } else {
            request.setPath(readUnicodeStringField(byteBuf, ctx));
        }
        return request;
    }

    private static void encodeTreeConnectRequest(final ByteBuf byteBuf, final Smb2TreeConnectRequest request,
        final CodecContext ctx) {

        byteBuf.writeShortLE(9); // const length
        byteBuf.writeShortLE(request.flags().asIntValue());
        final var pathRef = prepareFieldRef(byteBuf, RefType.SHORT, ctx);
        if (request.flags().get(Smb2TreeConnectFlags.SMB2_TREE_CONNECT_FLAG_EXTENSION_PRESENT)) {
            // TODO write ext content incl path name
        } else {
            writeUnicodeStringField(byteBuf, pathRef, request.path());
        }
    }

    // SMB2 TREE_CONNECT Response (MS-SMB2 #2.2.10)

    private static Smb2Response decodeTreeConnectResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2TreeConnectResponse(header);
        readAssertStructSize(byteBuf, 16, "TREE_CONNECT Response");
        response.setShareType(Smb2ShareType.fromCode(byteBuf.readByte()));
        byteBuf.skipBytes(1);
        response.setShareFlags(new Flags<>(byteBuf.readIntLE()));
        response.setCapabilities(new Flags<>(byteBuf.readIntLE()));
        response.setMaxAccess(new Flags<>(byteBuf.readIntLE()));
        return response;
    }

    private static void encodeTreeConnectResponse(final ByteBuf byteBuf, final Smb2TreeConnectResponse response,
        final CodecContext ctx) {

        byteBuf.writeShortLE(16); // struct size
        byteBuf.writeByte(response.shareType().code());
        byteBuf.writeZero(1);
        byteBuf.writeIntLE(response.shareFlags().asIntValue());
        byteBuf.writeIntLE(response.capabilities().asIntValue());
        byteBuf.writeIntLE(response.maxAccess().asIntValue());
    }

    // SMB2 CREATE Request (MS-SMB2 #2.2.13)

    private static Smb2Request decodeCreateRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2CreateRequest(header);
        readAssertStructSize(byteBuf, 57, "CREATE Request");
        byteBuf.skipBytes(1); // securityFlags -- reserved
        request.setOpLockLevel(Smb2OpLockLevel.fromCode(byteBuf.readByte()));
        request.setImpersonationLevel(Smb2ImpersonationLevel.fromCode(byteBuf.readIntLE()));
        byteBuf.skipBytes(16); // SmbCreateFlags (8 bytes, reserved) + 8 bytes reserved
        request.setDesiredAccess(new Flags<>(byteBuf.readIntLE()));
        request.setFileAttributes(new Flags<>(byteBuf.readIntLE()));
        request.setShareAccess(new Flags<>(byteBuf.readIntLE()));
        request.setCreateDisposition(Smb2CreateDisposition.fromCode(byteBuf.readIntLE()));
        request.setCreateOptions(new Flags<>(byteBuf.readIntLE()));
        request.setName(readUnicodeStringField(byteBuf, ctx));
        // TODO contexts
        return request;
    }

    private static void encodeCreateRequest(final ByteBuf byteBuf, final Smb2CreateRequest request,
        final CodecContext ctx) {

        byteBuf.writeShortLE(57); // struct size constant
        byteBuf.writeZero(1); // securityFlags -- reserved
        byteBuf.writeByte(request.opLockLevel().code());
        byteBuf.writeIntLE(request.impersonationLevel().code());
        byteBuf.writeZero(16); // SmbCreateFlags (8 bytes, reserved) + 8 bytes reserved
        byteBuf.writeIntLE(request.desiredAccess().asIntValue());
        byteBuf.writeIntLE(request.fileAttributes().asIntValue());
        byteBuf.writeIntLE(request.shareAccess().asIntValue());
        byteBuf.writeIntLE(request.createDisposition().code());
        byteBuf.writeIntLE(request.createOptions().asIntValue());
        final var nameRef = prepareFieldRef(byteBuf, RefType.SHORT, ctx);
        final var contextsRef = prepareFieldRef(byteBuf, RefType.INT, ctx);
        final var bufPos = byteBuf.writerIndex();
        writeUnicodeStringField(byteBuf, nameRef, request.name());
        // TODO contexts
        if (byteBuf.writerIndex() == bufPos) {
            byteBuf.writeZero(1); // buffer part of the message shoudl be at least 1 byte long
        }
    }

    // SMB2 CREATE Response (MS-SMB2 #2.2.14)

    private static Smb2Response decodeCreateResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2CreateResponse(header);
        readAssertStructSize(byteBuf, 89, "CREATE Response");
        response.setOpLockLevel(Smb2OpLockLevel.fromCode(byteBuf.readByte()));
        response.setFlags(new Flags<>(byteBuf.readByte()));
        response.setCreateAction(Smb2CreateAction.fromCode(byteBuf.readIntLE()));
        response.setCreationTime(byteBuf.readLongLE());
        response.setLastAccessTime(byteBuf.readLongLE());
        response.setLastWriteTime(byteBuf.readLongLE());
        response.setChangeTime(byteBuf.readLongLE());
        response.setAllocationSize(byteBuf.readLongLE());
        response.setEndOfFile(byteBuf.readLongLE());
        response.setFileAttributes(new Flags<>(byteBuf.readIntLE()));
        byteBuf.skipBytes(4); // reserved
        response.setFileId(Utils.readGuid(byteBuf));
        // reading context omitted; TODO implement
        return response;
    }

    private static void encodeCreateResponse(final ByteBuf byteBuf, final Smb2CreateResponse response,
        final CodecContext ctx) {

        byteBuf.writeIntLE(89); // structure size constant
        byteBuf.writeByte(response.opLockLevel().code());
        byteBuf.writeByte(response.flags().asIntValue());
        byteBuf.writeIntLE(response.createAction().code());
        byteBuf.writeLongLE(response.creationTime());
        byteBuf.writeLongLE(response.lastAccessTime());
        byteBuf.writeLongLE(response.lastWriteTime());
        byteBuf.writeLongLE(response.changeTime());
        byteBuf.writeLongLE(response.allocationSize());
        byteBuf.writeLongLE(response.endOfFile());
        byteBuf.writeIntLE(response.fileAttributes().asIntValue());
        byteBuf.writeZero(4); // reserved
        Utils.writeGuid(byteBuf, response.fileId());
        byteBuf.writeZero(8); // writing no contexts; TODO implement
    }

    // SMB2 CLOSE Request (MS-SMB2 #2.2.15)

    private static Smb2Request decodeCloseRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2CloseRequest(header);
        readAssertStructSize(byteBuf, 24, "CLOSE Request");
        request.setFlags(new Flags<>(byteBuf.readUnsignedShortLE()));
        byteBuf.skipBytes(4); //reserved
        request.setFileId(Utils.readGuid(byteBuf));
        return request;
    }

    private static void encodeCloseRequest(final ByteBuf byteBuf, final Smb2CloseRequest request,
        final CodecContext ctx) {

        byteBuf.writeShortLE(24); // struct size constant
        byteBuf.writeShortLE(request.flags().asIntValue());
        byteBuf.writeZero(4); // reserved
        Utils.writeGuid(byteBuf, request.fileId());
    }

    // SMB2 CLOSE Response (MS-SMB2 #2.2.16)

    private static Smb2Response decodeCloseResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2CloseResponse(header);
        readAssertStructSize(byteBuf, 60, "CLOSE Response");
        response.setFlags(new Flags<>(byteBuf.readUnsignedShortLE()));
        byteBuf.skipBytes(4); // reserved
        response.setCreationTime(byteBuf.readLongLE());
        response.setLastAccessTime(byteBuf.readLongLE());
        response.setLastWriteTime(byteBuf.readLongLE());
        response.setChangeTime(byteBuf.readLongLE());
        response.setAllocationSize(byteBuf.readLongLE());
        response.setEndOfFile(byteBuf.readLongLE());
        response.setFileAttributes(new Flags<>(byteBuf.readIntLE()));
        return response;
    }

    private static void encodeCloseResponse(final ByteBuf byteBuf, final Smb2CloseResponse response,
        final CodecContext ctx) {

        byteBuf.writeIntLE(60); // structure size constant
        byteBuf.writeShortLE(response.flags().asIntValue());
        byteBuf.writeZero(4); // reserved
        byteBuf.writeLongLE(response.creationTime());
        byteBuf.writeLongLE(response.lastAccessTime());
        byteBuf.writeLongLE(response.lastWriteTime());
        byteBuf.writeLongLE(response.changeTime());
        byteBuf.writeLongLE(response.allocationSize());
        byteBuf.writeLongLE(response.endOfFile());
        byteBuf.writeIntLE(response.fileAttributes().asIntValue());
    }

    // SMB2 IOCTL Request (MS-SMB2 #2.2.31)

    private static Smb2Request decodeIoctlRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2IoctlRequest(header);
        readAssertStructSize(byteBuf, 57, "IOCTL Request");
        byteBuf.skipBytes(2); // reserved
        final var cc = FsctlCode.fromCode(byteBuf.readIntLE());
        request.setCtlCode(cc);
        request.setFileId(Utils.readGuid(byteBuf));
        request.setInput(
            readField(byteBuf, RefType.INT, ctx, slice -> IoctlCodecUtils.decodeInput(slice, cc), null));
        request.setMaxInputResponse(byteBuf.readIntLE());
        request.setOutput(
            readField(byteBuf, RefType.INT, ctx, slice -> IoctlCodecUtils.decodeOutput(slice, cc), null));
        request.setMaxOutputResponse(byteBuf.readIntLE());
        request.setFsctl(byteBuf.readIntLE() == 1); // single bit flag
        return request;
    }

    private static void encodeIoctlRequest(final ByteBuf byteBuf, final Smb2IoctlRequest request,
        final CodecContext ctx) {

        byteBuf.writeShortLE(57); // struct size
        byteBuf.writeZero(2); // reserved
        final var cc = request.ctlCode();
        byteBuf.writeIntLE(cc.code());
        Utils.writeGuid(byteBuf, request.fileId());
        final var inputRef = prepareFieldRef(byteBuf, RefType.INT, ctx);
        byteBuf.writeIntLE(request.maxInputResponse());
        final var outputRef = prepareFieldRef(byteBuf, RefType.INT, ctx);
        byteBuf.writeIntLE(request.maxOutputResponse());
        byteBuf.writeIntLE(request.isFsctl() ? 1 : 0); // single bit flag
        byteBuf.writeZero(4); // reserved (padding)
        writeField(byteBuf, inputRef, () -> IoctlCodecUtils.encodeInput(byteBuf, request.input(), cc));
        writeField(byteBuf, outputRef, () -> IoctlCodecUtils.encodeOutput(byteBuf, request.output(), cc));
    }

    // SMB2 IOCTL Response (MS-SMB2 #2.2.32)

    private static Smb2Response decodeIoctlResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2IoctlResponse(header);
        readAssertStructSize(byteBuf, 49, "IOCTL Response");
        byteBuf.skipBytes(2); // reserved
        final var cc = FsctlCode.fromCode(byteBuf.readIntLE());
        response.setCtlCode(cc);
        response.setFileId(Utils.readGuid(byteBuf));
        response.setInput(
            readField(byteBuf, RefType.INT, ctx, slice -> IoctlCodecUtils.decodeInput(slice, cc), null));
        response.setOutput(
            readField(byteBuf, RefType.INT, ctx, slice -> IoctlCodecUtils.decodeOutput(slice, cc), null));
        return response;
    }

    private static void encodeIoctlResponse(final ByteBuf byteBuf, final Smb2IoctlResponse response,
        final CodecContext ctx) {

        byteBuf.writeShortLE(49); // struct size
        byteBuf.writeZero(2); // reserved
        final var cc = response.ctlCode();
        byteBuf.writeIntLE(cc.code());
        Utils.writeGuid(byteBuf, response.fileId());
        final var inputRef = prepareFieldRef(byteBuf, RefType.INT, ctx);
        final var outputRef = prepareFieldRef(byteBuf, RefType.INT, ctx);
        byteBuf.writeZero(8); // flags (not used) + reserved (padding)
        writeField(byteBuf, inputRef, () -> IoctlCodecUtils.encodeInput(byteBuf, response.input(), cc));
        writeField(byteBuf, outputRef, () -> IoctlCodecUtils.encodeOutput(byteBuf, response.output(), cc));
    }

    // SMB2 QUERY_DIRECTORY Request (MS-SMB2 #2.2.33)

    private static Smb2Request decodeQueryDirRequest(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var request = new Smb2QueryDirectoryRequest(header);
        readAssertStructSize(byteBuf, 33, "QUERY_DIRECTORY Request");
        request.setFileInformationClass(FileInformationClass.fromValue(byteBuf.readUnsignedByte()));
        request.setFlags(new Flags<>(byteBuf.readUnsignedByte()));
        request.setFileIndex(byteBuf.readIntLE());
        request.setFileId(Utils.readGuid(byteBuf));
        request.setSearchPattern(readUnicodeStringField(byteBuf, ctx));
        request.setOutputBufferLength(byteBuf.readIntLE());
        return request;
    }

    private static void encodeQueryDirRequest(final ByteBuf byteBuf, final Smb2QueryDirectoryRequest request,
        final CodecContext ctx) {

        byteBuf.writeShortLE(33); // struct size
        byteBuf.writeByte(request.fileInformationClass().value());
        byteBuf.writeByte(request.flags().asIntValue());
        byteBuf.writeIntLE(request.fileIndex());
        Utils.writeGuid(byteBuf, request.fileId());
        final var patternRef = prepareFieldRef(byteBuf, RefType.SHORT, ctx);
        byteBuf.writeIntLE(request.outputBufferLength());
        writeUnicodeStringField(byteBuf, patternRef, request.searchPattern());
    }

    // SMB2 QUERY_DIRECTORY Response (MS-SMB2 #2.2.34)

    private static Smb2Response decodeQueryDirResponse(final ByteBuf byteBuf, final Smb2Header header,
        final CodecContext ctx) {

        final var response = new Smb2QueryDirectoryResponse(header);
        readAssertStructSize(byteBuf, 9, "QUERY_DIRECTORY Response");
        final var offset = byteBuf.readUnsignedShortLE();
        final var length = byteBuf.readIntLE();
        response.setEncoded(Utils.getByteArray(byteBuf, ctx.headerStartPosition() + offset, length));
        return response;
    }

    private static void encodeQueryDirResponse(final ByteBuf byteBuf, final Smb2QueryDirectoryResponse response,
        final CodecContext ctx) {

        byteBuf.writeShortLE(9); // struct size
        final var infoPos = byteBuf.writerIndex();
        byteBuf.writeZero(6); // 2x offset + 4x length
        final var dataPos = byteBuf.writerIndex();
        FsccCodecUtils.encodeFileInformation(byteBuf, response.decoded());
        byteBuf.setShortLE(infoPos, dataPos - ctx.headerStartPosition()); // offset
        byteBuf.setIntLE(infoPos + 2, byteBuf.writerIndex() - dataPos); // length
    }

    // SHARED

    private static void encodeEmpty(final ByteBuf byteBuf) {
        //  04 00 00 00 - first 2 bytes is structure size, last 2 bytes are reserved
        byteBuf.writeIntLE(4);
    }

    private static void readAssertStructSize(final ByteBuf byteBuf, final int expected, final String label) {
        final var structSize = byteBuf.readUnsignedShortLE();
        if (structSize != expected) {
            throw new SmbException("Invalid structSize value %d (expected %d) decoding %s."
                .formatted(structSize, expected, label));
        }
    }

    private static FieldRef prepareFieldRef(final ByteBuf byteBuf, final RefType refType, final CodecContext ctx) {
        final var refPos = byteBuf.writerIndex();
        byteBuf.writeZero(refType.size * 2); // reserve space for reference
        return new FieldRef(refType, refPos, ctx.headerStartPosition());
    }

    private static void writeField(final ByteBuf byteBuf, final FieldRef ref, final Runnable encoder) {
        final var startPos = byteBuf.writerIndex();
        encoder.run();
        final int offset = startPos - ref.startPos();
        final int length = byteBuf.writerIndex() - startPos;
        if (ref.type == RefType.SHORT) {
            byteBuf.setShortLE(ref.offsetPos(), offset);
            byteBuf.setShortLE(ref.lengthPos(), length);
        } else {
            byteBuf.setIntLE(ref.offsetPos(), offset);
            byteBuf.setIntLE(ref.lengthPos(), length);
        }
    }

    private static <T> T readField(final ByteBuf byteBuf, final RefType refType, final CodecContext ctx,
        final Function<ByteBuf, T> decoder, T defaultResult) {

        final var startPos = (refType == RefType.SHORT ? byteBuf.readShortLE() : byteBuf.readIntLE())
            + ctx.headerStartPosition();
        final var length = refType == RefType.SHORT ? byteBuf.readShortLE() : byteBuf.readIntLE();
        return length > 0 ? decoder.apply(byteBuf.slice(startPos, length)) : defaultResult;
    }

    private static void writeUnicodeStringField(final ByteBuf byteBuf, final FieldRef ref, final String value) {
        writeField(byteBuf, ref, () -> byteBuf.writeCharSequence(value, StandardCharsets.UTF_16LE));
    }

    private static String readUnicodeStringField(final ByteBuf byteBuf, final CodecContext ctx) {
        final var pos = byteBuf.readUnsignedShortLE() + ctx.headerStartPosition();
        final var length = byteBuf.readUnsignedShortLE();
        return length > 0 ? byteBuf.getCharSequence(pos, length, StandardCharsets.UTF_16LE).toString() : "";
    }

    private enum RefType {
        SHORT(2), INT(4);

        final int size;

        RefType(final int size) {
            this.size = size;
        }
    }

    private record FieldRef(RefType type, int offsetPos, int startPos) {
        int lengthPos() {
            return offsetPos + type.size;
        }
    }
}
