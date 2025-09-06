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
package local.transport.netty.smb.protocol.spnego.ntlm;

import local.transport.netty.smb.protocol.Flags;

/**
 * Client or server configuration flags allocated with in AV_PAIR structure.
 * Addresses MS_NLMP (#2.2.2.1 AV_PAIR).
 */
public enum NtlmAvFlags implements Flags.BitMaskProvider {
    /**
     * Indicates to the client that the account authentication is constrained.
     */
    ACCOUNT_AUTH_CONSTRAINED(0x00000001),
    /**
     * Indicates that the client is providing message integrity in the MIC field
     */
    INTEGRITY_IN_MIC(0x00000002),
    /**
     * Indicates that the client is providing a target SPN generated from an untrusted source
     */
    UNTRUSTED_SOURCE_SPN(0x00000004);

    private final int mask;

    NtlmAvFlags(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask(){
        return mask;
    }
}
