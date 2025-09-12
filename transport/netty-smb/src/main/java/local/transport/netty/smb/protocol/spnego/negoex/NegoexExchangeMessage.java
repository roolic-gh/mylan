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

import com.google.common.base.Preconditions;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * NEGOEX Exchange message. Addresses MS-NEGOEX (#2.2.6.4 EXCHANGE_MESSAGE).
 */
public class NegoexExchangeMessage extends NegoexMessageHeader {
    private static final Set<NegoexMessageType> COMPATIBLE_TYPES = EnumSet.of(
        NegoexMessageType.MESSAGE_TYPE_INITIATOR_META_DATA,
        NegoexMessageType.MESSAGE_TYPE_ACCEPTOR_META_DATA,
        NegoexMessageType.MESSAGE_TYPE_CHALLENGE,
        NegoexMessageType.MESSAGE_TYPE_AP_REQUEST);
    private final NegoexMessageType messageType;

    private UUID authScheme;
    private byte[] exchangeData;

    public NegoexExchangeMessage(final NegoexMessageType messageType) {
        Preconditions.checkArgument(COMPATIBLE_TYPES.contains(messageType), "Incompatible message type");
        this.messageType = messageType;
    }

    @Override
    public NegoexMessageType messageType() {
        return messageType;
    }

    public UUID authScheme() {
        return authScheme;
    }

    public void setAuthScheme(final UUID authScheme) {
        this.authScheme = authScheme;
    }

    public byte[] exchangeData() {
        return exchangeData;
    }

    public void setExchangeData(final byte[] exchangeData) {
        this.exchangeData = exchangeData;
    }
}
