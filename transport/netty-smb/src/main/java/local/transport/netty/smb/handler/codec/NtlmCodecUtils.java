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

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.EnumMap;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.spnego.MechType;
import local.transport.netty.smb.protocol.spnego.ntlm.LmChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAuthenticateMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvId;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvPairs;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmChallenge;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmChallengeMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmChannelBindingHash;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmMessageType;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiationFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmSingleHostData;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmVersion;

final class NtlmCodecUtils {

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
                msg.setDomainName(getNtlmString(byteBuf, readNtlmFieldRef(byteBuf, startPos), false));
                msg.setWorkstationName(getNtlmString(byteBuf, readNtlmFieldRef(byteBuf, startPos), false));
                msg.setVersion(readNtlmVersion(byteBuf));
                yield msg;
            }
            case NtLmChallenge -> {
                final var msg = new NtlmChallengeMessage();
                final var targetNameRef = readNtlmFieldRef(byteBuf, startPos);
                msg.setNegotiationFlags(new Flags<>(byteBuf.readIntLE()));
                msg.setServerChallenge(NtlmChallenge.encoded(Utils.readToByteArray(byteBuf, 8)));
                byteBuf.skipBytes(8); // reserved
                final var targetInfoRef = readNtlmFieldRef(byteBuf, startPos);
                msg.setVersion(readNtlmVersion(byteBuf));
                final var unicode = isUnicode(msg.negotiationFlags());
                msg.setTargetName(getNtlmString(byteBuf, targetNameRef, unicode));
                msg.setTargetInfo(getNtmlAvPair(byteBuf, targetInfoRef));
                yield msg;
            }
            case NtLmAuthenticate -> {
                final var msg = new NtlmAuthenticateMessage();
                final var lmChallengeRespRef = readNtlmFieldRef(byteBuf, startPos);
                final var ntChallengeRespRef = readNtlmFieldRef(byteBuf, startPos);
                final var domainNameRef = readNtlmFieldRef(byteBuf, startPos);
                final var userNameRef = readNtlmFieldRef(byteBuf, startPos);
                final var workstationNameRef = readNtlmFieldRef(byteBuf, startPos);
                final var sessionKeyRef = readNtlmFieldRef(byteBuf, startPos);
                msg.setNegotiationFlags(new Flags<>(byteBuf.readIntLE()));
                msg.setVersion(readNtlmVersion(byteBuf));
                msg.setMic(Utils.readToByteArray(byteBuf, 16));
                boolean unicode = isUnicode(msg.negotiationFlags());
                msg.setDomainName(getNtlmString(byteBuf, domainNameRef, unicode));
                msg.setUserName(getNtlmString(byteBuf, userNameRef, unicode));
                msg.setWorkstationName(getNtlmString(byteBuf, workstationNameRef, unicode));
                boolean ntlmv2 = false; //todo identify version from flags
                msg.setLmChallengeResponse(getLmChallengeResponse(byteBuf, lmChallengeRespRef, ntlmv2));
                msg.setNtChallengeResponse(getNtChallengeResponse(byteBuf, ntChallengeRespRef, ntlmv2));
                msg.setEncryptedRandomSessionKey(getNtlmBytes(byteBuf, sessionKeyRef));
                yield msg;
            }
            default -> null;
        };
    }

    static void encodeNtlmMessage(final ByteBuf byteBuf, final NtlmMessage msg) {
        // TODO
    }

    private static NtlmFieldRef readNtlmFieldRef(final ByteBuf byteBuf, final int startPos) {
        final var length = byteBuf.readUnsignedShortLE();
        byteBuf.skipBytes(2); // maxLength is same as length
        return new NtlmFieldRef(startPos + byteBuf.readIntLE(), length);
    }

    private static NtlmVersion readNtlmVersion(final ByteBuf byteBuf) {
        final var major = byteBuf.readUnsignedByte();
        final var minor = byteBuf.readUnsignedByte();
        final var build = byteBuf.readUnsignedShortLE();
        byteBuf.skipBytes(3);
        final var revision = byteBuf.readUnsignedByte();
        return new NtlmVersion(major, minor, build, revision);
    }

    private static boolean isUnicode(final Flags<NtlmNegotiationFlags> flags) {
        if (flags.get(NtlmNegotiationFlags.NTLMSSP_NEGOTIATE_UNICODE)) {
            return true;
        } else if (flags.get(NtlmNegotiationFlags.NTLM_NEGOTIATE_OEM)) {
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
        if (unicode) {
            final var sb = new StringBuilder();
            for (var i = 0; i < length; i += 2) {
                sb.appendCodePoint(byteBuf.getShortLE(pos + i));
            }
            return sb.toString();
        }
        return byteBuf.getCharSequence(pos, length, US_ASCII).toString();
    }

    private static NtlmAvPairs getNtmlAvPair(final ByteBuf byteBuf, final NtlmFieldRef fieldRef) {
        int pos = fieldRef.pos();
        final int limit = pos + fieldRef.length();
        final var attrMap = new EnumMap<NtlmAvId, Object>(NtlmAvId.class);
        while (pos < limit && (byteBuf.capacity() - pos) > 4) {
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
                    case MsvAvTimestamp -> Utils.unixMillisFromFiletime(byteBuf.getLongLE(pos));
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
