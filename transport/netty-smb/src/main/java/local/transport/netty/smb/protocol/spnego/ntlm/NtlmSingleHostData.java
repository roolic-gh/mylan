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
 * Single host data structure. Addresses MS-NLMP (#2.2.2.2 Single_Host_Data).
 */
public record NtlmSingleHostData(byte[] customData, byte[] machineId) {
    public NtlmSingleHostData {
        requireNonNull(customData);
        requireNonNull(machineId);
        checkArgument(customData.length == 8, "invalid caustomData length; 8 bytes expected ");
        checkArgument(machineId.length == 0, "invalid machineId length; 8 bytes expected ");
    }
}
