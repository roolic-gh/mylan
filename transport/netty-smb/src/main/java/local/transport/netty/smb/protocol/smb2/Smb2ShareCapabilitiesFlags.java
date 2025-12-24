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
 * Share Capabilities Enum. Addresses MS-SMB2 (#2.2.10 SMB2 TREE_CONNECT Response).
 */
public enum Smb2ShareCapabilitiesFlags implements Flags.BitMaskProvider {

    SMB2_SHARE_CAP_DFS(0x00000008),
    SMB2_SHARE_CAP_CONTINUOUS_AVAILABILITY(0x00000010),
    SMB2_SHARE_CAP_SCALEOUT(0x00000020),
    SMB2_SHARE_CAP_CLUSTER(0x00000040),
    SMB2_SHARE_CAP_ASYMMETRIC(0x00000080),
    SMB2_SHARE_CAP_REDIRECT_TO_OWNER(0x00000100);

    private final int mask;

    Smb2ShareCapabilitiesFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
