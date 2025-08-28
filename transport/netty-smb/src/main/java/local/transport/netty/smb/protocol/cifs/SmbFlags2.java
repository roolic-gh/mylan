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
 * Addresses MS-CIFS (#2.2.3.1 The SMB Header) and MS-SMB (#2.2.3.1 SMB Header Extensions)
 */
public enum SmbFlags2 implements Flags.BitMaskProvider {
    SMB_FLAGS2_LONG_NAMES(0x0001),
    SMB_FLAGS2_EAS(0x0002),
    SMB_FLAGS2_SMB_SECURITY_SIGNATURE(0x0004),
    SMB_FLAGS2_COMPRESSED(0x0008),
    SMB_FLAGS2_SMB_SECURITY_SIGNATURE_REQUIRED(0x0010),
    SMB_FLAGS2_IS_LONG_NAME(0x0040),
    SMB_FLAGS2_REPARSE_PATH(0x0400),
    SMB_FLAGS2_EXTENDED_SECURITY(0x0800),
    SMB_FLAGS2_DFS(0x1000),
    SMB_FLAGS2_PAGING_IO(0x2000),
    SMB_FLAGS2_NT_STATUS(0x4000),
    SMB_FLAGS2_UNICODE(0x8000);

    private final int mask;

    SmbFlags2(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
