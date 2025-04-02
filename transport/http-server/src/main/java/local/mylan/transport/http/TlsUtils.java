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
package local.mylan.transport.http;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.net.ssl.SSLException;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class TlsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TlsUtils.class);

    private TlsUtils() {
        // utility class
    }

    static SslContext buildSslContext(final Path confDir, final HttpServerConfig config) {
        if (!config.tlsEnabled()) {
            return null;
        }
        if (!Files.isDirectory(confDir)) {
            throw new IllegalArgumentException("%s is not a directory".formatted(confDir));
        }
        final var certPath = confDir.resolve(config.tlsCertPath());
        final var keyPath = confDir.resolve(config.tlsPrivateKeyPath());
        final var loaded = loadCertData(certPath, keyPath);
        if (loaded != null) {
            return buildSslContext(loaded);
        }
        final var generated = generateCertData();
        if (config.tlsCertPersistGenerated()) {
            persistCertData(generated, certPath, keyPath);
        }
        return buildSslContext(generated);
    }

    private static SslContext buildSslContext(final CertData data) {
        try {
            return SslContextBuilder.forServer(data.privateKey(), data.certificate()).build();
        } catch (SSLException e) {
            LOG.warn("Exception building SSL context", e);
            return null;
        }
    }

    private static CertData generateCertData() {
        // GEN with openssl

        final var random = new SecureRandom();
        try {
            // RSA
            final var keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(new RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4), random);
            final var keyPair = keyPairGenerator.generateKeyPair();
            final var certificate = generateCertificate(keyPair, "SHA256withRSA");
            /*
                // EC
                final var keyPairGenerator = KeyPairGenerator.getInstance("EC");
                keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"), random);
                final var keyPair = keyPairGenerator.generateKeyPair();
                final var certificate = generateCertificate(keyPair, "SHA256withECDSA");
             */
            return new CertData(certificate, keyPair.getPrivate());

        } catch (GeneralSecurityException | OperatorException e) {
            throw new IllegalStateException("Exception on generating Certificate/Private key for TLS", e);
        }
    }

    private static X509Certificate generateCertificate(final KeyPair keyPair, final String hashAlgorithm)
        throws OperatorException, CertificateException {
        final var now = Instant.now();
        final var contentSigner = new JcaContentSignerBuilder(hashAlgorithm).build(keyPair.getPrivate());
        final var x500Name = new X500Name("CN=mylan.local");
        final var certificateBuilder = new JcaX509v3CertificateBuilder(x500Name,
            BigInteger.valueOf(now.toEpochMilli()),
            Date.from(now),
            Date.from(now.plus(Duration.ofDays(3650))), // 10 years
            x500Name,
            keyPair.getPublic());
        return new JcaX509CertificateConverter()
            .setProvider(new BouncyCastleProvider()).getCertificate(certificateBuilder.build(contentSigner));
    }

    private static CertData loadCertData(final Path certPath, final Path keyPath) {
        if (Files.isRegularFile(certPath) && Files.isRegularFile(keyPath)) {
            try {
                final var cert = readPemCert(certPath);
                final var privateKey = readPemKey(keyPath);
                if (cert != null && privateKey != null) {
                    LOG.info("TLS Key loaded from {}", keyPath);
                    LOG.info("TLS Certificate loaded from {}", certPath);
                    return new CertData(cert, privateKey);
                }
            } catch (IOException | GeneralSecurityException e) {
                LOG.warn("Exception on reading PEM file", e);
            }
        }
        return null;
    }

    private static X509Certificate readPemCert(final Path path) throws IOException, CertificateException {
        final var obj = readPEM(path);
        if (obj instanceof X509CertificateHolder certHolder) {
            return new JcaX509CertificateConverter().getCertificate(certHolder);
        }
        LOG.warn("Unexpected object {} when reading certificate from {}", obj, path);
        return null;
    }

    private static PrivateKey readPemKey(final Path path) throws IOException {
        final var obj = readPEM(path);
        if (obj instanceof PEMKeyPair keyPair) {
            return new JcaPEMKeyConverter().getPrivateKey(keyPair.getPrivateKeyInfo());
        }
        if (obj instanceof PrivateKeyInfo keyInfo) {
            return new JcaPEMKeyConverter().getPrivateKey(keyInfo);
        }
        LOG.warn("Unexpected object {} when reading private key from {}", obj, path);
        return null;
    }

    private static void persistCertData(final CertData data, final Path certPath, final Path keyPath) {
        try {
            writePEM(certPath, data.certificate());
            writePEM(keyPath, data.privateKey());
            LOG.info("Generated TLS private key persisted to {}", keyPath);
            LOG.info("Generated TLS certificate persisted to {}", certPath);
        } catch (IOException e) {
            LOG.warn("Exception on persisting generated TLS Certificate/Key", e);
        }
    }

    private static Object readPEM(final Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path); var parser = new PEMParser(reader)) {
            return parser.readObject();
        }
    }

    private static void writePEM(final Path path, final Object obj) throws IOException {
        Files.createDirectories(path.getParent());
        try (var writer = Files.newBufferedWriter(path); var pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(obj);
            pemWriter.flush();
        }
    }

    private record CertData(X509Certificate certificate, PrivateKey privateKey) {
    }
}
