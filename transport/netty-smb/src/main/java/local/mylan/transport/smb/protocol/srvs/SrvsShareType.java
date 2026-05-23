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
package local.mylan.transport.smb.protocol.srvs;

import static java.util.Objects.requireNonNull;

/**
 * Share Type. Addresses MS-SRVS (#2.2.2.4 Share Types).
 */
public class SrvsShareType {
    private static final int SPECIAL = 0x80000000;
    private static final int TEMPORARY = 0x40000000;

    private final SType type;
    private final boolean special;
    private final boolean temporary;
    private final int code;

    public SrvsShareType(final int code) {
        this.code = code;
        type = SType.fromCode(code & 0x0FFFFFFF);
        special = (code & SPECIAL) != 0;
        temporary = (code & TEMPORARY) != 0;
    }


    public SrvsShareType(final SType type, final boolean special, final boolean temporary) {
        this.type = requireNonNull(type);
        this.special = special;
        this.temporary = temporary;
        code = type.code | (special ? SPECIAL : 0) | (temporary ? TEMPORARY : 0);
    }

    public SType type() {
        return type;
    }

    public boolean special() {
        return special;
    }

    public boolean temporary() {
        return temporary;
    }

    public int code() {
        return code;
    }

    public enum SType {
        STYPE_DISKTREE(0x00000000),
        STYPE_PRINTQ(0x00000001),
        STYPE_DEVICE(0x00000002),
        STYPE_IPC(0x00000003),
        STYPE_CLUSTER_FS(0x02000000),
        STYPE_CLUSTER_SOFS(0x04000000),
        STYPE_CLUSTER_DFS(0x08000000);

        private final int code;

        SType(final int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

        public static SType fromCode(int code) {
            for (var err : values()) {
                if (err.code == code) {
                    return err;
                }
            }
            return STYPE_DISKTREE;
        }
    }
}
