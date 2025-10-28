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

import java.util.Arrays;
import java.util.List;

public enum Smb2Dialect {
    Unknown("Unknown", 0x0000),
    SMB2_0_2("SMB 2.0.2", 0x0202),
    SMB2_1("SMB 2.1", 0x0210),
    SMB3_0("SMB 3.0", 0x0300),
    SMB3_0_2("SMB 3.0.2", 0x0302),
    SMB3_1_1("SMB 3.1.1", 0x0311);

    private final String identifier;
    private final int code;

    Smb2Dialect(String identifier, int code) {
        this.identifier = identifier;
        this.code = code;
    }

    public String identifier() {
        return identifier;
    }

    public int code() {
        return code;
    }

    public boolean equalsOrHigher(final Smb2Dialect dialect) {
        return dialect != null && code >= dialect.code;
    }

    public boolean before(final Smb2Dialect dialect) {
        return dialect != null && dialect.code > code;
    }

    public static Smb2Dialect fromCode(final int code) {
        for (var dialect : values()) {
            if (dialect.code == code) {
                return dialect;
            }
        }
        return Unknown;
    }

    public static List<Smb2Dialect> negotiateDialects(final Smb2Dialect minDialect, final Smb2Dialect maxDialect) {
        return Arrays.stream(values())
            .filter(sd -> sd.code() >= minDialect.code && sd.code <= maxDialect.code)
            .toList();
    }
}
