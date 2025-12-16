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
package local.transport.netty.smb;

import io.netty.buffer.Unpooled;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.zip.CRC32;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import local.transport.netty.smb.protocol.SmbException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.KDFCounterBytesGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KDFCounterParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class SecurityUtils {

    private static final Provider BC = new BouncyCastleProvider();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int R = 32;
    private static final int L_ENC_BYTES = 4;

    private SecurityUtils() {
        // utility class
    }

    public static byte[] nonce(final int length) {
        final var result = new byte[length];
        RANDOM.nextBytes(result);
        return result;
    }

    public static byte[] crc32(final byte[]... items) {
        final var crc = new CRC32();
        for (var item : items) {
            crc.update(item);
        }
        final var result = new byte[8];
        final var byteBuf = Unpooled.wrappedBuffer(result);
        byteBuf.setLong(0, crc.getValue());
        return Arrays.copyOfRange(result, 4, 8);
    }

    public static byte[] md4(final byte[]... items) {
        return digest("MD4", items);
    }

    public static byte[] md5(final byte[]... items) {
        return digest("MD5", items);
    }

    public static byte[] hmacMd5(final byte[] key, final byte[]... items) {
        return mac("HMac-MD5", key, items);
    }

    public static byte[] rc4(final byte[] key, final byte[]... items) {
        return cipher("RC4", key, items);
    }

    public static byte[] des(final byte[] key, final byte[]... items) {
        return cipher("DES/ECB/NoPadding", "DES", desKey64(key), items);
    }

    private static byte[] digest(final String algoritm, final byte[]... items) {
        final var digest = digestInstance(algoritm);
        for (var item : items) {
            digest.update(item);
        }
        return digest.digest();
    }

    private static byte[] mac(final String algoritm, final byte[] key, final byte[]... items) {
        final var mac = macInstance(algoritm, key);
        for (var item : items) {
            mac.update(item);
        }
        return mac.doFinal();
    }

    private static byte[] cipher(final String algoritm, final byte[] key, final byte[]... items) {
        return cipher(algoritm, algoritm, key, items);
    }

    private static byte[] cipher(final String algoritm, final String keyAlhorithm,
        final byte[] key, final byte[]... items) {

        var length = 0;
        for (var item : items) {
            length += item.length;
        }
        final var cipher = cipherInstance(algoritm, keyAlhorithm, Cipher.ENCRYPT_MODE, key);
        try {
            final byte[] result = new byte[length];
            var offset = 0;
            for (var item : items) {
                cipher.update(item, 0, item.length, result, offset);
                offset += item.length;
            }
            return result;
        } catch (ShortBufferException e) {
            throw new SmbException("Encryption error", e);
        }

    }

    private static MessageDigest digestInstance(final String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm, BC);
        } catch (NoSuchAlgorithmException e) {
            throw new SmbException("Invalid digest algorithm", e);
        }
    }

    public static Mac macInstance(final String algorithm, final byte[] key) {
        return macInstance(algorithm, algorithm, key);
    }

    public static Mac macInstance(final String algorithm, final String keyAlgotithm, final byte[] key) {
        try {
            final var mac = Mac.getInstance(algorithm, BC);
            mac.init(new SecretKeySpec(key, keyAlgotithm));
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SmbException("Invalid mac algorithm", e);
        }
    }

    private static Cipher cipherInstance(final String algorithm, final String keyAlgorithm,
        final int mode, final byte[] key) {

        try {
            final var cipher = Cipher.getInstance(algorithm, BC);
            cipher.init(mode, new SecretKeySpec(key, keyAlgorithm));
            return cipher;
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
            throw new SmbException("Invalid cipher algorithm", e);
        }
    }

    private static byte[] desKey64(final byte[] key) {
        return switch (key.length) {
            case 8 -> key;
            case 7 -> {
                // create 64-bit key from 56 bit key
                // least significant bit can have any value
                final var key64 = new byte[8];
                key64[0] = (byte) (key[0] & 0xFE); // << 0
                key64[1] = (byte) (key[0] << 7 | (key[1] & 0xFF) >>> 1);
                key64[2] = (byte) (key[1] << 6 | (key[2] & 0xFF) >>> 2);
                key64[3] = (byte) (key[2] << 5 | (key[3] & 0xFF) >>> 3);
                key64[4] = (byte) (key[3] << 4 | (key[4] & 0xFF) >>> 4);
                key64[5] = (byte) (key[4] << 3 | (key[5] & 0xFF) >>> 5);
                key64[6] = (byte) (key[5] << 2 | (key[6] & 0xFF) >>> 6);
                key64[7] = (byte) (key[6] << 1);
                // set parity in time independent of the values within key64
                for (int i = 0; i < key64.length; i++) {
                    // if even # bits, make uneven, take last bit of count so XOR with 1
                    // for uneven # bits, make even, take last bit of count so XOR with 0
                    key64[i] ^= (byte) (Integer.bitCount(key64[i] ^ 1) & 1);
                }
                yield key64;
            }
            default -> throw new IllegalArgumentException("DES key has invalid length");
        };
    }

    /**
     * Key Deriviation Function (KDF) in Counter Mode.
     * Addresses MS-SMB2 (#3.1.4.2 Generating Cryptographic Keys), referencing NIST SP 800-108.
     */
    public static byte[] kdfcm(final byte[] key, final byte[] label, final byte[] context, final int l) {
        // R = 32 bits (4 bytes)
        // fixed input is "Label || 0x00 || Context || [L]_2",
        // where [L]_2 is binary representation of L (R bytes encoded hi-ended)

        final var fixedInput = new byte[label.length + 1 + context.length + 4];
        final var fixedInputBuf = Unpooled.wrappedBuffer(fixedInput);
        System.arraycopy(label, 0, fixedInput, 0, label.length);
        System.arraycopy(context, 0, fixedInput, label.length + 1, context.length);
        fixedInput[fixedInput.length - 1] = (byte) l;

        final var kdfGen = new KDFCounterBytesGenerator(new HMac(new SHA256Digest()));
        kdfGen.init(new KDFCounterParameters(key, fixedInput, 32));

        final var output = new byte[l / 8];
        kdfGen.generateBytes(output, 0, output.length);
        return output;
    }
}
