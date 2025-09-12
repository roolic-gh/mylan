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
package local.transport.netty.smb.protocol.spnego.negoex;

/**
 * NEGOEX Message type. Addresses MS-NEGOEX (#2.2.6.1 MESSAGE_TYPE).
 */
public enum NegoexMessageType {
    MESSAGE_TYPE_INITIATOR_NEGO(0),
    MESSAGE_TYPE_ACCEPTOR_NEGO(1),
    MESSAGE_TYPE_INITIATOR_META_DATA(2),
    MESSAGE_TYPE_ACCEPTOR_META_DATA(3),
    MESSAGE_TYPE_CHALLENGE(4),
    MESSAGE_TYPE_AP_REQUEST(5),
    MESSAGE_TYPE_VERIFY(6),
    MESSAGE_TYPE_ALERT(7);

    final int code;

    NegoexMessageType(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static NegoexMessageType fromCode(final int code) {
        for (var nmt : values()) {
            if (nmt.code == code) {
                return nmt;
            }
        }
        return null;
    }
}
