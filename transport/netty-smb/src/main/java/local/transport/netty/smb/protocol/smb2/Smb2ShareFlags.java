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
 * Share Flags Enum. Addresses MS-SMB2 (#2.2.10 SMB2 TREE_CONNECT Response).
 */
public enum Smb2ShareFlags implements Flags.BitMaskProvider {

    SMB2_SHAREFLAG_MANUAL_CACHING(0x00000000),
    SMB2_SHAREFLAG_AUTO_CACHING(0x00000010),
    SMB2_SHAREFLAG_VDO_CACHING(0x00000020),
    SMB2_SHAREFLAG_NO_CACHING(0x00000030),
    SMB2_SHAREFLAG_DFS(0x00000001),
    SMB2_SHAREFLAG_DFS_ROOT(0x00000002),
    SMB2_SHAREFLAG_RESTRICT_EXCLUSIVE_OPENS(0x00000100),
    SMB2_SHAREFLAG_FORCE_SHARED_DELETE(0x00000200),
    SMB2_SHAREFLAG_ALLOW_NAMESPACE_CACHING(0x00000400),
    SMB2_SHAREFLAG_ACCESS_BASED_DIRECTORY_ENUM(0x00000800),
    SMB2_SHAREFLAG_FORCE_LEVELII_OPLOCK(0x00001000),
    SMB2_SHAREFLAG_ENABLE_HASH_V1(0x00002000),
    SMB2_SHAREFLAG_ENABLE_HASH_V2(0x00004000),
    SMB2_SHAREFLAG_ENCRYPT_DATA(0x00008000),
    SMB2_SHAREFLAG_IDENTITY_REMOTING(0x00040000),
    SMB2_SHAREFLAG_COMPRESS_DATA(0x00100000),
    SMB2_SHAREFLAG_ISOLATED_TRANSPORT(0x00200000);

    private final int mask;

    Smb2ShareFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
