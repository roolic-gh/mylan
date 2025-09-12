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
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * NEGOEX Negotiation message. Addresses MS-NEGOEX (#2.2.6.3 NEGO_MESSAGE).
 */
public class NegoexNegoMessage extends NegoexMessageHeader {
    private static final Set<NegoexMessageType> COMPATIBLE_TYPES = EnumSet.of(
        NegoexMessageType.MESSAGE_TYPE_INITIATOR_NEGO,
        NegoexMessageType.MESSAGE_TYPE_ACCEPTOR_NEGO);
    private final NegoexMessageType messageType;

    private byte[] random;
    private long protocolVersion;
    private List<UUID> authSchemes;
    private List<NegoexExtension> extensions;

    public NegoexNegoMessage(final NegoexMessageType messageType) {
        Preconditions.checkArgument(COMPATIBLE_TYPES.contains(messageType), "Incompatible message type");
        this.messageType = messageType;
    }

    @Override
    public NegoexMessageType messageType() {
        return messageType;
    }

    public byte[] random() {
        return random;
    }

    public void setRandom(final byte[] random) {
        this.random = random;
    }

    public long protocolVersion() {
        return protocolVersion;
    }

    public void setProtocolVersion(final long protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public List<UUID> authSchemes() {
        return authSchemes;
    }

    public void setAuthSchemes(final List<UUID> authSchemes) {
        this.authSchemes = authSchemes;
    }

    public List<NegoexExtension> extensions() {
        return extensions;
    }

    public void setExtensions(final List<NegoexExtension> extensions) {
        this.extensions = extensions;
    }
}
