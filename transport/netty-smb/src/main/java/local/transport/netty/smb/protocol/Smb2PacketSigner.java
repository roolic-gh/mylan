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
package local.transport.netty.smb.protocol;

import static java.util.Objects.requireNonNull;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.util.Arrays;
import javax.crypto.Mac;
import local.transport.netty.smb.SecurityUtils;
import local.transport.netty.smb.Utils;
import local.transport.netty.smb.protocol.details.SessionDetails;
import local.transport.netty.smb.protocol.smb2.Smb2Flags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Addresses MS-SMB2 (#3.1.4.1 Signing An Outgoing Message).
 */
public final class Smb2PacketSigner {
    private static final Logger LOG = LoggerFactory.getLogger(Smb2PacketSigner.class);

    private static final int FLAGS_OFFSET = 16;
    private static final int SIGNATURE_OFFSET = 48;
    private static final int SIGNATURE_LENGTH = 16;
    private static final byte[] EMPTY_SIGNATURE = new byte[SIGNATURE_LENGTH];

    private final Mac mac;

    public Smb2PacketSigner(final Smb2Dialect dialect, final SessionDetails sessDetails) {
        requireNonNull(dialect);
        requireNonNull(sessDetails);

        if (dialect.equalsOrHigher(Smb2Dialect.SMB3_1_1)) {
            throw new IllegalStateException("Signing for SMB 3.1.1 is not implemented");
            //TODO implement
        } else if (dialect.equalsOrHigher(Smb2Dialect.SMB3_0)) {
            // SMB 3.0, 3.0.2
            mac = SecurityUtils.macInstance("AESCMAC", "AES",
                requireNonNull(sessDetails.signingKey(), "Session.SigningKey is undefined"));
        } else {
            // SMB 2.0.2 & SMB 2.1
            mac = SecurityUtils.macInstance("HMac-SHA256",
                requireNonNull(sessDetails.sessionKey(), "Session.SessionKey is undefined"));
        }
    }

    public void signOutbound(final ByteBuf byteBuf) {
        // set signed flag
        final var flags = new Flags<Smb2Flags>(byteBuf.getIntLE(FLAGS_OFFSET));
        flags.set(Smb2Flags.SMB2_FLAGS_SIGNED, true);
        byteBuf.setIntLE(FLAGS_OFFSET, flags.asIntValue());
        // set signature
        final var signature = getSignature(byteBuf, mac);
        byteBuf.setBytes(SIGNATURE_OFFSET, signature);
    }

    public boolean verifyInboundSignature(final ByteBuf byteBuf) {
        final var flags = new Flags<Smb2Flags>(byteBuf.getIntLE(FLAGS_OFFSET));
        if (!flags.get(Smb2Flags.SMB2_FLAGS_SIGNED)) {
            LOG.warn("Inbound message is not signed.");
            return false;
        }
        final var signature = Utils.getByteArray(byteBuf, SIGNATURE_OFFSET, SIGNATURE_LENGTH);
        final var expected = getSignature(byteBuf, mac);
        if (!Arrays.equals(signature, expected)) {
            LOG.warn("Invalid inbound signature");
            return false;
        }
        return true;
    }

    private static byte[] getSignature(final ByteBuf byteBuf, final Mac mac) {
        final var bytes = ByteBufUtil.getBytes(byteBuf);
        LOG.info("packet.length {}", bytes.length);
        // ensure signature placeholder is empty
        System.arraycopy(EMPTY_SIGNATURE, 0, bytes, SIGNATURE_OFFSET, SIGNATURE_LENGTH);
        synchronized (mac) {
            mac.reset();
            mac.update(bytes);
            final var signature = mac.doFinal();
            // in case of HMAC-SHA256 return first half of 32 byte array
            return signature.length == SIGNATURE_LENGTH ? signature : Arrays.copyOf(signature, SIGNATURE_LENGTH);
        }
    }
}
