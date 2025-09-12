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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.spnego.MechToken;
import local.transport.netty.smb.protocol.spnego.MechType;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexChecksum;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexExchangeMessage;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexExtension;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexMessage;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexMessageHeader;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexMessageSequence;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexMessageType;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexNegoMessage;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexVerifyMessage;

final class NegoexCodecUtils {

    private NegoexCodecUtils() {
        // utility class
    }

    private record NegoexVector(int pos, int size) {
    }

    static MechToken decodeNegoexMessage(final ByteBuf byteBuf) {
        final var messages = new ArrayList<NegoexMessage>();
        int nextPos = byteBuf.readerIndex();
        while (nextPos < byteBuf.capacity()) {
            byteBuf.readerIndex(nextPos);
            final var nextMsg = decodeNextNegoexMessage(byteBuf);
            if (nextMsg instanceof NegoexMessageHeader nextHeader) {
                messages.add(nextMsg);
                nextPos += nextHeader.cbMessageLength();
            } else {
                break;
            }
        }
        if (messages.isEmpty()) {
            return null;
        }
        return messages.size() == 1 ? messages.getFirst() : new NegoexMessageSequence(List.copyOf(messages));
    }

    private static NegoexMessage decodeNextNegoexMessage(final ByteBuf byteBuf) {
        final var startPos = byteBuf.readerIndex();
        System.out.printf("decode from pos %d, readable %d %n", startPos, byteBuf.readableBytes());
        final var signature = Utils.readToByteArray(byteBuf, 8);
        if (!Arrays.equals(signature, MechType.NEGOEX.signature())) {
            return null;
        }
        final var msgType = NegoexMessageType.fromCode(byteBuf.readIntLE());
        return switch (msgType) {
            case MESSAGE_TYPE_INITIATOR_NEGO, MESSAGE_TYPE_ACCEPTOR_NEGO -> readNegoMessage(byteBuf, msgType, startPos);
            case MESSAGE_TYPE_INITIATOR_META_DATA, MESSAGE_TYPE_ACCEPTOR_META_DATA, MESSAGE_TYPE_CHALLENGE,
                 MESSAGE_TYPE_AP_REQUEST -> readExchangeMessage(byteBuf, msgType, startPos);
            case MESSAGE_TYPE_VERIFY -> readVerifyMessage(byteBuf, startPos);

            default -> null;
        };
    }

    static void encodeNegoexMessage(final ByteBuf byteBuf, final NegoexMessage msg) {

    }

    private static NegoexVector readVector(final ByteBuf byteBuf, final int startPos) {
        return new NegoexVector(startPos + byteBuf.readIntLE(), byteBuf.readUnsignedShortLE());
    }

    private static void readHeader(final ByteBuf byteBuf, final NegoexMessageHeader header) {
        header.setSequenceNum(byteBuf.readIntLE());
        header.setCbHeaderLength(byteBuf.readIntLE());
        header.setCbMessageLength(byteBuf.readIntLE());
        header.setConversationId(Utils.readGuid(byteBuf));
    }

    private static NegoexNegoMessage readNegoMessage(final ByteBuf byteBuf, final NegoexMessageType messageType,
        final int startPos) {

        final var msg = new NegoexNegoMessage(messageType);
        readHeader(byteBuf, msg);
        msg.setRandom(Utils.readToByteArray(byteBuf, 32));
        msg.setProtocolVersion(byteBuf.readLongLE());
        msg.setAuthSchemes(readAuthSchemeVector(byteBuf, startPos));
        msg.setExtensions(readExtensionVector(byteBuf, startPos));
        return msg;
    }

    private static NegoexExchangeMessage readExchangeMessage(final ByteBuf byteBuf, final NegoexMessageType messageType,
        final int startPos) {

        final var msg = new NegoexExchangeMessage(messageType);
        readHeader(byteBuf, msg);
        msg.setAuthScheme(Utils.readGuid(byteBuf));
        msg.setExchangeData(readByteVector(byteBuf, startPos));
        return msg;
    }

    private static NegoexVerifyMessage readVerifyMessage(final ByteBuf byteBuf, final int startPos){
        final var msg = new NegoexVerifyMessage();
        readHeader(byteBuf, msg);
        msg.setAuthScheme(Utils.readGuid(byteBuf));
        byteBuf.skipBytes(4); // checksum cbHeaderLength contant = 20
        final var checksumScheme = byteBuf.readIntLE();
        final var checksumType = byteBuf.readIntLE();
        msg.setChecksum(new NegoexChecksum(checksumScheme, checksumType, readByteVector(byteBuf, startPos)));
        return msg;
    }

    private static List<UUID> readAuthSchemeVector(final ByteBuf byteBuf, final int startPos) {
        final var vector = readVector(byteBuf, startPos);
        if (vector.size() > 0) {
            final var slice = byteBuf.slice(vector.pos(), vector.size() * 16);
            final var result = new ArrayList<UUID>(vector.size());
            for (int i = 0; i < vector.size(); i++) {
                result.add(Utils.readGuid(slice));
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    private static List<NegoexExtension> readExtensionVector(final ByteBuf byteBuf, final int startPos) {
        final var vector = readVector(byteBuf, startPos);
        if (vector.size() > 0) {
            final var slice = byteBuf.slice(vector.pos(), vector.size() * 10); // extType(4) + byteVector(6)
            final var result = new ArrayList<NegoexExtension>(vector.size());
            for (int i = 0; i < vector.size(); i++) {
                final var extType = slice.readIntLE();
                final var byteVector = readVector(slice, startPos);
                final var data = Utils.getByteArray(byteBuf, byteVector.pos(), byteVector.size());
                result.add(new NegoexExtension(extType, data));
            }
            return List.copyOf(result);
        }
        return List.of();
    }

    private static byte[] readByteVector(final ByteBuf byteBuf, final int startPos) {
        final var vector = readVector(byteBuf, startPos);
        if (vector.size() > 0) {
            return Utils.getByteArray(byteBuf, vector.pos(), vector.size());
        }
        return new byte[0];
    }

}
