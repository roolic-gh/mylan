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
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.details.NtlmMessageSignature;
import local.transport.netty.smb.protocol.spnego.ContainsSelfEncoded;
import local.transport.netty.smb.protocol.spnego.MechListMIC;
import local.transport.netty.smb.protocol.spnego.MechToken;
import local.transport.netty.smb.protocol.spnego.MechType;
import local.transport.netty.smb.protocol.spnego.NegState;
import local.transport.netty.smb.protocol.spnego.NegToken;
import local.transport.netty.smb.protocol.spnego.NegTokenInit;
import local.transport.netty.smb.protocol.spnego.NegTokenResp;
import local.transport.netty.smb.protocol.spnego.negoex.NegoexMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmMessage;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERTags;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;

final class SpnegoCodecUtils {
    private static final ASN1ObjectIdentifier SPNEGO_OID = new ASN1ObjectIdentifier("1.3.6.1.5.5.2");
    private static final byte[] NIL = new byte[0];

    private SpnegoCodecUtils() {
        // utility class
    }

    static NegToken decodeNegToken(final ByteBuf byteBuf) {
        if (byteBuf.readableBytes() == 0) {
            return null;
        }
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
        if (negToken == null) {
            return;
        }
        final var rootSeq = switch (negToken) {
            case NegTokenInit init -> new DERTaggedObject(false, BERTags.APPLICATION, 0,
                new DERSequence(new ASN1Encodable[]{SPNEGO_OID, encodeNegTokenInit(init, byteBuf.alloc())}));
            case NegTokenResp resp -> encodeNegTokenResp(resp, byteBuf.alloc());
            default -> throw new SmbException("Unsupported NegToken type " + negToken);
        };
        try (var out = new ByteBufOutputStream(byteBuf, false)) {
            rootSeq.encodeTo(out, ASN1Encoding.DER);
        } catch (IOException e) {
            throw new SmbException("Error encoding NegToken", e);
        }
    }

    private static NegTokenInit decodeNegTokenInit(final ASN1Sequence tokenSeq) {
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

    private static ASN1TaggedObject encodeNegTokenInit(final NegTokenInit initToken, final ByteBufAllocator alloc) {
        final var objects = new ArrayList<ASN1Encodable>();
        if (initToken.mechTypes() != null) {
            final var mechTypeOids = initToken.mechTypes()
                .stream().map(mt -> new ASN1ObjectIdentifier(mt.oid()))
                .toArray(ASN1Encodable[]::new);
            objects.add(new DERTaggedObject(0, new DERSequence(mechTypeOids)));
        }
        if (initToken.mechToken() != null) {
            objects.add(new DERTaggedObject(2, encodeMechToken(initToken.mechToken(), alloc)));
        }
        if (initToken.mechListMIC() != null) {
            objects.add(new DERTaggedObject(3, encodeMechListMIC(initToken.mechListMIC())));
        }
        return new DERTaggedObject(0, new DERSequence(objects.toArray(ASN1Encodable[]::new)));
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

    private static ASN1TaggedObject encodeNegTokenResp(final NegTokenResp negToken, final ByteBufAllocator alloc) {
        final var objects = new ArrayList<ASN1Encodable>();
        if (negToken.state() != null) {
            objects.add(new DERTaggedObject(0, new ASN1Enumerated(negToken.state().code())));
        }
        if (negToken.supportedMech() != null) {
            objects.add(new DERTaggedObject(1, new ASN1ObjectIdentifier(negToken.supportedMech().oid())));
        }
        if (negToken.mechToken() != null) {
            objects.add(new DERTaggedObject(2, encodeMechToken(negToken.mechToken(), alloc)));
        }
        if (negToken.mechListMIC() != null) {
            objects.add(new DERTaggedObject(3, encodeMechListMIC(negToken.mechListMIC())));
        }
        return new DERTaggedObject(true, 1, new DERSequence(objects.toArray(ASN1Encodable[]::new)));
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

    private static MechToken decodeMechToken(final MechToken encoded, final MechType mechType) {
        if (encoded instanceof MechToken.EncodedMechToken emt) {
            final var decoded = switch (mechType) {
                case NEGOEX -> NegoexCodecUtils.decodeNegoexMessage(Unpooled.wrappedBuffer(emt.bytes()));
                case NTLMSSP -> NtlmCodecUtils.decodeNtlmMessage(Unpooled.wrappedBuffer(emt.bytes()));
                default -> null;
            };
            if (decoded != null) {
                if (decoded instanceof ContainsSelfEncoded selfEncoded) {
                    selfEncoded.setEncoded(emt.bytes());
                }
                return decoded;
            }
        }
        return encoded;
    }

    private static ASN1OctetString encodeMechToken(final MechToken mechToken, final ByteBufAllocator alloc) {
        final var byteBuf = Unpooled.wrappedBuffer(new byte[1024]);
        byteBuf.writerIndex(0);

        switch (mechToken) {
            case NtlmMessage msg -> NtlmCodecUtils.encodeNtlmMessage(byteBuf, msg);
            case NegoexMessage msg -> NegoexCodecUtils.encodeNegoexMessage(byteBuf, msg);
            default -> {
            }
        }

        return new DEROctetString(ByteBufUtil.getBytes(byteBuf));
    }

    private static MechListMIC decodeMechListMIC(final MechListMIC encoded, final MechType mechType) {
        if (encoded instanceof MechListMIC.EncodedMechListMIC eml) {
            // todo decode depending on mechType
        }
        return encoded;
    }

    private static ASN1OctetString encodeMechListMIC(final MechListMIC mechListMIC) {
        if (mechListMIC instanceof MechListMIC.EncodedMechListMIC eml) {
            return new DEROctetString(eml.bytes());
        }
        if (mechListMIC instanceof NtlmMessageSignature nms) {
            final var bytes = new byte[16];
            final var byteBuf = Unpooled.wrappedBuffer(bytes);
            byteBuf.writerIndex(0);
            NtlmCodecUtils.encodeNtlmMessageSignature(byteBuf, nms);
            return new DEROctetString(bytes);
        }
        return new DEROctetString(new byte[0]);
    }

}
