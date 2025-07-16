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
package local.mylan.service.spi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import local.mylan.common.utils.ConfUtils;
import local.mylan.service.api.EncryptionService;
import local.mylan.service.api.exceptions.EncryptionException;

public final class DefaultEncryptionService implements EncryptionService {

    private static final String CIPHER_ALHORYTHM = "AES/GCM/NoPadding";
    private static final String KEY_ALHORYTHM = "AES";
    private static final String KEY_FACTORY_ALGORYTHM = "PBKDF2WithHmacSHA256";
    private static final String IV_FILENAME = "secret.iv";
    private static final int IV_LENGTH = 12;
    private static final int IV_SPEC_LENGTH = 128;
    private static final String SALT_FILENAME = "secret.salt";
    private static final int SALT_LENGTH = 32;

    private final int keyIterations;
    private final int keyLength;
    private final byte[] salt;
    private final AlgorithmParameterSpec iv;
    private final SecretKeyFactory keyFactory;
    private final SecretKeySpec confSecret;

    public DefaultEncryptionService(final Path confPath, final Path workPath) {
        final var conf = ConfUtils.loadConfiguration(DefaultEncryptionServiceConf.class, confPath);
        keyIterations = conf.keyIterations();
        keyLength = conf.keyLength();
        try {
            salt = readOrGenBytes(workPath.resolve(SALT_FILENAME), SALT_LENGTH);
            iv = new GCMParameterSpec(IV_SPEC_LENGTH, readOrGenBytes(workPath.resolve(IV_FILENAME), IV_LENGTH));
            keyFactory = SecretKeyFactory.getInstance(KEY_FACTORY_ALGORYTHM);
            confSecret = toSecret(conf.password());
        } catch (GeneralSecurityException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static byte[] readOrGenBytes(final Path path, int length) throws IOException {
        if (Files.isRegularFile(path)) {
            return Files.readAllBytes(path);
        }
        final var bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        Files.write(path, bytes);
        return bytes;
    }

    private SecretKeySpec toSecret(final String password) {
        try {
            final var spec = new PBEKeySpec(password.toCharArray(), salt, keyIterations, keyLength);
            return new SecretKeySpec(keyFactory.generateSecret(spec).getEncoded(), KEY_ALHORYTHM);
        } catch (InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String encrypt(final String input) {
        return encrypt(input, confSecret);
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
        return decrypt(input, confSecret);
    }

    @Override
    public String decrypt(final String input, final String password) {
        return decrypt(input, confSecret);
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
            final var spec = new PBEKeySpec(input.toCharArray(), salt, keyIterations, keyLength);
            final var bytes = keyFactory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }
}
