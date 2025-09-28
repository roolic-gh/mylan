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

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16LE;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.EnumMap;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.details.NtlmMessageSignature;
import local.transport.netty.smb.protocol.spnego.MechType;
import local.transport.netty.smb.protocol.spnego.ntlm.LmChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.LmV1ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.LmV2ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAuthEncoder;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAuthenticateMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvId;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvPairs;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmChallengeMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmChannelBindingHash;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmMessageType;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmSingleHostData;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmV1ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmV2ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmV2ClientChallenge;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmVersion;

public final class NtlmCodecUtils {

    public static NtlmAuthEncoder AUTH_ENCODER = new NtlmAuthEncoder() {
        @Override
        public void encode(final ByteBuf byteBuf, final NtlmMessage message) {
            encodeNtlmMessage(byteBuf, message);
        }

        @Override
        public void encode(final ByteBuf byteBuf, final NtlmV2ClientChallenge challenge) {
            encodeNtlmV2ClientChallenge(byteBuf, challenge);
        }
    };

    private NtlmCodecUtils() {
        // utility class
    }

    private record NtlmFieldRef(int pos, int length) {

    }

    static NtlmMessage decodeNtlmMessage(final ByteBuf byteBuf) {
        final var startPos = byteBuf.readerIndex();
        final var signature = Utils.readToByteArray(byteBuf, 8);
        if (!Arrays.equals(signature, MechType.NTLMSSP.signature())) {
            return null;
        }
        final var msgType = NtlmMessageType.fromCode(byteBuf.readIntLE());
        return switch (msgType) {
            case NtLmNegotiate -> {
                final var msg = new NtlmNegotiateMessage();
                msg.setNegotiateFlags(new Flags<>(byteBuf.readIntLE()));
                msg.setDomainName(getNtlmString(byteBuf, readFieldRef(byteBuf, startPos), false));
                msg.setWorkstationName(getNtlmString(byteBuf, readFieldRef(byteBuf, startPos), false));
                msg.setVersion(readNtlmVersion(byteBuf));
                yield msg;
            }
            case NtLmChallenge -> {
                final var msg = new NtlmChallengeMessage();
                final var targetNameRef = readFieldRef(byteBuf, startPos);
                msg.setNegotiateFlags(new Flags<>(byteBuf.readIntLE()));
                msg.setServerChallenge(Utils.readToByteArray(byteBuf, 8));
                byteBuf.skipBytes(8); // reserved
                final var targetInfoRef = readFieldRef(byteBuf, startPos);
                msg.setVersion(readNtlmVersion(byteBuf));
                final var unicode = isUnicode(msg.negotiateFlags());
                msg.setTargetName(getNtlmString(byteBuf, targetNameRef, unicode));
                msg.setTargetInfo(getNtmlAvPair(byteBuf, targetInfoRef));
                yield msg;
            }
            case NtLmAuthenticate -> {
                final var msg = new NtlmAuthenticateMessage();
                final var lmChallengeRespRef = readFieldRef(byteBuf, startPos);
                final var ntChallengeRespRef = readFieldRef(byteBuf, startPos);
                final var domainNameRef = readFieldRef(byteBuf, startPos);
                final var userNameRef = readFieldRef(byteBuf, startPos);
                final var workstationNameRef = readFieldRef(byteBuf, startPos);
                final var sessionKeyRef = readFieldRef(byteBuf, startPos);
                msg.setNegotiateFlags(new Flags<>(byteBuf.readIntLE()));
                msg.setVersion(readNtlmVersion(byteBuf));
                msg.setMic(Utils.readToByteArray(byteBuf, 16));
                boolean unicode = isUnicode(msg.negotiateFlags());
                msg.setDomainName(getNtlmString(byteBuf, domainNameRef, unicode));
                msg.setUserName(getNtlmString(byteBuf, userNameRef, unicode));
                msg.setWorkstationName(getNtlmString(byteBuf, workstationNameRef, unicode));
                boolean ntlmv2 = false;
                msg.setLmChallengeResponse(getLmChallengeResponse(byteBuf, lmChallengeRespRef, ntlmv2));
                msg.setNtChallengeResponse(getNtChallengeResponse(byteBuf, ntChallengeRespRef, ntlmv2));
                msg.setEncryptedRandomSessionKey(getNtlmBytes(byteBuf, sessionKeyRef));
                yield msg;
            }
            default -> null;
        };
    }

    // expose method required for NTLM Auth Mechanism, operating binaries

    public static void encodeNtlmMessage(final ByteBuf byteBuf, final NtlmMessage ntlmMsg) {
        final var startPos = byteBuf.writerIndex();
        byteBuf.writeBytes(MechType.NTLMSSP.signature());
        byteBuf.writeIntLE(ntlmMsg.messageType().code());
        switch (ntlmMsg) {
            case NtlmNegotiateMessage msg -> {
                byteBuf.writeIntLE(msg.negotiateFlags().asIntValue());
                final var domainNamePos = reserveFieldRef(byteBuf);
                final var workstationNamePos = reserveFieldRef(byteBuf);
                writeNtlmVersion(byteBuf, msg.version());
                writeNtlmString(byteBuf, msg.domainName(), domainNamePos, startPos, false);
                writeNtlmString(byteBuf, msg.workstationName(), workstationNamePos, startPos, false);
            }
            case NtlmChallengeMessage msg -> {
                final var targetNamePos = reserveFieldRef(byteBuf);
                byteBuf.writeIntLE(msg.negotiateFlags().asIntValue());
                byteBuf.writeBytes(msg.serverChallenge());
                byteBuf.writeZero(8); // reserved
                final var targetInfoPos = reserveFieldRef(byteBuf);
                writeNtlmVersion(byteBuf, msg.version());
                final var unicode = isUnicode(msg.negotiateFlags());
                writeNtlmString(byteBuf, msg.targetName(), targetNamePos, startPos, unicode);
                writeNtmlAvPair(byteBuf, msg.targetInfo(), targetInfoPos, startPos);
            }
            case NtlmAuthenticateMessage msg -> {
                final var lmChallengePos = reserveFieldRef(byteBuf);
                final var ntChallengePos = reserveFieldRef(byteBuf);
                final var domainPos = reserveFieldRef(byteBuf);
                final var usernamePos = reserveFieldRef(byteBuf);
                final var workstationPos = reserveFieldRef(byteBuf);
                final var sessionKeyPos = reserveFieldRef(byteBuf);
                byteBuf.writeIntLE(msg.negotiateFlags().asIntValue());
                writeNtlmVersion(byteBuf, msg.version());
                if (msg.mic() == null) {
                    byteBuf.writeZero(16);
                } else {
                    byteBuf.writeBytes(msg.mic());
                }
                final var unicode = isUnicode(msg.negotiateFlags());
                writeLmChallengeResponse(byteBuf, msg.lmChallengeResponse(), lmChallengePos, startPos);
                writeNtChallengeResponse(byteBuf, msg.ntChallengeResponse(), ntChallengePos, startPos);
                writeNtlmString(byteBuf, msg.domainName(), domainPos, startPos, unicode);
                writeNtlmString(byteBuf, msg.userName(), usernamePos, startPos, unicode);
                writeNtlmString(byteBuf, msg.workstationName(), workstationPos, startPos, unicode);
                if (msg.encryptedRandomSessionKey() != null) {
                    final var offset = byteBuf.writerIndex() - startPos;
                    byteBuf.writeBytes(msg.encryptedRandomSessionKey());
                    setFieldRef(byteBuf, sessionKeyPos, offset, msg.encryptedRandomSessionKey().length);
                }
            }
            default -> {
                // not expected
            }
        }
    }

    static void encodeNtlmMessageSignature(final ByteBuf byteBuf, final NtlmMessageSignature nms) {
        byteBuf.writeIntLE(nms.version());
        if (nms.randomPad() != null) {
            byteBuf.writeBytes(nms.randomPad());
        }
        byteBuf.writeBytes(nms.checksum());
        byteBuf.writeIntLE(nms.seqNum());
    }

    private static void encodeNtlmV2ClientChallenge(final ByteBuf byteBuf, NtlmV2ClientChallenge cc) {
        byteBuf.writeByte(cc.respType());
        byteBuf.writeByte(cc.hiRespType());
        byteBuf.writeZero(6);
        byteBuf.writeLongLE(cc.timestamp());
        byteBuf.writeBytes(cc.clientChallenge());
        byteBuf.writeZero(4);
        writeNtmlAvPair(byteBuf, cc.avPairs());
        byteBuf.writeZero(4);
    }

    private static NtlmFieldRef readFieldRef(final ByteBuf byteBuf, final int startPos) {
        final var length = byteBuf.readUnsignedShortLE();
        byteBuf.skipBytes(2); // maxLength is same as length
        return new NtlmFieldRef(startPos + byteBuf.readIntLE(), length);
    }

    private static int reserveFieldRef(final ByteBuf byteBuf) {
        final var pos = byteBuf.writerIndex();
        byteBuf.writeZero(8);
        return pos;
    }

    private static void setFieldRef(final ByteBuf byteBuf, final int pos, final int offset, final int length) {
        if (length > 0) {
            byteBuf.setShortLE(pos, length);
            byteBuf.setIntLE(pos + 4, offset);
        }
    }

    private static NtlmVersion readNtlmVersion(final ByteBuf byteBuf) {
        final var major = byteBuf.readUnsignedByte();
        final var minor = byteBuf.readUnsignedByte();
        final var build = byteBuf.readUnsignedShortLE();
        byteBuf.skipBytes(3);
        final var revision = byteBuf.readUnsignedByte();
        return new NtlmVersion(major, minor, build, revision);
    }

    private static void writeNtlmVersion(final ByteBuf byteBuf, final NtlmVersion version) {
        if (version == null) {
            byteBuf.writeZero(8);
        } else {
            byteBuf.writeByte(version.major());
            byteBuf.writeByte(version.minor());
            byteBuf.writeShortLE(version.build());
            byteBuf.writeZero(3); // reserved
            byteBuf.writeByte(version.curRevision());
        }
    }

    private static boolean isUnicode(final Flags<NtlmNegotiateFlags> flags) {
        if (flags.get(NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_UNICODE)) {
            return true;
        } else if (flags.get(NtlmNegotiateFlags.NTLM_NEGOTIATE_OEM)) {
            return false;
        } else {
            throw new SmbException("Invalid NTLM packet. Either UNICODE or OEM flags require to be enabled");
        }
    }

    private static byte[] getNtlmBytes(final ByteBuf byteBuf, final NtlmFieldRef fieldRef) {
        return fieldRef.length <= 0 ? null : Utils.getByteArray(byteBuf, fieldRef.pos(), fieldRef.length());
    }

    private static String getNtlmString(final ByteBuf byteBuf, final NtlmFieldRef fieldRef, final boolean unicode) {
        return getNtlmString(byteBuf, fieldRef.pos(), fieldRef.length(), unicode);
    }

    private static String getNtlmString(final ByteBuf byteBuf, final int pos, final int length, boolean unicode) {
        if (length <= 0) {
            return null;
        }
        return byteBuf.getCharSequence(pos, length, unicode ? UTF_16LE: US_ASCII).toString();
    }

    private static void writeNtlmString(final ByteBuf byteBuf, final String value, final int fieldPos,
        final int startPos, boolean unicode) {

        if (value == null) {
            // write nothing, field ref length remain 0
            return;
        }
        final var valuePos = byteBuf.writerIndex();
        byteBuf.writeCharSequence(value, unicode ? UTF_16LE : US_ASCII);
        final var offset = valuePos - startPos;
        final var length = byteBuf.writerIndex() - valuePos;
        setFieldRef(byteBuf, fieldPos, offset, length);
    }

    private static void writeLmChallengeResponse(final ByteBuf byteBuf, final LmChallengeResponse lmChallengeResponse,
        final int fieldPos, final int startPos) {

        final var start = byteBuf.writerIndex();
        switch (lmChallengeResponse) {
            case LmChallengeResponse.EncodedLmChallengeResponse enc -> byteBuf.writeBytes(enc.bytes());
            case LmV1ChallengeResponse lm1 -> byteBuf.writeBytes(lm1.bytes());
            case LmV2ChallengeResponse lm2 -> byteBuf.writeBytes(lm2.bytes());
            default -> {
            }
        }
        final var offset = start - startPos;
        final var length = byteBuf.writerIndex() - start;
        setFieldRef(byteBuf, fieldPos, offset, length);
    }

    private static void writeNtChallengeResponse(final ByteBuf byteBuf, final NtChallengeResponse ntChallengeResponse,
        final int fieldPos, final int startPos) {

        final var start = byteBuf.writerIndex();
        switch (ntChallengeResponse) {
            case NtChallengeResponse.EncodedNtChallengeResponse enc -> byteBuf.writeBytes(enc.bytes());
            case NtlmV1ChallengeResponse nt1 -> byteBuf.writeBytes(nt1.response());
            case NtlmV2ChallengeResponse nt2 -> {
                byteBuf.writeBytes(nt2.ntProofStr());
                encodeNtlmV2ClientChallenge(byteBuf, nt2.clentChallenge());
            }
            default -> {
            }
        }
        final var offset = start - startPos;
        final var length = byteBuf.writerIndex() - start;
        setFieldRef(byteBuf, fieldPos, offset, length);
    }

    private static NtlmAvPairs getNtmlAvPair(final ByteBuf byteBuf, final NtlmFieldRef fieldRef) {
        int pos = fieldRef.pos();
        final int limit = pos + fieldRef.length();
        final var attrMap = new EnumMap<NtlmAvId, Object>(NtlmAvId.class);
        while (pos < limit && byteBuf.capacity() - pos > 4) {
            final var id = NtlmAvId.fromCode(byteBuf.getUnsignedShortLE(pos));
            if (id == NtlmAvId.MsvAvEOL) {
                break;
            }
            final var length = byteBuf.getUnsignedShortLE(pos + 2);
            pos += 4;
            if (length > 0) {
                final var value = switch (id) {
                    case MsvAvNbComputerName, MsvAvNbDomainName, MsvAvDnsComputerName, MsvAvDnsDomainName,
                         MsvAvTargetName, MsvAvDnsTreeName -> getNtlmString(byteBuf, pos, length, true);
                    case MsvAvTimestamp -> byteBuf.getLongLE(pos);
                    case MsvAvFlags -> new Flags<NtlmAvFlags>(byteBuf.readIntLE());
                    case MsvAvChannelBindings -> new NtlmChannelBindingHash(Utils.getByteArray(byteBuf, pos, length));
                    case MsvAvSingleHost -> new NtlmSingleHostData(
                        Utils.getByteArray(byteBuf, pos + 8, 8), Utils.getByteArray(byteBuf, pos + 16, 32));
                    default -> null;
                };
                if (value != null) {
                    attrMap.put(id, value);
                }
                pos += length;
            }
        }
        return new NtlmAvPairs(attrMap);
    }

    private static void writeNtmlAvPair(final ByteBuf byteBuf, final NtlmAvPairs avp, final int fieldPos,
        final int startPos) {

        final var mapPos = byteBuf.writerIndex();
        writeNtmlAvPair(byteBuf, avp);
        final var offset = mapPos - startPos;
        final var length = byteBuf.writerIndex() - mapPos;
        setFieldRef(byteBuf, fieldPos, offset, length);
    }

    private static void writeNtmlAvPair(final ByteBuf byteBuf, final NtlmAvPairs avp) {
        avp.asMap().forEach((id, value) -> {
            byteBuf.writeShortLE(id.code());
            byteBuf.writeShortLE(0); // reserve for length
            if (value != null) {
                final var valuePos = byteBuf.writerIndex();
                switch (id) {
                    case MsvAvNbComputerName, MsvAvNbDomainName, MsvAvDnsComputerName, MsvAvDnsDomainName,
                         MsvAvTargetName, MsvAvDnsTreeName -> byteBuf.writeCharSequence((String) value, UTF_16LE);
                    case MsvAvTimestamp -> byteBuf.writeLongLE((Long) value);
                    case MsvAvFlags -> byteBuf.writeIntLE(((Flags<?>) value).asIntValue());
                    case MsvAvChannelBindings -> byteBuf.writeBytes(((NtlmChannelBindingHash) value).hash());
                    case MsvAvSingleHost -> {
                        final var data = (NtlmSingleHostData) value;
                        byteBuf.writeIntLE(48); // const field length
                        byteBuf.writeZero(4); // Z4
                        byteBuf.writeBytes(data.customData()); // 8 bytes
                        byteBuf.writeBytes(data.machineId()); // 32 bytes
                    }
                    default -> {
                    }
                }
                final var valueLength = byteBuf.writerIndex() - valuePos;
                byteBuf.setShortLE(valuePos - 2, valueLength);
            }
        });
        byteBuf.writeShortLE(NtlmAvId.MsvAvEOL.code());
        byteBuf.writeShortLE(0);
    }

    private static LmChallengeResponse getLmChallengeResponse(final ByteBuf byteBuf, final NtlmFieldRef fieldRef,
        final boolean v2) {
        return fieldRef.length <= 0 ? null : new LmChallengeResponse.EncodedLmChallengeResponse(
            Utils.getByteArray(byteBuf, fieldRef.pos, fieldRef.length));
    }

    private static NtChallengeResponse getNtChallengeResponse(final ByteBuf byteBuf, final NtlmFieldRef fieldRef,
        final boolean v2) {
        return fieldRef.length <= 0 ? null : new NtChallengeResponse.EncodedNtChallengeResponse(
            Utils.getByteArray(byteBuf, fieldRef.pos, fieldRef.length));
    }

}
