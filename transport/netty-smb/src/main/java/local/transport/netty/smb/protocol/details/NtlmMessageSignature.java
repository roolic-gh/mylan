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
package local.transport.netty.smb.protocol.details;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import local.transport.netty.smb.protocol.spnego.MechListMIC;

/**
 * Addresses MS-NLMP (#2.2.2.9 NTLMSSP_MESSAGE_SIGNATURE).
 */
public class NtlmMessageSignature implements MechListMIC {
    final byte[] randomPad;
    final byte[] checksum;
    final int seqNum;

    public NtlmMessageSignature(final byte[] randomPad, final byte[] checksum, final int seqNum) {
        checkArgument(requireNonNull(randomPad).length == 4, "random pad length expected to be 4 bytes");
        checkArgument(requireNonNull(checksum).length == 4, "checksum length expected to be 4 bytes");
        this.randomPad = randomPad;
        this.checksum = checksum;
        this.seqNum = seqNum;
    }

    public NtlmMessageSignature(final byte[] checksum, final int seqNum) {
        checkArgument(requireNonNull(checksum).length == 8, "checksum length expected to be 8 bytes");
        this.checksum = checksum;
        this.seqNum = seqNum;
        randomPad = null;
    }

    public int version() {
        return 1;
    }

    public byte[] randomPad() {
        return randomPad;
    }

    public byte[] checksum() {
        return checksum;
    }

    public int seqNum() {
        return seqNum;
    }
}
