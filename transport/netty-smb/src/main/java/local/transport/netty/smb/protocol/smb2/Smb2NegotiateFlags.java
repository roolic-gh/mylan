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
 * Addresses MS-SMB2 (2.2.3 SMB2 NEGOTIATE Request)
 */
public enum Smb2NegotiateFlags implements Flags.BitMaskProvider {
    SMB2_NEGOTIATE_SIGNING_ENABLED(0x0001),
    SMB2_NEGOTIATE_SIGNING_REQUIRED(0x0002);

    private final int mask;

    Smb2NegotiateFlags(int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
