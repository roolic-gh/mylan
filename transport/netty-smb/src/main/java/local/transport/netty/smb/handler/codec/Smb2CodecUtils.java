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

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.ProtocolVersion;
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.SmbHeader;
import local.transport.netty.smb.protocol.SmbRequestMessage;
import local.transport.netty.smb.protocol.SmbResponseMessage;
import local.transport.netty.smb.protocol.smb2.Smb2Header;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateRequest;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateResponse;

final class Smb2CodecUtils {

    private Smb2CodecUtils() {
        //utility class
    }

    // Header (MS-SMB2 2.2.1 SMB2 Packet Header)

    static SmbHeader decodeHeader(final ByteBuf byteBuf, final CodecContext ctx) {
        final var header = new Smb2Header();
        // protocol version is already read
        final var length = byteBuf.readUnsignedShortLE(); // todo check expected constant is 64 always
        if (ctx.dialect().sameOrAfter(SmbDialect.SMB2_1)) {
            header.setCreditCharge(byteBuf.readUnsignedShortLE());
        } else {
            byteBuf.skipBytes(2);
        }
        if (ctx.isRequest() && ctx.dialect().sameOrAfter(SmbDialect.SMB3_0)) {
            header.setChannelSequence(byteBuf.readUnsignedShortLE());
            byteBuf.skipBytes(2);
        } else if (ctx.isResponse()) {
            header.setStatus(SmbError.fromCode(byteBuf.readIntLE()));
        } else {
            byteBuf.skipBytes(4);
        }
        header.setCommand(SmbCommand.fromCode(byteBuf.readUnsignedShortLE(), ProtocolVersion.SMB2));
        if (ctx.isRequest()) {
            header.setCreditRequest(byteBuf.readUnsignedShortLE());
        } else {
            header.setCreditResponse(byteBuf.readUnsignedShortLE());
        }
        header.setFlags(new Flags<>(byteBuf.readIntLE()));
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

    static void encodeHeader(final ByteBuf byteBuf, final Smb2Header header, final CodecContext ctx) {
        byteBuf.writeIntLE(header.protocolVersion().code());
        byteBuf.writeShortLE(64); // fixed header length
        byteBuf.writeShortLE(ctx.dialect().sameOrAfter(SmbDialect.SMB2_1) ? header.creditCharge() : 0);
        if (ctx.isRequest() && ctx.dialect().sameOrAfter(SmbDialect.SMB3_0)) {
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
        byteBuf.writeBytes(header.signature());
    }

    // Message bodies

    static SmbRequestMessage decodeRequestMessage(final ByteBuf byteBuf, final SmbCommand command,
        final CodecContext ctx) {

        return switch (command) {
            case SMB2_NEGOTIATE -> decodeNegotiateRequest(byteBuf, ctx);

            default -> throw new SmbException("no request decoder for command " + command);
        };
    }

    static void encodeRequest(final ByteBuf byteBuf, final SmbRequestMessage message, final CodecContext ctx) {
        switch (message) {
            case Smb2NegotiateRequest req -> encodeNegotiateRequest(byteBuf, req, ctx);

            default -> throw new SmbException("no request encoder for class " + message.getClass());
        }
    }

    static SmbResponseMessage decodeResponseMessage(final ByteBuf byteBuf, final SmbCommand command,
        final CodecContext ctx) {

        return switch (command) {
            case SMB2_NEGOTIATE -> decodeNegotiateResponse(byteBuf, ctx);

            default -> throw new SmbException("no response decoder for command " + command);
        };
    }

    static void encodeResponce(final ByteBuf byteBuf, final SmbResponseMessage message, final CodecContext ctx) {
        switch (message) {
            case Smb2NegotiateResponse resp -> encodeNegotiateResponse(byteBuf, resp, ctx);

            default -> throw new SmbException("no response encoder for class " + message.getClass());
        }
    }

    // SMB2 NEGOTIATE Request (MS-SMB2 #2.2.3)

    private static SmbRequestMessage decodeNegotiateRequest(final ByteBuf byteBuf, final CodecContext ctx) {
        final var request = new Smb2NegotiateRequest();
        final var structureSize = byteBuf.readShortLE(); // TODO validate expected const value = 36
        final var dialectCount = byteBuf.readShortLE();
        request.setSecurityMode(new Flags<>(byteBuf.readUnsignedShortLE()));
        byteBuf.skipBytes(2);
        request.setCapabilities(new Flags<>(byteBuf.readIntLE()));
        request.setClientGuid(Utils.readGuid(byteBuf));
        final var tempBuf = byteBuf.readBytes(8);
        final var dialects = new ArrayList<SmbDialect>(dialectCount);
        for (int i = 0; i < dialectCount; i++) {
            final var dialect = SmbDialect.fromCode(byteBuf.readUnsignedShortLE());
            if (dialect != SmbDialect.Unknown) {
                dialects.add(dialect);
            }
        }
        request.setDialects(List.copyOf(dialects));
        if (byteBuf.readableBytes() > 0 && dialects.contains(SmbDialect.SMB3_1_1)) {
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

        if (request.dialects().contains(SmbDialect.SMB3_1_1)) {
            // write negotiation contexts if dialects contains SMB 3.1.1
            final var negCtxs = request.negotiateContexts();
            if (negCtxs != null && !negCtxs.isEmpty()) {
                // add padding if necessary to ensure first context is 8 byte aligned;
                final var align = (byteBuf.writerIndex() - msgStartPos) % 8;
                if (align != 0) {
                    byteBuf.writeBytes(new byte[8-align]);
                }
                byteBuf.setIntLE(negCtxInfoPos, byteBuf.writerIndex() - ctx.headerStartPosition()); // offset
                byteBuf.setShortLE(negCtxInfoPos + 4, negCtxs.size()); // cnt
                    // todo encode contexts
            }
        }
    }

    // SMB2 NEGOTIATE Response (MS-SMB2 #2.2.4)

    private static SmbResponseMessage decodeNegotiateResponse(final ByteBuf byteBuf, final CodecContext ctx) {

        final var response = new Smb2NegotiateResponse();
        final var structureSize = byteBuf.readShortLE(); // todo validate expected const value = 65




        return response;
    }

    private static void encodeNegotiateResponse(final ByteBuf byteBuf, final Smb2NegotiateResponse response,
        final CodecContext ctx) {
    }
}
