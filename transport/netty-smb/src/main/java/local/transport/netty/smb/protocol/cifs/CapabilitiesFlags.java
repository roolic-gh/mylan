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
package local.transport.netty.smb.protocol.cifs;

import local.transport.netty.smb.protocol.Flags;

/**
 * Capabilities Flags. Addresses MS-CIFS (#2.2.4.52.2 Response).
 */
public enum CapabilitiesFlags implements Flags.BitMaskProvider {
    CAP_RAW_MODE(0x00000001),
    CAP_MPX_MODE(0x00000002),
    CAP_UNICODE(0x00000004),
    CAP_LARGE_FILES(0x00000008),
    CAP_NT_SMBS(0x00000010),
    CAP_RPC_REMOTE_APIS(0x00000020),
    CAP_STATUS32(0x00000040),
    CAP_LEVEL_II_OPLOCKS(0x00000080),
    CAP_LOCK_AND_READ(0x00000100),
    CAP_NT_FIND(0x00000200),
    CAP_BULK_TRANSFER(0x00000400),
    CAP_COMPRESSED_DATA(0x00000800),
    CAP_DFS(0x00001000),
    CAP_QUADWORD_ALIGNED(0x00002000),
    CAP_LARGE_READX(0x00004000);

    private final int mask;

    CapabilitiesFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
