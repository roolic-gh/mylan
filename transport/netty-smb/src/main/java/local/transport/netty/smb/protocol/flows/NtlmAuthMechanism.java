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
package local.transport.netty.smb.protocol.flows;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_16LE;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.Objects.requireNonNull;
import static local.transport.netty.smb.SecurityUtils.crc32;
import static local.transport.netty.smb.SecurityUtils.des;
import static local.transport.netty.smb.SecurityUtils.hmacMd5;
import static local.transport.netty.smb.SecurityUtils.md4;
import static local.transport.netty.smb.SecurityUtils.md5;
import static local.transport.netty.smb.SecurityUtils.nonce;
import static local.transport.netty.smb.SecurityUtils.rc4;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_128;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_ALWAYS_SIGN;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_ANONIMOUS;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_KEY_EXCHANGE;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_LM_KEY;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_NTLM;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_SEAL;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_SIGN;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_TARGET_INFO;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_UNICODE;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_NEGOTIATE_VERSION;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_REQUEST_NON_NT_SESSION_KEY;
import static local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags.NTLMSSP_REQUEST_TARGET;

import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbException;
import local.transport.netty.smb.protocol.details.NtlmClientDetails;
import local.transport.netty.smb.protocol.details.NtlmMessageSignature;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.details.UserCredentials;
import local.transport.netty.smb.protocol.spnego.MechListMIC;
import local.transport.netty.smb.protocol.spnego.MechType;
import local.transport.netty.smb.protocol.spnego.NegState;
import local.transport.netty.smb.protocol.spnego.NegToken;
import local.transport.netty.smb.protocol.spnego.NegTokenInit;
import local.transport.netty.smb.protocol.spnego.NegTokenResp;
import local.transport.netty.smb.protocol.spnego.ntlm.LmChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.LmV1ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.LmV2ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAuthEncoder;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAuthenticateMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmAvPairs;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmChallengeMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmChannelBindingHash;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateFlags;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmNegotiateMessage;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmV1ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmV2ChallengeResponse;
import local.transport.netty.smb.protocol.spnego.ntlm.NtlmV2ClientChallenge;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;

public class NtlmAuthMechanism implements AuthMechanism {
    private static final MechType MECH_TYPE = MechType.NTLMSSP;
    private static final byte[] MECH_LIST_ENCODED = mechListEncoded(MechType.NTLMSSP);
    private static final byte[] SERVER_SIGN_MAGIC_CONSTANT =
        "session key to server-to-client signing key magic constant\u0000".getBytes(US_ASCII);
    private static final byte[] SERVER_SEAL_MAGIC_CONSTANT =
        "session key to server-to-client sealing key magic constant\u0000".getBytes(US_ASCII);
    private static final byte[] CLIENT_SIGN_MAGIC_CONSTANT =
        "session key to client-to-server signing key magic constant\u0000".getBytes(US_ASCII);
    private static final byte[] CLIENT_SEAL_MAGIC_CONSTANT =
        "session key to client-to-server sealing key magic constant\u0000".getBytes(US_ASCII);
    private static final byte[] LMOWF1_MAGIC = "KGS!@#$%".getBytes(US_ASCII);
    private static final byte[] NIL = new byte[0];

    private final SessionDetails sessDetails;
    private final NtlmClientDetails details;
    private final NtlmAuthEncoder encoder;

    private NtlmNegotiateMessage negotiateMsg;

    public NtlmAuthMechanism(final SessionDetails sessDetails, final NtlmClientDetails details,
        final NtlmAuthEncoder encoder) {

        this.sessDetails = sessDetails;
        this.details = details;
        this.encoder = encoder;
    }

    @Override
    public MechType mechType() {
        return MECH_TYPE;
    }

    @Override
    public NegTokenInit init() {
        // MS-NLMP #3.1.5.1.1 Client Initiates the NEGOTIATE_MESSAGE
        final var msg = new NtlmNegotiateMessage();
        msg.setNegotiateFlags(new Flags<NtlmNegotiateFlags>()
            .set(NTLMSSP_NEGOTIATE_KEY_EXCHANGE, true)
            .set(NTLMSSP_NEGOTIATE_128, details.clientRequire128bitEncryption())
            .set(NTLMSSP_REQUEST_TARGET, true)
            .set(NTLMSSP_NEGOTIATE_NTLM, true)
            .set(NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY, true)
            .set(NTLMSSP_NEGOTIATE_ALWAYS_SIGN, true)
            .set(NTLMSSP_NEGOTIATE_SIGN, true)
            .set(NTLMSSP_NEGOTIATE_UNICODE, true)
            .set(NTLMSSP_NEGOTIATE_ANONIMOUS, sessDetails.anonymous())
        );
        negotiateMsg = msg;
        final var negToken = new NegTokenInit();
        negToken.setMechTypes(List.of(MECH_TYPE));
        negToken.setMechToken(msg);
        return negToken;
    }

    @Override
    public NegTokenResp next(final NegToken response) {
        if (response instanceof NegTokenResp ntr && ntr.state() == NegState.ACCEPT_INCOMPLETE
            && ntr.mechToken() instanceof NtlmChallengeMessage challengeMsg) {

            extractServerName(challengeMsg);
            final var resp = new NegTokenResp();
            resp.setMechToken(handleChallenge(challengeMsg));
            resp.setMechListMIC(mechListMIC());
            return resp;
        }
        return null;
    }

    @Override
    public boolean verify(final NegToken response) {
        if (response instanceof NegTokenResp ntr && ntr.state() == NegState.ACCEPT_COMPLETED) {
            return verifyMechListMIC(ntr.mechListMIC());
        }
        return true;
    }

    private void extractServerName(final NtlmChallengeMessage challengeMsg) {
        final var serverDetails = sessDetails.connection() != null && sessDetails.connection().details() != null
            ? sessDetails.connection().details().server() : null;
        if (serverDetails != null && serverDetails.serverName() == null) {
            serverDetails.setServerName(challengeMsg.targetInfo() == null
                ? challengeMsg.targetName() : challengeMsg.targetInfo().netbiosComputerName());
        }
    }

    private NtlmAuthenticateMessage handleChallenge(final NtlmChallengeMessage challengeMsg) {
        // validate what's being supported by current implementation
        if (!challengeMsg.negotiateFlags().get(NTLMSSP_NEGOTIATE_NTLM)) {
            throw new SmbException("Server does not support NTLM authentication");
        }
        if (sessDetails.anonymous() && !challengeMsg.negotiateFlags().get(NTLMSSP_NEGOTIATE_ANONIMOUS)) {
            throw new SmbException("Server does not support anonymous access");
        }
        if (challengeMsg.targetInfo() == null) {
            throw new SmbException("Server did not provide target information within Challenge message");
        }

        // Below addresses MS-NLMP #3.1.5.1.2 Client Receives a CHALLENGE_MESSAGE from the Server.

        // validate challenge message
        if (details.clientRequire128bitEncryption() && !challengeMsg.negotiateFlags().get(NTLMSSP_NEGOTIATE_128)) {
            throw new SmbException("Server is not supporting 128 bit encription");
        }
        if (details.ntlmV2()
            && (challengeMsg.targetInfo().netbiosComputerName() == null
            || challengeMsg.targetInfo().netbiosComputerName() == null)
            && challengeMsg.targetInfo().flags() != null
            && challengeMsg.targetInfo().flags().asIntValue() != 0) {
            throw new SmbException("Server did not provide target information within Challenge message ");
        }
        details.setNegFlags(challengeMsg.negotiateFlags());

        // create auth message with base data
        final var authMsg = new NtlmAuthenticateMessage();
        authMsg.setNegotiateFlags(new Flags<>(negotiateMsg.negotiateFlags().asIntValue()));
        final var creds = sessDetails.userCredentials();
        if (creds != null) {
            authMsg.setUserName(creds.username());
            authMsg.setDomainName(creds.domain());
        }
        if (details.negFlags().get(NTLMSSP_NEGOTIATE_TARGET_INFO)) {
            authMsg.negotiateFlags().set(NTLMSSP_NEGOTIATE_TARGET_INFO, true);
        }
        if (details.negFlags().get(NTLMSSP_NEGOTIATE_VERSION)) {
            authMsg.setVersion(details.clientVersion());
            authMsg.negotiateFlags().set(NTLMSSP_NEGOTIATE_VERSION, true);
        }

        // set NTLM response
        final var extSecurity = details.negFlags().get(NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY);
        final var sessionBaseKey = computeResponse(challengeMsg, authMsg);
        if (details.ntlmV2() && challengeMsg.targetInfo().timestamp() != null) {
            authMsg.setLmChallengeResponse(new LmChallengeResponse.EncodedLmChallengeResponse(new byte[24]));
        }
        if (sessionBaseKey == null) {
            return authMsg; // anonymous
        }

        final var keyExchangeKey =
            kxKey(sessionBaseKey, authMsg.lmChallengeResponse().bytes(), challengeMsg.serverChallenge());
        final byte[] exportedSessionKey;
        if (details.negFlags().get(NTLMSSP_NEGOTIATE_KEY_EXCHANGE)
            && (details.negFlags().get(NTLMSSP_NEGOTIATE_SIGN) || details.negFlags().get(NTLMSSP_NEGOTIATE_SEAL))) {

            exportedSessionKey = nonce(16);
            authMsg.setEncryptedRandomSessionKey(rc4(keyExchangeKey, exportedSessionKey));
        } else {
            exportedSessionKey = keyExchangeKey;
        }

        // set sequre context
        details.setExportedSessionKey(exportedSessionKey);
        details.setClientSigningKey(signKey(extSecurity, exportedSessionKey, Mode.Client));
        details.setServerSigningKey(signKey(extSecurity, exportedSessionKey, Mode.Server));
        details.setClientSealingKey(sealKey(extSecurity, exportedSessionKey, Mode.Client));
        details.setServerSealingKey(sealKey(extSecurity, exportedSessionKey, Mode.Server));

        authMsg.setMic(hmacMd5(exportedSessionKey, concat(encoder, negotiateMsg, challengeMsg, authMsg)));
        return authMsg;
    }

    // sets LmChallengeResponces and NtChallengeResponse to authMessage, returns sessionBaseKey

    private byte[] computeResponse(final NtlmChallengeMessage challengeMsg, final NtlmAuthenticateMessage authMsg) {
        final var creds = sessDetails.userCredentials();

        // anonymous handling is same for both NTLM v1 and v2
        if (sessDetails.anonymous() || creds == null || creds.password() == null || creds.password().isEmpty()) {
            authMsg.setNtChallengeResponse(null);
            authMsg.setLmChallengeResponse(new LmChallengeResponse.EncodedLmChallengeResponse(new byte[1]));
            return null;
        }

        final var clientChallenge = nonce(8);
        if (details.ntlmV2()) {
            // MS-NLMP (#3.3.2 NTLM v2 Authentication)

            // build client challenge (setting timestamp and values of NTLMv2_CLIENT_CHALLENGE.AvPairs
            // are described in MS-NLMP #3.1.5.1.2 Client Receives a CHALLENGE_MESSAGE from the Server)
            final var serverTimestamp = challengeMsg.targetInfo().timestamp();
            final var clientTimestamp = serverTimestamp == null
                ? Utils.filetimeFromUnixMillis(System.currentTimeMillis()) : serverTimestamp;

            final var cc = new NtlmV2ClientChallenge();
            cc.setClientChallenge(clientChallenge);
            cc.setTimestamp(clientTimestamp);
            cc.setAvPairs(challengeMsg.targetInfo() == null ?
                new NtlmAvPairs() : NtlmAvPairs.copyOf(challengeMsg.targetInfo()));
            if (cc.avPairs().flags() == null) {
                cc.avPairs().setFlags(new Flags<>());
            }
            cc.avPairs().flags().set(NtlmAvFlags.INTEGRITY_IN_MIC, true);
            cc.avPairs().setChannelBinding(new NtlmChannelBindingHash(
                details.channelBindingUnhashed() == null ? new byte[16] : md5(details.channelBindingUnhashed())
            ));
            cc.avPairs().setTargetName(details.suppliedTargetName() == null ? "" : details.suppliedTargetName());
            cc.avPairs().flags().set(NtlmAvFlags.UNTRUSTED_SOURCE_SPN,
                details.suppliedTargetName() != null && details.unverifiedTargetName());

            // build challenge responses

            final var byteBuf = Unpooled.wrappedBuffer(new byte[1024]);
            byteBuf.writerIndex(0);
            encoder.encode(byteBuf, cc);
            final var temp = ByteBufUtil.getBytes(byteBuf);

            final var responseKeyNt = ntOWFv2(creds);
            final var ntProofStr = hmacMd5(responseKeyNt, challengeMsg.serverChallenge(), temp);
            authMsg.setNtChallengeResponse(new NtlmV2ChallengeResponse(ntProofStr, cc));

            final var responseKeyLm = lmOWFv2(creds);
            final var lmResponse = hmacMd5(responseKeyLm, challengeMsg.serverChallenge(), clientChallenge);
            authMsg.setLmChallengeResponse(new LmV2ChallengeResponse(lmResponse, clientChallenge));

            // sessionBaseKey
            return hmacMd5(responseKeyNt, ntProofStr);
        }

        // MS-NLMP (#3.3.1 NTLM v1 Authentication)

        final var responseKeyNt = ntOWFv1(creds);
        final var responseKeyLm = lmOWFv1(creds);
        if (details.negFlags().get(NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY)) {
            authMsg.setNtChallengeResponse(new NtlmV1ChallengeResponse(
                desl(responseKeyNt, copyOf(md5(challengeMsg.serverChallenge(), clientChallenge), 8))
            ));
            authMsg.setLmChallengeResponse(new LmV1ChallengeResponse(concat(clientChallenge, new byte[16])));

        } else {
            final var ntResponse = new NtlmV1ChallengeResponse(desl(responseKeyNt, clientChallenge));
            authMsg.setNtChallengeResponse(ntResponse);
            authMsg.setLmChallengeResponse(new LmV1ChallengeResponse(
                details.noLMResponseNTLMv1()
                    ? ntResponse.response() : desl(responseKeyLm, challengeMsg.serverChallenge())
            ));
        }
        // SessionBaseKey
        return md4(responseKeyNt);
    }

    private static byte[] lmOWFv1(final UserCredentials creds) {
        final var passwFixed = new byte[14];
        try {
            final var passw = nonnullPassw(creds).toUpperCase(Locale.US).getBytes(US_ASCII);
            if (passw.length > 14) {
                throw new SmbException("password is too long (max 14 ascii chars allowed)");
            }
            System.arraycopy(passw, 0, passwFixed, 0, passw.length);
        } catch (Exception e) {
            throw new SmbException("Unsupported password encoding", e);
        }
        return concat(
            des(copyOf(passwFixed, 7), LMOWF1_MAGIC),
            des(copyOfRange(passwFixed, 7, 14), LMOWF1_MAGIC));
    }

    private static byte[] ntOWFv1(final UserCredentials creds) {
        return md4(nonnullPassw(creds).getBytes(UTF_16LE));
    }

    private static byte[] lmOWFv2(final UserCredentials creds) {
        return ntOWFv2(creds);
    }

    private static byte[] ntOWFv2(final UserCredentials creds) {
        return hmacMd5(
            md4(nonnullPassw(creds).getBytes(UTF_16LE)),
            nonnullUser(creds).toUpperCase().getBytes(UTF_16LE),
            nonnullDomain(creds).getBytes(UTF_16LE)
        );
    }

    private static String nonnullUser(final UserCredentials creds) {
        return creds == null || creds.username() == null ? "" : creds.username();
    }

    private static String nonnullPassw(final UserCredentials creds) {
        return creds == null || creds.password() == null ? "" : creds.password();
    }

    private static String nonnullDomain(final UserCredentials creds) {
        return creds == null || creds.domain() == null ? "" : creds.domain();
    }

    private byte[] kxKey(final byte[] sessionBaseKey, final byte[] lmChallengeResponse, final byte[] serverChallenge) {
        // MS-NLMP (#3.4.5.1 KXKEY)
        if (details.ntlmV2()) {
            return sessionBaseKey;
        }
        if (details.negFlags().get(NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY)) {
            return hmacMd5(sessionBaseKey, serverChallenge, copyOf(lmChallengeResponse, 8));
        }
        if (details.negFlags().get(NTLMSSP_NEGOTIATE_LM_KEY)) {
            final var lmowf = lmOWFv1(sessDetails.userCredentials());
            return concat(
                des(copyOf(lmowf, 7), copyOf(lmChallengeResponse, 8)),
                des(new byte[]{lmowf[7], (byte) 0xBD, (byte) 0xBD, (byte) 0xBD, (byte) 0xBD, (byte) 0xBD, (byte) 0xBD},
                    copyOf(lmChallengeResponse, 8))
            );
        }
        if (details.negFlags().get(NTLMSSP_REQUEST_NON_NT_SESSION_KEY)) {
            final var result = new byte[16];
            final var lmowf = lmOWFv1(sessDetails.userCredentials());
            System.arraycopy(lmowf, 0, result, 0, 8);
            return result;
        }
        return sessionBaseKey;
    }

    private static byte[] concat(final byte[]... items) {
        var length = 0;
        for (var item : items) {
            length += item.length;
        }
        final var result = new byte[length];
        var offset = 0;
        for (var item : items) {
            System.arraycopy(item, 0, result, offset, item.length);
            offset += item.length;
        }
        return result;
    }

    private static byte[] concat(final NtlmAuthEncoder encoder, final NtlmMessage... messages) {
        final var byteBuf = Unpooled.wrappedBuffer(new byte[1024]);
        byteBuf.writerIndex(0);
        for (var msg : messages) {
            if (msg instanceof NtlmChallengeMessage cm) {
                byteBuf.writeBytes(cm.encoded());
            } else {
                encoder.encode(byteBuf, msg);
            }
        }
        return ByteBufUtil.getBytes(byteBuf);
    }

    private static byte[] signKey(final boolean extSecurity, final byte[] exportedSessionKey, final Mode mode) {
        // MS-NLMP (#3.4.5.2 SIGNKEY)
        return extSecurity
            ? md5(exportedSessionKey, mode == Mode.Client ? CLIENT_SIGN_MAGIC_CONSTANT : SERVER_SIGN_MAGIC_CONSTANT)
            : NIL;
    }

    private static byte[] sealKey(final boolean extSecurity, final byte[] exportedSessionKey, final Mode mode) {
        // MS-NLMP (#3.4.5.3 SEALNKEY)
        return extSecurity
            ? md5(exportedSessionKey, mode == Mode.Client ? CLIENT_SEAL_MAGIC_CONSTANT : SERVER_SEAL_MAGIC_CONSTANT)
            : NIL;
    }

    private static byte[] desl(final byte[] key, final byte[] data) {
        // MS-NLMP (Appendix A: Cryptographic Operations Reference)
        checkArgument(requireNonNull(key).length == 16, "DESL key size expected to be 16 bytes");
        checkArgument(requireNonNull(data).length == 8, "DESL data size expected to be 8 bytes");
        return concat(
            des(copyOf(key, 7), data),
            des(copyOfRange(key, 7, 14), data),
            des(concat(copyOfRange(key, 14, 16), new byte[5]), data)
        );
    }

    private MechListMIC mechListMIC() {
        return mac(MECH_LIST_ENCODED);
    }

    private static byte[] mechListEncoded(final MechType mechType) {
        // addresses https://www.rfc-editor.org/rfc/rfc4178.html#section-5 (Processing of mechListMIC)

        final var seq = new DERSequence(new ASN1Encodable[]{new ASN1ObjectIdentifier(mechType.oid())});
        final var byteBuf = Unpooled.buffer(64);
        try (var out = new ByteBufOutputStream(byteBuf)) {
            seq.encodeTo(out, ASN1Encoding.DER);
            return ByteBufUtil.getBytes(byteBuf);
        } catch (IOException e) {
            throw new SmbException("Error encoding mechList");
        }
    }

    private NtlmMessageSignature mac(final byte[] data) {
        if (details.clientSigningKey() == null || details.clientSealingKey() == null) {
            // no signature if sign/seal keys arn't defined
            return null;
        }
        if (details.negFlags().get(NTLMSSP_NEGOTIATE_ALWAYS_SIGN) && !details.negFlags().get(NTLMSSP_NEGOTIATE_SIGN)) {
            // MS-NLMP #3.4.4.3 Without NTLMSSP_NEGOTIATE_SIGN
            return new NtlmMessageSignature(new byte[8], 0);
        }
        return mac(details.clientSealingKey(), details.clientSigningKey(), data, details.seqNum());
    }

    private NtlmMessageSignature mac(final byte[] sealKey, final byte[] signKey, final byte[] data, final int seqNum) {
        // MS-NLMP #3.4.4 Message Signature Functions

        if (details.negFlags().get(NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY)) {
            // MS-NLMP #3.4.4.2 With Extended Session Security
            final var seqNumBytes = new byte[4];
            final var seqNumBuf = Unpooled.wrappedBuffer(seqNumBytes);
            seqNumBuf.setIntLE(0, seqNum);
            final var checksum = copyOf(hmacMd5(signKey, seqNumBytes, data), 8);
            return new NtlmMessageSignature(
                details.negFlags().get(NTLMSSP_NEGOTIATE_KEY_EXCHANGE) ? rc4(sealKey, checksum) : checksum,
                seqNum);
        }

        // MS-NLMP #3.4.4.1 Without Extended Session Security
        final var xorBuf = Unpooled.wrappedBuffer(rc4(sealKey, new byte[4]));
        final var xor = xorBuf.getInt(0);
        final var checksum = rc4(sealKey, crc32(data));
        return new NtlmMessageSignature(new byte[4], checksum, seqNum ^ xor);
    }

    private boolean verifyMechListMIC(final MechListMIC mlmic) {
        // validate if there are keys and mic defined, otherwise omit
        if (mlmic instanceof MechListMIC.EncodedMechListMIC enc
            && details.serverSigningKey() != null && details.serverSealingKey() != null) {

            final var encoded = enc.bytes();
            if (encoded.length != 16) {
                throw new SmbException("Unexpected MechListMIC length (%d, expected %d)");
            }
            final var byteBuf = Unpooled.wrappedBuffer(encoded);
            final var version = byteBuf.getIntLE(0);
            if (version != 1) {
                return false;
            }
            final var expected = mac(details.serverSealingKey(), details.serverSigningKey(), MECH_LIST_ENCODED, 0);
            if (expected.randomPad() != null) {
                final var randomPad = Utils.getByteArray(byteBuf, 4, 4);
                final var checksum = Utils.getByteArray(byteBuf, 8, 4);
                return Arrays.equals(expected.randomPad(), randomPad)
                    && Arrays.equals(expected.checksum(), checksum);
            }
            final var checksum = Utils.getByteArray(byteBuf, 4, 8);
            return Arrays.equals(expected.checksum(), checksum);
        }
        return true;
    }

    private enum Mode {Client, Server}
}
