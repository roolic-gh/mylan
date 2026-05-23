/*
 * Copyright 2026 Ruslan Kashapov
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
package local.mylan.transport.smb.protocol.pcerpc;

import java.util.List;
import java.util.UUID;
import local.mylan.transport.smb.protocol.Flags;

/**
 * Connection Oriented PDU Data Types. Addresses C706 PCE 1.1
 * (12.6.3 Connection-oriented PDU Data Types).
 */
public class PduDataTypes {

    private PduDataTypes() {
        // subtype wrapper class
    }

    public record Version(int major, int minor) {
    }

    public record Syntax(UUID uuid, Version version) {
    }

    public record ContextElement(int contextId, Syntax abstractSyntax, List<Syntax> transferSyntaxes) {
    }

    public enum Result {
        acceptance(0),
        user_rejection(1),
        provider_rejection(2);

        final int code;

        Result(final int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

        public static Result fromCode(final int code) {
            for (var res : values()) {
                if (res.code == code) {
                    return res;
                }
            }
            throw new IllegalArgumentException("Invalid Result code " + code);
        }
    }

    public enum ProviderReason {
        reason_not_specified(0),
        abstract_syntax_not_supported(1),
        proposed_transfer_syntaxes_not_supported(2),
        local_limit_exceeded(3);

        final int code;

        ProviderReason(final int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }

        public static ProviderReason fromCode(final int code) {
            for (var pr : values()) {
                if (pr.code == code) {
                    return pr;
                }
            }
            throw new IllegalArgumentException("Invalid Provider Reason code " + code);
        }
    }

    public record ResultElement(Result result, ProviderReason reason, Syntax transferSyntax) {
    }

    public enum PfcFlags implements Flags.BitMaskProvider {
        PFC_FIRST_FRAG(0x01),
        PFC_LAST_FRAG(0x02),
        PFC_PENDING_CANCEL(0x04),
        PFC_CONC_MPX(0x10),
        PFC_DID_NOT_EXECUTE(0x20),
        PFC_MAYBE(0x40),
        PFC_OBJECT_UUID(0x80);

        private final int mask;

        PfcFlags(final int mask) {
            this.mask = mask;
        }

        @Override
        public int mask() {
            return mask;
        }
    }
}
