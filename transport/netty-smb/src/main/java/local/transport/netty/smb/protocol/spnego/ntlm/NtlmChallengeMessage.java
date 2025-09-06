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
 * NTLM Challenge Message. Addresses MS-NLMP (#2.2.1.2 CHALLENGE_MESSAGE).
 */
public class NtlmChallengeMessage implements NtlmMessage {

    private Flags<NtlmNegotiationFlags> negotiationFlags;
    private String targetName;
    private NtlmAvPairs targetInfo;
    private NtlmChallenge serverChallenge;
    private NtlmVersion version;

    @Override
    public NtlmMessageType messageType() {
        return NtlmMessageType.NtLmChallenge;
    }

    public Flags<NtlmNegotiationFlags> negotiationFlags() {
        return negotiationFlags;
    }

    public void setNegotiationFlags(
        final Flags<NtlmNegotiationFlags> negotiationFlags) {
        this.negotiationFlags = negotiationFlags;
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

    public NtlmChallenge serverChallenge() {
        return serverChallenge;
    }

    public void setServerChallenge(final NtlmChallenge serverChallenge) {
        this.serverChallenge = serverChallenge;
    }

    public NtlmVersion version() {
        return version;
    }

    public void setVersion(final NtlmVersion version) {
        this.version = version;
    }
}
