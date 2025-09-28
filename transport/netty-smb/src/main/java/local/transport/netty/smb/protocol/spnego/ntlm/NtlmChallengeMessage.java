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
import local.transport.netty.smb.protocol.spnego.ContainsSelfEncoded;

/**
 * NTLM Challenge Message. Addresses MS-NLMP (#2.2.1.2 CHALLENGE_MESSAGE).
 */
public class NtlmChallengeMessage implements NtlmMessage, ContainsSelfEncoded {

    private Flags<NtlmNegotiateFlags> negotiateFlags;
    private String targetName;
    private NtlmAvPairs targetInfo;
    private byte[] serverChallenge;
    private NtlmVersion version;
    private byte[] encoded;

    @Override
    public NtlmMessageType messageType() {
        return NtlmMessageType.NtLmChallenge;
    }

    public Flags<NtlmNegotiateFlags> negotiateFlags() {
        return negotiateFlags;
    }

    public void setNegotiateFlags(final Flags<NtlmNegotiateFlags> negotiateFlags) {
        this.negotiateFlags = negotiateFlags;
    }

    public String targetName() {
        return targetName;
    }

    public void setTargetName(final String targetName) {
        this.targetName = targetName;
    }

    public NtlmAvPairs targetInfo() {
        return targetInfo;
    }

    public void setTargetInfo(final NtlmAvPairs targetInfo) {
        this.targetInfo = targetInfo;
    }

    public byte[] serverChallenge() {
        return serverChallenge;
    }

    public void setServerChallenge(final byte[] serverChallenge) {
        this.serverChallenge = serverChallenge;
    }

    public NtlmVersion version() {
        return version;
    }

    public void setVersion(final NtlmVersion version) {
        this.version = version;
    }

    @Override
    public byte[] encoded() {
        return encoded;
    }

    @Override
    public void setEncoded(final byte[] encoded) {
        this.encoded = encoded;
    }
}
