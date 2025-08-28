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
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.spnego.MechType;
import local.transport.netty.smb.protocol.spnego.NegState;
import local.transport.netty.smb.protocol.spnego.NegToken;
import local.transport.netty.smb.protocol.spnego.NegTokenInit;
import local.transport.netty.smb.protocol.spnego.NegTokenResp;
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

public final class SpnegoCodecUtils {
    private static final ASN1ObjectIdentifier SPNEGO_OID = new ASN1ObjectIdentifier("1.3.6.1.5.5.2");

    private SpnegoCodecUtils() {
        // utility class
    }

    public static NegToken decodeNegToken(final ByteBuf byteBuf) {
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

    public static void encodeNegToken(final ByteBuf byteBuf, final NegToken negToken) {
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
                    // todo decode mechToken
                    case 2 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechToken(octets.getOctets()));

                    // mechListMIC [3] OCTET STRING  OPTIONAL,
                    // todo decode mechListMIC
                    case 3 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechListMIC(octets.getOctets()));
                    default -> {
                    }
                }
            }));
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
                    // todo decode mechToken
                    case 2 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechToken(octets.getOctets()));
                    // mechListMIC [3] OCTET STRING  OPTIONAL
                    // todo decode mechListMIC
                    case 3 -> castOpt(tagged, ASN1OctetString.class)
                        .ifPresent(octets -> negToken.setMechListMIC(octets.getOctets()));
                    default -> {
                    }
                }
            }));
        return negToken;
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

// NTLM

}
