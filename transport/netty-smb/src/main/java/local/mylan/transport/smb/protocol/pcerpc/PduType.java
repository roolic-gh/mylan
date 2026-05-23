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

/**
 * Connection Oriented PDU Type. Addresses C706 PCE 1.1
 * (#12.1 Generic PDU Structure, 12.6 Connection-oriented RPC PDUs).
 */
public enum PduType {
    request(0),
    response(2),
    fault(3),
    bind(11),
    bind_ack(12),
    bind_nak(13),
    alter_context(14),
    alter_context_resp(15),
    shutdown(17),
    co_cancel(18),
    orphaned(19);

    private final int code;

    PduType(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static PduType fromCode(final int code) {
        for (var pt : values()) {
            if (pt.code == code) {
                return pt;
            }
        }
        throw new IllegalArgumentException("Unknown PDU Type code " + code);
    }

    public static boolean isKnown(final int code) {
        for (var pt : values()) {
            if (pt.code == code) {
                return true;
            }
        }
        return false;
    }
}
