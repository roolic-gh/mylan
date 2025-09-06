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
package local.transport.netty.smb.protocol.spnego;

import static java.nio.charset.StandardCharsets.US_ASCII;

import java.util.Arrays;

public enum MechType {
    NEGOEX("1.3.6.1.4.1.311.2.2.30", "NEGOEXTS".getBytes(US_ASCII)),
    NTLMSSP("1.3.6.1.4.1.311.2.2.10", "NTLMSSP\u0000".getBytes(US_ASCII)),
    OTHER("", new byte[0]);

    final String oid;
    final byte[] signature;

    MechType(final String oid, byte[] signature) {
        this.oid = oid;
        this.signature = signature;
    }

    public String oid() {
        return oid;
    }

    public byte[] signature() {
        return signature;
    }

    public static MechType fromOid(final String oid) {
        for (var mt : values()) {
            if (mt.oid.equals(oid)) {
                return mt;
            }
        }
        return OTHER;
    }

    public static MechType fromSignature(final byte[] signature) {
        for (var mt : values()) {
            if (Arrays.equals(mt.signature, signature)) {
                return mt;
            }
        }
        return OTHER;
    }
}
