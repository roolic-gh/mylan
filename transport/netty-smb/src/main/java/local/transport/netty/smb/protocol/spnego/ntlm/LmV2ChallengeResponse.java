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

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;

/**
 * Addresses MS-NLMP #2.2.2.4 LMv2_RESPONSE.
 */

public class LmV2ChallengeResponse implements LmChallengeResponse {

    private final byte[] bytes;

    public LmV2ChallengeResponse(final byte[] bytes) {
        checkArgument(requireNonNull(bytes).length == 24, "data lenght expected is 24");
        this.bytes = bytes;
    }

    public LmV2ChallengeResponse(final byte[] response, final byte[] clientChallenge ) {
        checkArgument(requireNonNull(response).length == 16, "response lenght expected is 16");
        checkArgument(requireNonNull(clientChallenge).length == 8, "client challenge lenght expected is 8");
        bytes = new byte[24];
        System.arraycopy(response, 0, bytes, 0, 16);
        System.arraycopy(clientChallenge, 0, bytes, 16, 8);
    }

    @Override
    public byte[] bytes() {
        return bytes;
    }

    public byte[] response(){
        return Arrays.copyOfRange(bytes, 0,16);
    }

    public byte[] clientChallenge(){
        return Arrays.copyOfRange(bytes, 16,24);
    }

    public static LmV2ChallengeResponse empty(){
        return new LmV2ChallengeResponse(new byte[24]);
    }

}
