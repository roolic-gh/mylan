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
package local.mylan.service.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import local.mylan.service.api.EncryptionService;
import local.mylan.service.api.exceptions.EncryptionException;
import local.mylan.service.spi.model.EncryptedDeviceAccountWithCredentials;

public class TestEncryptionService implements EncryptionService {

    private static final String CIPHER_ALHORYTHM = "AES/GCM/NoPadding";
    private static final String KEY_ALHORYTHM = "AES";
    private static final String KEY_FACTORY_ALGORYTHM = "PBKDF2WithHmacSHA256";
    private static final String DIGEST_ALGORYTM = "SHA-256";

    private final byte[] salt;
    private final AlgorithmParameterSpec iv;
    private final SecretKeyFactory keyFactory;
    private final SecretKeySpec defaultSecret;

    public TestEncryptionService() {
        try {
            salt = randomBytes(32);
            iv = new GCMParameterSpec(128, randomBytes(12));
            keyFactory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORYTHM);
            defaultSecret = toSecret("test");
        } catch (GeneralSecurityException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static byte[] randomBytes(int length) throws IOException {
        final var bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    private SecretKeySpec toSecret(final String password) {
        try {
            final var spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
            return new SecretKeySpec(keyFactory.generateSecret(spec).getEncoded(), KEY_ALHORYTHM);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String encrypt(final String input) {
        return encrypt(input, defaultSecret);
    }

    @Override
    public String encrypt(final String input, final String password) {
        return encrypt(input, toSecret(password));
    }

    private String encrypt(final String input, final SecretKeySpec secret) {
        try {
            final var cipher = Cipher.getInstance(CIPHER_ALHORYTHM);
            cipher.init(Cipher.ENCRYPT_MODE, secret, iv);
            final var encrypted = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    @Override
    public String decrypt(final String input) {
        return decrypt(input, defaultSecret);
    }

    @Override
    public String decrypt(final String input, final String password) {
        return decrypt(input, toSecret(password));
    }

    private String decrypt(final String input, final SecretKeySpec secret) {
        try {
            final var cipher = Cipher.getInstance(CIPHER_ALHORYTHM);
            cipher.init(Cipher.DECRYPT_MODE, secret, iv);
            final var decrypted = cipher.doFinal(Base64.getDecoder().decode(input));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    @Override
    public String buildHash(final String input) {
        try {
            final var digest = MessageDigest.getInstance(DIGEST_ALGORYTM );
            final var bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(bytes);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    public EncryptedDeviceAccountWithCredentials.Encryptor credentialsEncryptor() {
        return (input, key) -> encrypt(input, key == null ? defaultSecret : toSecret(key));
    }

    public EncryptedDeviceAccountWithCredentials.Decryptor credentialsDecryptor() {
        return (input, key) -> decrypt(input, key == null ? defaultSecret : toSecret(key));
    }
}
