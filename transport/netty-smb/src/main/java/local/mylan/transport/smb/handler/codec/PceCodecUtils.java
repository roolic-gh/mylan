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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import local.mylan.transport.smb.Utils;
import local.mylan.transport.smb.protocol.Flags;
import local.mylan.transport.smb.protocol.fscc.Blob;
import local.mylan.transport.smb.protocol.pcerpc.NdrFormatLabel;
import local.mylan.transport.smb.protocol.pcerpc.PceBind;
import local.mylan.transport.smb.protocol.pcerpc.PceBindAck;
import local.mylan.transport.smb.protocol.pcerpc.PceMessage;
import local.mylan.transport.smb.protocol.pcerpc.PceRequest;
import local.mylan.transport.smb.protocol.pcerpc.PceResponse;
import local.mylan.transport.smb.protocol.pcerpc.PceUnsupported;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes.ContextElement;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes.ResultElement;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes.Syntax;
import local.mylan.transport.smb.protocol.pcerpc.PduDataTypes.Version;
import local.mylan.transport.smb.protocol.pcerpc.PduType;
import local.mylan.transport.smb.protocol.srvs.SrvsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PceCodecUtils {

    private static final Logger LOG = LoggerFactory.getLogger(PceCodecUtils.class);

    private static final Set<Version> SUPPORTED_VERSIONS =
        Set.of(new Version(5, 0), new Version(5, 1));
    private static final Set<Integer> SUPPORTED_PDU_TYPE_CODES =
        Set.of(PduType.bind.code(), PduType.bind_ack.code(), PduType.request.code(), PduType.response.code());

    private PceCodecUtils() {
        // utility class
    }

    static boolean isPceBuf(final ByteBuf byteBuf) {
         /* Validate BLOB as PCE message using following header attributes:
         - at least 16 bytes length
         - @pos 0, 1 contains version 5.0 or 5.1 (#12.4.2 Protocol Version Numbers)
         - contains exact packet length (assuming LE order) -- msg size @pos 8 + auth size @pos 10
         */
        if (byteBuf.readableBytes() < 16) {
            return false;
        }
        final var version = new Version(byteBuf.getUnsignedByte(0), byteBuf.getUnsignedByte(1));
        final var pceLen = byteBuf.getUnsignedShortLE(8) + byteBuf.getUnsignedShortLE(10);
        return SUPPORTED_VERSIONS.contains(version) && byteBuf.readableBytes() == pceLen;
    }

    static Object decode(final ByteBuf byteBuf) {
        try {
            if (isPceBuf(byteBuf)) {
                return decodePceMessage(byteBuf.slice());
            }
        } catch (Exception e) {
            LOG.error("Error decoding PCE/RPC packet", e);
        }
        return new Blob(ByteBufUtil.getBytes(byteBuf));
    }

    private static PceMessage decodePceMessage(final ByteBuf byteBuf) {
        final var startPos = byteBuf.readerIndex();
        final var pduVersion = new PduDataTypes.Version(byteBuf.readUnsignedByte(), byteBuf.readUnsignedByte());
        final var pduType = PduType.fromCode(byteBuf.readUnsignedByte());
        final var pfcFlags = new Flags<PduDataTypes.PfcFlags>(byteBuf.readUnsignedByte());
        final var ndrFormatLabel = NdrFormatLabel.fromBytes(Utils.readToByteArray(byteBuf, 4));
        if (ndrFormatLabel.character() != NdrFormatLabel.Character.ASCII
            || ndrFormatLabel.byteOrder() != NdrFormatLabel.ByteOrder.LITTLE_ENDIAN) {
            // unexpected formatting withn SMB2
            throw new IllegalArgumentException(
                "Unsupported byte order (%s) and/or charset (%s) within PCE message".formatted(
                    ndrFormatLabel.byteOrder(), ndrFormatLabel.character()));
        }
        final var length = byteBuf.readUnsignedShort();
        final var authLength = byteBuf.readUnsignedShort();
        final var callId = byteBuf.readIntLE();
        return switch (pduType) {
            case bind -> {
                final var bind = new PceBind(pduVersion, pfcFlags, ndrFormatLabel, callId);
                bind.setMaxTransmitFragmentSize(byteBuf.readUnsignedShortLE());
                bind.setMaxReceiveFragmentSize(byteBuf.readUnsignedShortLE());
                bind.setAssocGroupId(byteBuf.readIntLE());
                bind.setContexts(readContexts(byteBuf));
                yield bind;
            }
            case bind_ack -> {
                final var bindAck = new PceBindAck(pduVersion, pfcFlags, ndrFormatLabel, callId);
                bindAck.setMaxTransmitFragmentSize(byteBuf.readUnsignedShortLE());
                bindAck.setMaxReceiveFragmentSize(byteBuf.readUnsignedShortLE());
                bindAck.setAssocGroupId(byteBuf.readIntLE());
                bindAck.setSecAddress(readSecAddress(byteBuf));
                alignReader(byteBuf, startPos, 4);
                bindAck.setResults(readResults(byteBuf));
                yield bindAck;
            }
            case request -> {
                final var request = new PceRequest(pduVersion, pfcFlags, ndrFormatLabel, callId);
                final var allocHint = byteBuf.readIntLE();
                request.setContextId(byteBuf.readUnsignedShortLE());
                final var opnum = byteBuf.readUnsignedShortLE();
                if (pfcFlags.get(PduDataTypes.PfcFlags.PFC_OBJECT_UUID)) {
                    request.setObjectUid(Utils.readGuid(byteBuf));
                }
                request.setObject(
                    SrvsCodecUtils.decodeRequest(byteBuf.slice(byteBuf.readerIndex(), allocHint), opnum));
                yield request;
            }
            case response -> {
                final var response = new PceResponse(pduVersion, pfcFlags, ndrFormatLabel, callId);
                final var allocHint = byteBuf.readIntLE();
                response.setContextId(byteBuf.readUnsignedShortLE());
                response.setCancelCount(byteBuf.readUnsignedByte());
                byteBuf.skipBytes(1); // reserved
                // embedded object to be decoded explicitly from logic layer because opnum isn't in response
                response.setObject(new Blob(Utils.readToByteArray(byteBuf, allocHint)));
                yield response;
            }
            default -> new PceUnsupported(pduType, callId);
        };
    }

    static void encode(final ByteBuf byteBuf, final PceMessage pce) {
        verifyFormat(pce.ndrFormatLabel());
        final var startPos = byteBuf.writerIndex();
        byteBuf.writeByte(pce.pduVersion().major());
        byteBuf.writeByte(pce.pduVersion().minor());
        byteBuf.writeByte(pce.type().code());
        byteBuf.writeByte(pce.pfcFlags().asIntValue());
        byteBuf.writeBytes(pce.ndrFormatLabel().bytes());
        final var lengthPos = byteBuf.writerIndex();
        byteBuf.writeZero(4); // total fragment length (short) + auth length (short)
        byteBuf.writeIntLE(pce.callId());
        switch (pce) {
            case PceBind bind -> {
                byteBuf.writeShortLE(bind.maxTransmitFragmentSize());
                byteBuf.writeShortLE(bind.maxReceiveFragmentSize());
                byteBuf.writeIntLE(bind.assocGroupId());
                writeContexts(byteBuf, bind.contexts());
            }
            case PceBindAck bindAck -> {
                byteBuf.writeShortLE(bindAck.maxTransmitFragmentSize());
                byteBuf.writeShortLE(bindAck.maxReceiveFragmentSize());
                byteBuf.writeIntLE(bindAck.assocGroupId());
                writeSecAddress(byteBuf, bindAck.secAddress());
                alignWriter(byteBuf, startPos, 4);
                writeResults(byteBuf, bindAck.results());
            }
            case PceRequest request -> {
                final var allocHintPos = byteBuf.writerIndex();
                byteBuf.writeZero(4); // alloc hint
                byteBuf.writeShortLE(request.contextId());
                byteBuf.writeShortLE(request.opnum());
                if (request.pfcFlags().get(PduDataTypes.PfcFlags.PFC_OBJECT_UUID)) {
                    Utils.writeGuid(byteBuf, request.objectUid());
                }
                final var blobPos = byteBuf.writerIndex();
                switch (request.object()) {
                    case Blob blob -> byteBuf.writeBytes(blob.bytes());
                    case SrvsMessage srvsMsg -> SrvsCodecUtils.encodeRequest(byteBuf, srvsMsg);
                    default -> {
                    }
                }
                final var allocHint = byteBuf.writerIndex() - blobPos;
                byteBuf.setIntLE(allocHintPos, allocHint);
            }
            case PceResponse response -> {
                final var allocHintPos = byteBuf.writerIndex();
                byteBuf.writeZero(4); // alloc hint
                byteBuf.writeShortLE(response.contextId());
                byteBuf.writeByte(response.cancelCount());
                byteBuf.writeZero(1);
                final var blobPos = byteBuf.writerIndex();
                switch (response.object()) {
                    case Blob blob -> byteBuf.writeBytes(blob.bytes());
                    case SrvsMessage srvsMsg -> SrvsCodecUtils.encodeResponse(byteBuf, srvsMsg);
                    default -> {
                    }
                }
                final var allocHint = byteBuf.writerIndex() - blobPos;
                byteBuf.setIntLE(allocHintPos, allocHint);
            }
            default -> {
            }
        }
        // set length value
        byteBuf.setShortLE(lengthPos, byteBuf.writerIndex() - startPos);
    }

    private static void verifyFormat(final NdrFormatLabel fl) {
        if (fl.byteOrder() != NdrFormatLabel.ByteOrder.LITTLE_ENDIAN) {
            throw new IllegalArgumentException("Big-endian byte order is not supported by PCE/RPC codec");
        }
        if (fl.character() != NdrFormatLabel.Character.ASCII) {
            throw new IllegalArgumentException("EBCDIC character format is not supported by PCE/RPC codec");
        }
    }

    static void alignWriter(final ByteBuf byteBuf, final int startPos, final int mod) {
        final var overflow = (byteBuf.writerIndex() - startPos) % mod;
        if (overflow > 0) {
            byteBuf.writeZero(mod - overflow);
        }
    }

    static void alignReader(final ByteBuf byteBuf, final int startPos, final int mod) {
        final var overflow = (byteBuf.readerIndex() - startPos) % mod;
        if (overflow > 0) {
            byteBuf.skipBytes(mod - overflow);
        }
    }

    private static void writeSecAddress(final ByteBuf byteBuf, final String value) {
        final var bytes = value.getBytes(StandardCharsets.US_ASCII);
        byteBuf.writeShortLE(bytes.length + 1);
        byteBuf.writeBytes(bytes);
        byteBuf.writeZero(1); // trailing zero char
    }

    private static String readSecAddress(final ByteBuf byteBuf) {
        final var byteCount = byteBuf.readUnsignedShortLE();
        final var bytes = Utils.readToByteArray(byteBuf, byteCount - 1); // exclude trailing zero char
        byteBuf.skipBytes(1); // trailing zero char
        return new String(bytes, StandardCharsets.US_ASCII);
    }

    private static List<ContextElement> readContexts(final ByteBuf byteBuf) {
        final int ctxCount = byteBuf.readUnsignedByte();
        byteBuf.skipBytes(3); // padding;
        if (ctxCount == 0) {
            return List.of();
        }
        final var ctxList = new ArrayList<ContextElement>(ctxCount);
        for (int i = 0; i < ctxCount; i++) {
            final var id = byteBuf.readUnsignedShortLE();
            final int transfStxCount = byteBuf.readUnsignedByte();
            byteBuf.skipBytes(1); // padding
            final var abstractSyntax = readSyntax(byteBuf);
            final List<Syntax> transferSyntaxes;
            if (transfStxCount > 0) {
                final var list = new ArrayList<Syntax>(transfStxCount);
                for (int j = 0; j < transfStxCount; j++) {
                    list.add(readSyntax(byteBuf));
                }
                transferSyntaxes = List.copyOf(list);
            } else {
                transferSyntaxes = List.of();
            }
            ctxList.add(new ContextElement(id, abstractSyntax, transferSyntaxes));
        }
        return List.copyOf(ctxList);
    }

    private static void writeContexts(final ByteBuf byteBuf, final List<ContextElement> contexts) {

        byteBuf.writeByte(contexts.size());
        byteBuf.writeZero(3); // padding
        for (var ctx : contexts) {
            byteBuf.writeShortLE(ctx.contextId());
            byteBuf.writeByte(ctx.transferSyntaxes().size());
            byteBuf.writeZero(1); // padding;
            writeSyntax(byteBuf, ctx.abstractSyntax());
            ctx.transferSyntaxes().forEach(syntax -> writeSyntax(byteBuf, syntax));
        }
    }

    private static List<ResultElement> readResults(final ByteBuf byteBuf) {
        final int resCount = byteBuf.readUnsignedByte();
        byteBuf.skipBytes(3); // padding;
        if (resCount == 0) {
            return List.of();
        }
        final var resList = new ArrayList<ResultElement>(resCount);
        for (int i = 0; i < resCount; i++) {
            final var res = PduDataTypes.Result.fromCode(byteBuf.readUnsignedShortLE());
            final var reason = PduDataTypes.ProviderReason.fromCode(byteBuf.readUnsignedShortLE());
            final var syntax = readSyntax(byteBuf);
            resList.add(new ResultElement(res, reason, syntax));
        }
        return List.copyOf(resList);
    }

    private static void writeResults(final ByteBuf byteBuf, final List<ResultElement> results) {
        byteBuf.writeByte(results.size());
        byteBuf.writeZero(3); // padding
        for (var res : results) {
            byteBuf.writeShortLE(res.result().code());
            byteBuf.writeShortLE(res.reason().code());
            writeSyntax(byteBuf, res.transferSyntax());
        }
    }

    private static Syntax readSyntax(final ByteBuf byteBuf) {
        final var uuid = Utils.readGuid(byteBuf);
        final var verMajor = byteBuf.readUnsignedShortLE();
        final var verMinor = byteBuf.readUnsignedShortLE();
        return new Syntax(uuid, new Version(verMajor, verMinor));
    }

    private static void writeSyntax(final ByteBuf byteBuf, final Syntax syntax) {
        Utils.writeGuid(byteBuf, syntax.uuid());
        byteBuf.writeShortLE(syntax.version().major());
        byteBuf.writeShortLE(syntax.version().minor());
    }
}
