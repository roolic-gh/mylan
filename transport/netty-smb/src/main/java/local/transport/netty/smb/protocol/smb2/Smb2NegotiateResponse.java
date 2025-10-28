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

import java.util.UUID;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Dialect;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Response;
import local.transport.netty.smb.protocol.spnego.NegToken;

/**
 *  SMB2 Negotiate response. Addresses MS-SMB2 (#2.2.4 SMB2 NEGOTIATE Response).
 */
public class Smb2NegotiateResponse extends Smb2Response {

    private Smb2Dialect dialectRevision;
    private Flags<Smb2NegotiateFlags> securityMode;
    private Flags<Smb2CapabilitiesFlags> capabilities;
    private int maxTransactSize;
    private int MaxReadSize;
    private int maxWriteSize;
    private UUID serverGuid;
    private long systemTime;
    private long serverStartTime;

    private NegToken token;

    public Smb2NegotiateResponse() {
    }

    public Smb2NegotiateResponse(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_NEGOTIATE;
    }

    public Smb2Dialect dialectRevision() {
        return dialectRevision;
    }

    public void setDialectRevision(final Smb2Dialect dialectRevision) {
        this.dialectRevision = dialectRevision;
    }

    public Flags<Smb2NegotiateFlags> securityMode() {
        return securityMode;
    }

    public void setSecurityMode(
        final Flags<Smb2NegotiateFlags> securityMode) {
        this.securityMode = securityMode;
    }

    public Flags<Smb2CapabilitiesFlags> capabilities() {
        return capabilities;
    }

    public void setCapabilities(
        final Flags<Smb2CapabilitiesFlags> capabilities) {
        this.capabilities = capabilities;
    }

    public int maxTransactSize() {
        return maxTransactSize;
    }

    public void setMaxTransactSize(final int maxTransactSize) {
        this.maxTransactSize = maxTransactSize;
    }

    public int maxReadSize() {
        return MaxReadSize;
    }

    public void setMaxReadSize(final int maxReadSize) {
        MaxReadSize = maxReadSize;
    }

    public int maxWriteSize() {
        return maxWriteSize;
    }

    public void setMaxWriteSize(final int maxWriteSize) {
        this.maxWriteSize = maxWriteSize;
    }

    public long systemTime() {
        return systemTime;
    }

    public void setSystemTime(final long systemTime) {
        this.systemTime = systemTime;
    }

    public long serverStartTime() {
        return serverStartTime;
    }

    public void setServerStartTime(final long serverStartTime) {
        this.serverStartTime = serverStartTime;
    }

    public UUID serverGuid() {
        return serverGuid;
    }

    public void setServerGuid(final UUID serverGuid) {
        this.serverGuid = serverGuid;
    }

    public NegToken token() {
        return token;
    }

    public void setToken(final NegToken token) {
        this.token = token;
    }
}
