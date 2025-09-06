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

import io.netty.buffer.Unpooled;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import local.transport.netty.smb.protocol.spnego.NegTokenResp;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.junit.jupiter.api.Test;

public class SpnegoCodecUtilsTest {

    @Test
    void gss() throws Exception {

        final var initBytes = Base64.getDecoder().decode(
            "YIIBPAYGKwYBBQUCoIIBMDCCASygGjAYBgorBgEEAYI3AgIeBgorBgEEAYI3AgIKooIBDASCAQhORUdPRVhUUwEAAAAAAAAAYAAAAHAAAABmoqbU3wIqY4uvcviYiI+WV9ijr+VZaWWhRiFb8nXvN2cVNGDr7lL+j3QDrBLxTBEAAAAAAAAAAGAAAAABAAAAAAAAAAAAAABcM1MN6vkNTbLsSuN4bsMITkVHT0VYVFMDAAAAAQAAAEAAAACYAAAAZqKm1N8CKmOLr3L4mIiPllwzUw3q+Q1NsuxK43huwwhAAAAAWAAAADBWoFQwUjAngCUwIzEhMB8GA1UEAxMYVG9rZW4gU2lnbmluZyBQdWJsaWMgS2V5MCeAJTAjMSEwHwYDVQQDExhUb2tlbiBTaWduaW5nIFB1YmxpYyBLZXk="
//            "YEgGBisGAQUFAqA+MDygDjAMBgorBgEEAYI3AgIKoioEKE5UTE1TU1AAAQAAABWCCGIAAAAAKAAAAAAAAAAoAAAABgEAAAAAAA8="
        );
        final var respBytes = Base64.getDecoder().decode(
            "oYHOMIHLoAMKAQGhDAYKKwYBBAGCNwICCqKBtQSBsk5UTE1TU1AAAgAAABIAEgA4AAAAFYKKYhEj+2kAdCOsAAAAAAAAAABoAGgASgAAAAoAYUoAAAAPUgBPAE8ATABJAEMALQBaADkAAgASAFIATwBPAEwASQBDAC0AWgA5AAEAEgBSAE8ATwBMAEkAQwAtAFoAOQAEABIAUgBvAG8AbABpAGMALQBaADkAAwASAFIAbwBvAGwAaQBjAC0AWgA5AAcACACg/7A0qQ3cAQAAAAA="
        );

        final var initToken = SpnegoCodecUtils.decodeNegToken(Unpooled.wrappedBuffer(initBytes));
        final var respToken = SpnegoCodecUtils.decodeNegToken(Unpooled.wrappedBuffer(respBytes));

        final var list = new ArrayList<ASN1Primitive>();
        try (var in = new ASN1InputStream(new ByteArrayInputStream(initBytes))) {
            ASN1Primitive obj;
            while ((obj = in.readObject()) != null) {
                list.add(obj);
            }
        }
    }

    @Test
    void ntlm(){

        final var negBytes = Base64.getDecoder().decode(
            "YEgGBisGAQUFAqA+MDygDjAMBgorBgEEAYI3AgIKoioEKE5UTE1TU1AAAQAAABWCCGIAAAAAKAAAAAAAAAAoAAAABgEAAAAAAA8="
        );
        final var negToken = SpnegoCodecUtils.decodeNegToken(Unpooled.wrappedBuffer(negBytes));
        final var chlBytes = Base64.getDecoder().decode(
            "oYHOMIHLoAMKAQGhDAYKKwYBBAGCNwICCqKBtQSBsk5UTE1TU1AAAgAAABIAEgA4AAAAFYKKYhEj+2kAdCOsAAAAAAAAAABoAGgASgAAAAoAYUoAAAAPUgBPAE8ATABJAEMALQBaADkAAgASAFIATwBPAEwASQBDAC0AWgA5AAEAEgBSAE8ATwBMAEkAQwAtAFoAOQAEABIAUgBvAG8AbABpAGMALQBaADkAAwASAFIAbwBvAGwAaQBjAC0AWgA5AAcACACg/7A0qQ3cAQAAAAA="
        );
        final var chlToken = SpnegoCodecUtils.decodeNegToken(Unpooled.wrappedBuffer(chlBytes));


        final var authBytes = Base64.getDecoder().decode(
            "oYIBrjCCAaqiggGmBIIBok5UTE1TU1AAAwAAABgAGABYAAAADgEOAXAAAAAAAAAAfgEAABQAFAB+AQAAAAAAAJIBAAAQABAAkgEAABWCiGIGAQAAAAAAD1yvIkMadc/BJYGcu56iwtAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAGkVGR4J3vpsrTlXMnDbsJAQEAAAAAAACg/7A0qQ3cASeQaaPJjBhOAAAAAAIAEgBSAE8ATwBMAEkAQwAtAFoAOQABABIAUgBPAE8ATABJAEMALQBaADkABAASAFIAbwBvAGwAaQBjAC0AWgA5AAMAEgBSAG8AbwBsAGkAYwAtAFoAOQAGAAQAAgAAAAcACACg/7A0qQ3cAQkAIgBjAGkAZgBzAC8AMQA5ADIALgAxADYAOAAuADEALgA2ADkACgAQAAAAAAAAAAAAAAAAAAAAAAAIADAAMAAAAAAAAAAAAAAAAAAAALEWbpn752SUazU1BX3inwfgE9Y1Zog+0rVvR/GlRLM+AAAAAAAAAABKAEMASQBGAFMARwBVAEUAUwBUACA9dK9sC/abiQgSD7m5xLM="
            );
        final var authToken = SpnegoCodecUtils.decodeNegToken(Unpooled.wrappedBuffer(authBytes));
        final var msg = ((NegTokenResp)authToken).mechToken();

    }
}
