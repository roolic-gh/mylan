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
package local.mylan.transport.smb.protocol.smb2;

import local.mylan.transport.smb.protocol.Flags;

/**
 * Session setup response flags. Addresses MS-SMB2 (#2.2.6 SMB2 SESSION_SETUP Response).
 */
public enum Smb2SessionResponseFlags implements Flags.BitMaskProvider {
    SMB2_SESSION_FLAG_IS_GUEST(0x0001),
    SMB2_SESSION_FLAG_IS_NULL(0x0002), // anonimous
    SMB2_SESSION_FLAG_ENCRYPT_DATA(0x0004);

    private final int mask;

    Smb2SessionResponseFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
