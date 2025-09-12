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
 * NEGOEX Verify message. Addresses MS-NEGOEX (#2.2.6.5 VERIFY_MESSAGE).
 */
public class NegoexVerifyMessage extends NegoexMessageHeader {
    private UUID authScheme;
    private NegoexChecksum checksum;

    @Override
    public NegoexMessageType messageType() {
        return NegoexMessageType.MESSAGE_TYPE_VERIFY;
    }

    public UUID authScheme() {
        return authScheme;
    }

    public void setAuthScheme(final UUID authScheme) {
        this.authScheme = authScheme;
    }

    public NegoexChecksum checksum() {
        return checksum;
    }

    public void setChecksum(final NegoexChecksum checksum) {
        this.checksum = checksum;
    }
}
