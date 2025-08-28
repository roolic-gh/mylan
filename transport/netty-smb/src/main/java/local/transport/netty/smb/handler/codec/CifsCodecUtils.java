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
import java.nio.charset.StandardCharsets;
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
import local.transport.netty.smb.protocol.cifs.CifsSmbHeader;
import local.transport.netty.smb.protocol.cifs.SmbComNegotiateRequest;
import local.transport.netty.smb.protocol.cifs.SmbComNegotiateResponse;

final class CifsCodecUtils {

    private CifsCodecUtils() {
        // utility class
    }

    // Header (MS-CIFS #2.2.3.1 The SMB Header)

    static SmbHeader decodeHeader(final ByteBuf byteBuf) {
        final var header = new CifsSmbHeader();
        // protocol 4 bytes already extracted, read omitted
        header.setCommand(SmbCommand.fromCode(byteBuf.readByte(), ProtocolVersion.CIFS_SMB));
        header.setStatus(SmbError.fromCode(byteBuf.readIntLE()));
        header.setFlags(new Flags<>(byteBuf.readUnsignedByte()));
        header.setFlags2(new Flags<>(byteBuf.readUnsignedShortLE()));
        header.setProcessIdHigh(byteBuf.readUnsignedShortLE());
        header.setSecurityFeatures(Utils.readToByteArray(byteBuf, 8));
        byteBuf.skipBytes(2); // reserved
        header.setTreeId(byteBuf.readUnsignedShortLE());
        header.setProcessIdLow(byteBuf.readUnsignedShortLE());
        header.setUserId(byteBuf.readUnsignedShortLE());
        header.setMultiplexId(byteBuf.readUnsignedShortLE());
        return header;
    }

    static void encodeHeader(final ByteBuf byteBuf, final CifsSmbHeader header) {
        // todo validate field values to avoid NPE
        byteBuf.writeIntLE(ProtocolVersion.CIFS_SMB.code());
        byteBuf.writeByte(header.command().code());
        byteBuf.writeIntLE(header.getStatus().code());
        byteBuf.writeByte(header.getFlags().asIntValue());
        byteBuf.writeShortLE(header.getFlags2().asIntValue());
        byteBuf.writeShortLE(header.getProcessIdHigh());
        byteBuf.writeBytes(header.getSecurityFeatures());
        byteBuf.writeShort(0); // reserved
        byteBuf.writeShortLE(header.getTreeId());
        byteBuf.writeShortLE(header.getProcessIdLow());
        byteBuf.writeShortLE(header.getUserId());
        byteBuf.writeShortLE(header.getMultiplexId());
    }

    // Message bodies

    static SmbRequestMessage decodeRequestMessage(final ByteBuf byteBuf, final SmbCommand command) {
        return switch (command) {
            case SMB_COM_NEGOTIATE -> decodeNegotiateRequest(byteBuf);

            default -> throw new SmbException("no request decoder for command " + command);
        };
    }

    static void encodeRequest(final ByteBuf byteBuf, final SmbRequestMessage message) {
        switch (message) {
            case SmbComNegotiateRequest req -> encodeNegotiateRequest(req);

            default -> throw new SmbException("no request encoder for class " + message.getClass());
        }
    }

     static SmbResponseMessage decodeResponseMessage(final ByteBuf byteBuf, final SmbCommand command) {
        return switch (command) {
            case SMB_COM_NEGOTIATE -> decodeNegotiateResponse(byteBuf);

            default -> throw new SmbException("no response decoder for command " + command);
        };
    }

    static void encodeResponce(final ByteBuf byteBuf, final SmbResponseMessage message) {
        switch (message) {
            case SmbComNegotiateResponse resp -> encodeNegotiateResponse(resp);

            default -> throw new SmbException("no response encoder for class " + message.getClass());
        }
    }

    // SMB_COM_NEGOTIATE Request (MS-CIFS #2.2.4.52.1)

    private static SmbRequestMessage decodeNegotiateRequest(final ByteBuf byteBuf) {
        byteBuf.skipBytes(1); // wordcount always 0
        final var length = byteBuf.readUnsignedShortLE();
        final var maxIndex = byteBuf.readerIndex() + length;

        final var dialects = new ArrayList<SmbDialect>();
        byte[] bytes;
        while ((bytes = Utils.readSmbBuffer(byteBuf, maxIndex)) !=null ){
            final var dialect = SmbDialect.fromIdentifier(new String(bytes, StandardCharsets.US_ASCII));
            if (dialect != SmbDialect.Unknown) {
                dialects.add(dialect);
            }
        }

        final var request = new SmbComNegotiateRequest();
        request.setDialects(List.copyOf(dialects));
        return request;
    }

    private static void encodeNegotiateRequest(final SmbComNegotiateRequest request) {
    }

    // SMB_COM_NEGOTIATE Response (MS-CIFS #2.2.4.52.1)

    private static SmbResponseMessage decodeNegotiateResponse(final ByteBuf byteBuf) {
        final var response = new SmbComNegotiateResponse();
        // TODO
        return response;
    }

    private static void encodeNegotiateResponse(final SmbComNegotiateResponse response) {
    }
}
