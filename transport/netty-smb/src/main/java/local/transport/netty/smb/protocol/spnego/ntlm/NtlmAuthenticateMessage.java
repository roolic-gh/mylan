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
 * NTLM Authenticate Message. Addresses MS-NLMP (#2.2.1.3 AUTHENTICATE_MESSAGE).
 */
public class NtlmAuthenticateMessage implements NtlmMessage {

    private Flags<NtlmNegotiateFlags> negotiateFlags;
    private NtChallengeResponse ntChallengeResponse;
    private LmChallengeResponse lmChallengeResponse;
    private String domainName;
    private String workstationName;
    private String userName;
    private byte[] encryptedRandomSessionKey;
    private byte[] mic;
    private NtlmVersion version;

    @Override
    public NtlmMessageType messageType() {
        return NtlmMessageType.NtLmAuthenticate;
    }

    public Flags<NtlmNegotiateFlags> negotiateFlags() {
        return negotiateFlags;
    }

    public void setNegotiateFlags(final Flags<NtlmNegotiateFlags> negotiateFlags) {
        this.negotiateFlags = negotiateFlags;
    }

    public NtChallengeResponse ntChallengeResponse() {
        return ntChallengeResponse;
    }

    public void setNtChallengeResponse(final NtChallengeResponse ntChallengeResponse) {
        this.ntChallengeResponse = ntChallengeResponse;
    }

    public LmChallengeResponse lmChallengeResponse() {
        return lmChallengeResponse;
    }

    public void setLmChallengeResponse(final LmChallengeResponse lmChallengeResponse) {
        this.lmChallengeResponse = lmChallengeResponse;
    }

    public String domainName() {
        return domainName;
    }

    public void setDomainName(final String domainName) {
        this.domainName = domainName;
    }

    public String workstationName() {
        return workstationName;
    }

    public void setWorkstationName(final String workstationName) {
        this.workstationName = workstationName;
    }

    public String userName() {
        return userName;
    }

    public void setUserName(final String userName) {
        this.userName = userName;
    }

    public byte[] encryptedRandomSessionKey() {
        return encryptedRandomSessionKey;
    }

    public void setEncryptedRandomSessionKey(final byte[] encryptedRandomSessionKey) {
        this.encryptedRandomSessionKey = encryptedRandomSessionKey;
    }

    public byte[] mic() {
        return mic;
    }

    public void setMic(final byte[] mic) {
        this.mic = mic;
    }

    public NtlmVersion version() {
        return version;
    }

    public void setVersion(final NtlmVersion version) {
        this.version = version;
    }
}
