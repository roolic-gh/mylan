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

/**
 * Addresses MS-NLMP (#2.2.2.7 NTLM v2: NTLMv2_CLIENT_CHALLENGE).
 */
public class NtlmV2ClientChallenge {
    private byte[] clientChallenge;
    private Long timestamp;
    private NtlmAvPairs avPairs;

    public int respType(){
        return 1;
    }

    public int hiRespType(){
        return 1;
    }

    public byte[] clientChallenge() {
        return clientChallenge;
    }

    public void setClientChallenge(final byte[] clientChallenge) {
        this.clientChallenge = clientChallenge;
    }

    public Long timestamp() {
        return timestamp;
    }

    public void setTimestamp(final Long timestamp) {
        this.timestamp = timestamp;
    }

    public void setAvPairs(final NtlmAvPairs avPairs) {
        this.avPairs = avPairs;
    }

    public NtlmAvPairs avPairs() {
        return avPairs;
    }
}
