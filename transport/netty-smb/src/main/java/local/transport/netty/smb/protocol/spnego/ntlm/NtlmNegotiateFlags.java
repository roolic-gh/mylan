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
 * NTLM Negotiate Flags. Addresses MS-NLMP (#2.2.2.5 NEGOTIATE).
 */
public enum NtlmNegotiateFlags implements Flags.BitMaskProvider {
    NTLMSSP_NEGOTIATE_56(0x80000000),
    NTLMSSP_NEGOTIATE_KEY_EXCHANGE(0x40000000),
    NTLMSSP_NEGOTIATE_128(0x20000000),
    NTLMSSP_NEGOTIATE_VERSION(0x02000000),
    NTLMSSP_NEGOTIATE_TARGET_INFO(0x00800000),
    NTLMSSP_REQUEST_NON_NT_SESSION_KEY(0x00400000),
    NTLMSSP_NEGOTIATE_IDENTIFY(0x00100000),
    NTLMSSP_NEGOTIATE_EXTENDED_SESSION_SECURITY(0x00080000),
    NTLMSSP_TARGET_TYPE_SERVER(0x00020000),
    NTLMSSP_TARGET_TYPE_DOMAIN(0x00010000),
    NTLMSSP_NEGOTIATE_ALWAYS_SIGN(0x00008000),
    NTLMSSP_NEGOTIATE_OEM_WORKSTATION_SUPPLIED(0x00002000),
    NTLMSSP_NEGOTIATE_OEM_DOMAIN_SUPPLIED(0x00001000),
    NTLMSSP_NEGOTIATE_ANONIMOUS(0x00000800),
    NTLMSSP_NEGOTIATE_NTLM(0x00000200),
    NTLMSSP_NEGOTIATE_LM_KEY(0x00000080),
    NTLMSSP_NEGOTIATE_DATAGRAM(0x00000040),
    NTLMSSP_NEGOTIATE_SEAL(0x00000020),
    NTLMSSP_NEGOTIATE_SIGN(0x00000010),
    NTLMSSP_REQUEST_TARGET(0x00000004),
    NTLM_NEGOTIATE_OEM(0x00000002),
    NTLMSSP_NEGOTIATE_UNICODE(0x00000001);

    private final int mask;

    NtlmNegotiateFlags(final int mask) {
        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
