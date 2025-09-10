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
import java.net.InetAddress;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import local.transport.netty.smb.handler.codec.CodecUtils;
import local.transport.netty.smb.protocol.SmbDialect;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class SmbTest {

    @Test
    void test() {

        // smb2 negotiate req
        final var reqBytes = Base64.getDecoder().decode(
            "/lNNQkAAAAAAAAAAAAAAAgAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACQAAgADAAAAAQAAALEWbpn752SUazU1BX3inwcAAAAAAAAAAAICEAI="
        );
        // smb2 negotiate resp
        final var respBytes = Base64.getDecoder().decode(
            "/lNNQkAAAAAAAAAAAAABAAEAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEEAAQAQAgAApmeJool70k6+7EToVSHhgQcAAAAAAIAAAACAAAAAgAD5WpE0qQ3cAQAAAAAAAAAAgABAAQAAAABgggE8BgYrBgEFBQKgggEwMIIBLKAaMBgGCisGAQQBgjcCAh4GCisGAQQBgjcCAgqiggEMBIIBCE5FR09FWFRTAQAAAAAAAABgAAAAcAAAAGaiptTfAipji69y+JiIj5ZX2KOv5VlpZaFGIVvyde83ZxU0YOvuUv6PdAOsEvFMEQAAAAAAAAAAYAAAAAEAAAAAAAAAAAAAAFwzUw3q+Q1NsuxK43huwwhORUdPRVhUUwMAAAABAAAAQAAAAJgAAABmoqbU3wIqY4uvcviYiI+WXDNTDer5DU2y7ErjeG7DCEAAAABYAAAAMFagVDBSMCeAJTAjMSEwHwYDVQQDExhUb2tlbiBTaWduaW5nIFB1YmxpYyBLZXkwJ4AlMCMxITAfBgNVBAMTGFRva2VuIFNpZ25pbmcgUHVibGljIEtleQ=="
        );

        final var request = CodecUtils.decodeRequest(Unpooled.wrappedBuffer(reqBytes), SmbDialect.Unknown);
        final var resp = CodecUtils.decodeResponse(Unpooled.wrappedBuffer(respBytes), SmbDialect.Unknown);
    }

    @Test
    @Disabled
    void client() throws Exception {

        final var client = new SmbClient();
        final var conn = client.connect(InetAddress.getByName("192.168.1.69")).get(5, TimeUnit.SECONDS);
        conn.close();
    }

}
