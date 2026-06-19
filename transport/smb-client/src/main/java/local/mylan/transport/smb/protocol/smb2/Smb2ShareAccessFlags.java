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
 * Share Access Flags Enum. Addresses MS-SMB2 (#2.2.13 SMB2 CREATE Request).
 */
public enum Smb2ShareAccessFlags implements Flags.BitMaskProvider {
    FILE_SHARE_READ(0x00000001),
    FILE_SHARE_WRITE(0x00000002),
    FILE_SHARE_DELETE(0x00000004);

    private final int mask;

    Smb2ShareAccessFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
