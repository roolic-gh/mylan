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
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Base64;
import local.transport.netty.smb.handler.codec.CodecUtils;
import local.transport.netty.smb.handler.codec.SpnegoCodecUtils;
import local.transport.netty.smb.protocol.SmbDialect;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.junit.jupiter.api.Test;

class SmbTest {

    @Test
    void test() {

        // smb1 negotiate req
//        final var src = "/1NNQnIAAAAAGAPIAAAAAAAAAAAAAAAA//9BwAAAAAAAIgACTlQgTE0gMC4xMgACU01CIDIuPz8/AAJTTUIgMi4wMDIA";

        // smb2 negotiate req
        final var src = "/lNNQkAAAAAAAAAAAAAAAgAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACQAAgADAAAAAQAAALEWbpn752SUazU1BX3inwcAAAAAAAAAAAICEAI=";
        // smb2 negotiate resp
//        final var src = "/lNNQkAAAAAAAAAAAAABAAEAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEEAAQAQAgAApmeJool70k6+7EToVSHhgQcAAAAAAIAAAACAAAAAgAD5WpE0qQ3cAQAAAAAAAAAAgABAAQAAAABgggE8BgYrBgEFBQKgggEwMIIBLKAaMBgGCisGAQQBgjcCAh4GCisGAQQBgjcCAgqiggEMBIIBCE5FR09FWFRTAQAAAAAAAABgAAAAcAAAAGaiptTfAipji69y+JiIj5ZX2KOv5VlpZaFGIVvyde83ZxU0YOvuUv6PdAOsEvFMEQAAAAAAAAAAYAAAAAEAAAAAAAAAAAAAAFwzUw3q+Q1NsuxK43huwwhORUdPRVhUUwMAAAABAAAAQAAAAJgAAABmoqbU3wIqY4uvcviYiI+WXDNTDer5DU2y7ErjeG7DCEAAAABYAAAAMFagVDBSMCeAJTAjMSEwHwYDVQQDExhUb2tlbiBTaWduaW5nIFB1YmxpYyBLZXkwJ4AlMCMxITAfBgNVBAMTGFRva2VuIFNpZ25pbmcgUHVibGljIEtleQ==";

        final var bytes = Base64.getDecoder().decode(src);
        final var request = CodecUtils.decodeRequest(Unpooled.wrappedBuffer(bytes), SmbDialect.Unknown);
        //final var resp = CodecUtils.decodeResponse(Unpooled.wrappedBuffer(bytes), SmbDialect.Unknown);

        System.out.println();
    }

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
        list.forEach(obj -> dump(obj, ""));

    }

    private static void dump(final ASN1Primitive obj, final String indent) {
        System.out.println(indent + "* " + obj.getClass());
        switch (obj) {
            case ASN1TaggedObject tagged -> {
                final var baseobj = tagged.getBaseObject();
                System.out.printf(indent + "= %d %d %s %n", tagged.getTagNo(), tagged.getTagClass(),
                    baseobj.getClass());
                if (baseobj instanceof ASN1Sequence seq) {
                    seq.forEach(o -> {
                        if (o instanceof ASN1Primitive prim) {
                            dump(prim, indent + "    ");
                        } else {
                            System.out.println(indent + "    * " + o.getClass());
                        }
                    });
                }
            }
            case ASN1ObjectIdentifier oid -> System.out.println(indent + "= " + oid.getId());
            default -> {
            }
        }

    }
}
