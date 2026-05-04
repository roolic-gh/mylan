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
package local.transport.netty.smb.protocol.srvs;

/**
 * Information Level for Share Info data. Addresses MS-SRVS (#2.2.4.38 SHARE_ENUM_STRUCT).
 */
public enum SrvsShareInfoLevel {
    SHARE_INFO_0(0),
    SHARE_INFO_1(1),
    SHARE_INFO_2(2),
    SHARE_INFO_501(501),
    SHARE_INFO_502(502),
    SHARE_INFO_503(503);

    private final int code;

    SrvsShareInfoLevel(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static SrvsShareInfoLevel fromCode(int code) {
        for (var err : values()) {
            if (err.code == code) {
                return err;
            }
        }
        return null;
    }
}
