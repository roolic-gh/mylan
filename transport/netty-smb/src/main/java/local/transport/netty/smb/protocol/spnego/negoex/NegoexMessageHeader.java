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

import java.util.UUID;

/**
 * NEGOEX Message Header. Addresses MS-NEGOEX (#2.2.6.2 MESSAGE_HEADER).
 */
public abstract class NegoexMessageHeader implements NegoexMessage {

    private int sequenceNum;
    private int cbHeaderLength;
    private int cbMessageLength;
    private UUID conversationId;

    public int sequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(final int sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public int cbHeaderLength() {
        return cbHeaderLength;
    }

    public void setCbHeaderLength(final int cbHeaderLength) {
        this.cbHeaderLength = cbHeaderLength;
    }

    public int cbMessageLength() {
        return cbMessageLength;
    }

    public void setCbMessageLength(final int cbMessageLength) {
        this.cbMessageLength = cbMessageLength;
    }

    public UUID conversationId() {
        return conversationId;
    }

    public void setConversationId(final UUID conversationId) {
        this.conversationId = conversationId;
    }
}
