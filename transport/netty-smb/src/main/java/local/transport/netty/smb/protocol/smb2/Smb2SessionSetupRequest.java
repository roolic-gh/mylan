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
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.spnego.NegToken;

/**
 * SMB2 Session Setup Request. Addresses MS-SMB2 (#2.2.5 SMB2 SESSION_SETUP Request).
 */
public class Smb2SessionSetupRequest extends Smb2Request {

    private Flags<Smb2SessionRequestFlags> sessionFlags;
    private Flags<Smb2NegotiateFlags> securityMode;
    private Flags<Smb2CapabilitiesFlags> capabilities;
    private long previousSessionId;
    private NegToken token;

    public Smb2SessionSetupRequest() {
    }

    public Smb2SessionSetupRequest(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_SESSION_SETUP;
    }

    public Flags<Smb2SessionRequestFlags> sessionFlags() {
        return sessionFlags;
    }

    public void setSessionFlags(final Flags<Smb2SessionRequestFlags> sessionFlags) {
        this.sessionFlags = sessionFlags;
    }

    public Flags<Smb2NegotiateFlags> securityMode() {
        return securityMode;
    }

    public void setSecurityMode(final Flags<Smb2NegotiateFlags> securityMode) {
        this.securityMode = securityMode;
    }

    public Flags<Smb2CapabilitiesFlags> capabilities() {
        return capabilities;
    }

    public void setCapabilities(final Flags<Smb2CapabilitiesFlags> capabilities) {
        this.capabilities = capabilities;
    }

    public long previousSessionId() {
        return previousSessionId;
    }

    public void setPreviousSessionId(final long previousSessionId) {
        this.previousSessionId = previousSessionId;
    }

    public NegToken token() {
        return token;
    }

    public void setToken(final NegToken token) {
        this.token = token;
    }
}
