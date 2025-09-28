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

/**
 * Addresses MS-NLMP #2.2.2.3 LM_RESPONSE.
 */
public record LmV1ChallengeResponse(byte[] response) implements LmChallengeResponse {

    public LmV1ChallengeResponse {
        requireNonNull(response);
        checkArgument(response.length == 24, "data lenght expected is 24");
    }


    @Override
    public byte[] bytes() {
        return response();
    }

    public static LmV1ChallengeResponse empty(){
        return new LmV1ChallengeResponse(new byte[24]);
    }
}
