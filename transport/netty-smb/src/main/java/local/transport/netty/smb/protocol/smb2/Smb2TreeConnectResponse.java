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
package local.transport.netty.smb.protocol.smb2;

import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Response;

/**
 * SMB2 Tree Connect Response. Addresses MS-SMB2 (#2.2.10 SMB2 TREE_CONNECT Response).
 */
public final class Smb2TreeConnectResponse extends Smb2Response {

    private Smb2ShareType shareType;
    private Flags<Smb2ShareFlags> shareFlags;
    private Flags<Smb2ShareCapabilitiesFlags> capabilities;
    private Flags<Smb2AccessMask> maxAccess;

    public Smb2TreeConnectResponse() {
    }

    public Smb2TreeConnectResponse(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_TREE_CONNECT;
    }

    public Smb2ShareType shareType() {
        return shareType;
    }

    public void setShareType(final Smb2ShareType shareType) {
        this.shareType = shareType;
    }

    public Flags<Smb2ShareFlags> shareFlags() {
        return shareFlags;
    }

    public void setShareFlags(
        final Flags<Smb2ShareFlags> shareFlags) {
        this.shareFlags = shareFlags;
    }

    public Flags<Smb2ShareCapabilitiesFlags> capabilities() {
        return capabilities;
    }

    public void setCapabilities(
        final Flags<Smb2ShareCapabilitiesFlags> capabilities) {
        this.capabilities = capabilities;
    }

    public Flags<Smb2AccessMask> maxAccess() {
        return maxAccess;
    }

    public void setMaxAccess(
        final Flags<Smb2AccessMask> maxAccess) {
        this.maxAccess = maxAccess;
    }
}
