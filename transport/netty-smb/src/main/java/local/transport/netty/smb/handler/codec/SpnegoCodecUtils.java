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
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.spnego.MechListMIC;
import local.transport.netty.smb.protocol.spnego.MechToken;
import local.transport.netty.smb.protocol.spnego.MechType;
import local.transport.netty.smb.protocol.spnego.NegState;
import local.transport.netty.smb.protocol.spnego.NegToken;
import local.transport.netty.smb.protocol.spnego.NegTokenInit;
import local.transport.netty.smb.protocol.spnego.NegTokenResp;
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
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERTags;
import org.bouncycastle.asn1.DLSequence;
import org.bouncycastle.asn1.DLTaggedObject;

final class SpnegoCodecUtils {
    private static final ASN1ObjectIdentifier SPNEGO_OID = new ASN1ObjectIdentifier("1.3.6.1.5.5.2");

    private SpnegoCodecUtils() {
        // utility class
    }

    static NegToken decodeNegToken(final ByteBuf byteBuf) {
        final ASN1TaggedObject root;
        final ASN1Sequence rootSeq;
        try (var in = new ASN1InputStream(new ByteBufInputStream(byteBuf))) {
            root = cast(in.readObject(), ASN1TaggedObject.class);
            rootSeq = root == null ? null : cast(root.getBaseObject(), ASN1Sequence.class);
        } catch (IOException e) {
            throw new SmbException("Error decoding NegToken", e);
        }
        if (root != null && rootSeq != null) {
            if (root.hasTagClass(BERTags.APPLICATION)) {
                // init token using SPNEGO head
                final var oid = rootSeq == null ? null : cast(rootSeq.getObjectAt(0), ASN1ObjectIdentifier.class);
                final var tokenSeq = rootSeq == null ? null : cast(rootSeq.getObjectAt(1), ASN1Sequence.class);
                if (!SPNEGO_OID.equals(oid) || tokenSeq == null) {
                    throw new SmbException("Invalid SPNEGO header");
                }
                return decodeNegTokenInit(tokenSeq);
            } else {
                return decodeNegTokenResp(rootSeq);
            }
        }
        throw new SmbException("Unexpected SPNEGO token structure");
    }

    static void encodeNegToken(final ByteBuf byteBuf, final NegToken negToken) {
        final var rootSeq = switch (negToken) {
            case NegTokenInit init -> new DLTaggedObject(BERTags.APPLICATION, 0,
                new DLSequence(new ASN1Encodable[]{SPNEGO_OID, encodeNegTokenInit(init)}));
            case NegTokenResp resp -> encodeNegTokenResp(resp);
            default -> throw new SmbException("Unsupported NegToken type " + negToken);
        };
        try (var out = new ByteBufOutputStream(byteBuf, false)) {
            rootSeq.encodeTo(out);
        } catch (IOException e) {
            throw new SmbException("Error encoding NegToken", e);
        }
    }

    private static NegTokenInit decodeNegTokenInit(final ASN1Sequence tokenSeq) {
        byte[] mechTokenOctets = null;
        byte[] nechListMicOctets = null;
        final var negToken = new NegTokenInit();
        tokenSeq.forEach(
            item -> castOpt(item, ASN1TaggedObject.class).ifPresent(tagged -> {
                switch (tagged.getTagNo()) {
                    //  mechTypes [0] MechTypeList
                    case 0 -> castOpt(tagged.getBaseObject(), ASN1Sequence.class)
                        .ifPresent(seq -> {
                            final var mechTypes = new ArrayList<MechType>();
                            seq.forEach(oid -> {
                                final var mtype = toMechType(oid);
                                if (mtype != MechType.OTHER) {
                                    mechTypes.add(mtype);
                                }
                            });
                            negToken.setMechTypes(List.copyOf(mechTypes));
                        });
                    // reqFlags [1] ContextFlags OPTIONAL
                    // recommended to omit/ignore
                    // according to https://www.rfc-editor.org/rfc/rfc4178.html#section-4.2.1

                    // mechToken [2] OCTET STRING OPTIONAL
                    case 2 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechToken(MechToken.encoded(octets.getOctets())));
                    // mechListMIC [3] OCTET STRING  OPTIONAL
                    case 3 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechListMIC(MechListMIC.encoded(octets.getOctets())));
                    default -> {
                    }
                }
            }));
        // convert encoded fields into mechType specific
        negToken.setMechToken(decodeMechToken(negToken.mechToken(), negToken.optimisticMechType()));
        negToken.setMechListMIC(decodeMechListMIC(negToken.mechListMIC(), negToken.optimisticMechType()));
        return negToken;
    }

    private static ASN1TaggedObject encodeNegTokenInit(final NegTokenInit negToken) {
        return null;
    }

    private static NegTokenResp decodeNegTokenResp(final ASN1Sequence rootSeq) {
        final var negToken = new NegTokenResp();
        rootSeq.forEach(
            item -> castOpt(item, ASN1TaggedObject.class).ifPresent(tagged -> {
                switch (tagged.getTagNo()) {
                    // negState [0] ENUMERATED
                    case 0 -> castOpt(tagged, ASN1Enumerated.class)
                        .ifPresent(enm -> negToken.setState(NegState.fromCode(enm.getValue().intValue())));
                    // supportedMech [1] MechType OPTIONAL
                    case 1 -> negToken.setSupportedMech(toMechType(tagged));
                    // responseToken [2] OCTET STRING  OPTIONAL
                    case 2 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechToken(MechToken.encoded(octets.getOctets())));
                    // mechListMIC [3] OCTET STRING  OPTIONAL
                    case 3 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechListMIC(MechListMIC.encoded(octets.getOctets())));
                    default -> {
                    }
                }
            }));
        // detect and set mechType if not defined
        validateMechType(negToken);
        // convert encoded fields into mechType specific
        negToken.setMechToken(decodeMechToken(negToken.mechToken(), negToken.supportedMech()));
        negToken.setMechListMIC(decodeMechListMIC(negToken.mechListMIC(), negToken.supportedMech()));
        return negToken;
    }

    private static void validateMechType(final NegTokenResp negToken) {
        if (negToken.supportedMech() == null && negToken.mechToken() instanceof MechToken.EncodedMechToken emt) {
            final var signature = new byte[8];
            if (emt.bytes().length > 8) {
                System.arraycopy(emt.bytes(), 0, signature, 0, 8);
            }
            negToken.setSupportedMech(MechType.fromSignature(signature));
        }
    }

    private static ASN1TaggedObject encodeNegTokenResp(final NegTokenResp negToken) {
        return null;
    }

    private static MechType toMechType(final ASN1Encodable obj) {
        return castOpt(obj, ASN1ObjectIdentifier.class)
            .map(oid -> MechType.fromOid(oid.getId())).orElse(MechType.OTHER);
    }

    private static <T extends ASN1Primitive> Optional<T> castOpt(final ASN1Encodable obj, final Class<T> type) {
        return Optional.ofNullable(cast(obj, type));
    }

    private static <T extends ASN1Primitive> T cast(final ASN1Encodable obj, final Class<T> type) {
        if (type.isInstance(obj)) {
            return type.cast(obj);
        }
        return obj instanceof ASN1TaggedObject tagged ? cast(tagged.getBaseObject(), type) : null;
    }

    // Mechanism specific

    private static MechToken decodeMechToken(final MechToken encoded, final MechType mechType) {
        if (encoded instanceof MechToken.EncodedMechToken emt) {
            // todo decode negoex
            final var decoded = switch (mechType) {
                case NTLMSSP -> decodeNtlmMessage(Unpooled.wrappedBuffer(emt.bytes()));
                default -> null;
            };
            if (decoded != null) {
                return decoded;
            }
        }
        return encoded;
    }

    private static MechListMIC decodeMechListMIC(final MechListMIC encoded, final MechType mechType) {
        if (encoded instanceof MechListMIC.EncodedMechListMIC eml) {
            // todo decode depending on mechType
        }
        return encoded;
    }

    // NTLM

    private static NtlmMessage decodeNtlmMessage(final ByteBuf byteBuf) {
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

    private record NtlmFieldRef(int pos, int length) {
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
        return fieldRef.length <= 0 ? null :  Utils.getByteArray(byteBuf, fieldRef.pos(), fieldRef.length());
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
