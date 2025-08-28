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
package local.transport.netty.smb.protocol.smb2;

import local.transport.netty.smb.protocol.Flags;

/**
 * Capabilities flags. Addresses MS-SMB2 (#2.2.3 SMB2 NEGOTIATE Request).
 */
public enum Smb2CapabilitiesFlags implements Flags.BitMaskProvider {
    SMB2_GLOBAL_CAP_DFS(0x00000001),
    SMB2_GLOBAL_CAP_LEASING(0x00000002),
    SMB2_GLOBAL_CAP_LARGE_MTU(0x00000004),
    SMB2_GLOBAL_CAP_MULTI_CHANNEL(0x00000008),
    SMB2_GLOBAL_CAP_PERSISTENT_HANDLES(0x00000010),
    SMB2_GLOBAL_CAP_DIRECTORY_LEASING(0x00000020),
    SMB2_GLOBAL_CAP_ENCRYPTION(0x00000040),
    SMB2_GLOBAL_CAP_NOTIFICATIONS(0x00000080);

    private final int mask;

    Smb2CapabilitiesFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
