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

public enum SrvsError {
    NERR_Success(0x00000000),
    ERROR_MORE_DATA(0x000000EA),
    ERROR_INVALID_LEVEL(0x0000007C);

    private final int code;

    SrvsError(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static SrvsError fromCode(int code) {
        for (var err : values()) {
            if (err.code == code) {
                return err;
            }
        }
        return null;
    }
}
