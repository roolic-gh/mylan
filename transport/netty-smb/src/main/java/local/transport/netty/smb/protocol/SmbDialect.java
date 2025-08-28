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

import static local.transport.netty.smb.protocol.ProtocolVersion.CIFS_SMB;
import static local.transport.netty.smb.protocol.ProtocolVersion.SMB2;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public enum SmbDialect {
    Unknown("Unknown", 0x0000, null),
    NTLM_012("NT LM 0.12", 0x0000, CIFS_SMB),
    SMB2_Request("SMB 2.???", 0x0200, CIFS_SMB),
    SMB2_0_2("SMB 2.002", 0x0202, SMB2),
    SMB2_1("SMB 2.1", 0x0210, SMB2),
    SMB2_Wildcard("SMB 2.*", 0x02FF, SMB2),
    SMB3_0("SMB 3.0", 0x0300, SMB2),
    SMB3_0_2("SMB 3.0.2", 0x0302, SMB2),
    SMB3_1_1("SMB 3.1.1", 0x0311, SMB2);

    private static final SmbDialect[] IDENTIFIABLE = {NTLM_012, SMB2_Request, SMB2_0_2};

    private final String identifier;
    private final int code;
    private final ProtocolVersion protocolVersion;

    SmbDialect(String identifier, int code, ProtocolVersion protocolVersion) {
        this.identifier = identifier;
        this.code = code;
        this.protocolVersion = protocolVersion;
    }

    public String identifier() {
        return identifier;
    }

    public int code() {
        return code;
    }

    boolean isSmb2() {
        return code > 0;
    }

    public ProtocolVersion protocolVersion() {
        return protocolVersion;
    }

    public boolean sameOrAfter(final SmbDialect dialect) {
        return dialect != null && code >= dialect.code;
    }

    public boolean before(final SmbDialect dialect) {
        return dialect != null && dialect.code > code;
    }

    public static SmbDialect fromCode(final int code) {
        for (var dialect : values()) {
            if (dialect.code == code) {
                return dialect;
            }
        }
        return Unknown;
    }

    public static SmbDialect fromIdentifier(final String identifier) {
        for (var dialect : IDENTIFIABLE) {
            if (dialect.identifier.equals(identifier)) {
                return dialect;
            }
        }
        return Unknown;
    }

    public static List<SmbDialect> smb1NegotiateDialects(final SmbDialect maxDialect) {
        Objects.requireNonNull(maxDialect);
        return switch (maxDialect.code()) {
            case 0x00 -> List.of(NTLM_012);
            case 0x0202 -> List.of(NTLM_012, SMB2_0_2);
            default -> List.of(NTLM_012, SMB2_Request);
        };
    }

    public static List<SmbDialect> smb2NegotiateDialects(final SmbDialect minDialect, final SmbDialect maxDialect) {
        return Arrays.stream(values())
            .filter(sd -> sd.code() >= minDialect.code && sd.code <= maxDialect.code)
            .toList();
    }
}
