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
package local.transport.netty.smb.protocol.cifs;

import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbResponseMessage;

/**
 * SMB_COM_NEGOTIATE Response. Addresses MS-CIFS (#2.2.4.52.2. Response).
 */
public class SmbComNegotiateResponse implements SmbResponseMessage {
    private int dialectIndex;
    private Flags<NegotiateSecurityFlags> securityMode;
    private int maxMpxCount;
    private int maxNumberVcs;
    private long maxBufferSize;
    private long maxRawSize;
    private byte[] sessionKey;
    private Flags<CapabilitiesFlags> capabilities;
    private byte[] systemTime;
    private byte[] serverTimeZone;
    private byte[] challenge;
    private String domainName;

    @Override
    public SmbCommand command() {
        return SmbCommand.SMB_COM_NEGOTIATE;
    }

    public int getDialectIndex() {
        return dialectIndex;
    }

    public void setDialectIndex(final int dialectIndex) {
        this.dialectIndex = dialectIndex;
    }

    public Flags<NegotiateSecurityFlags> getSecurityMode() {
        return securityMode;
    }

    public void setSecurityMode(
        final Flags<NegotiateSecurityFlags> securityMode) {
        this.securityMode = securityMode;
    }

    public int getMaxMpxCount() {
        return maxMpxCount;
    }

    public void setMaxMpxCount(final int maxMpxCount) {
        this.maxMpxCount = maxMpxCount;
    }

    public int getMaxNumberVcs() {
        return maxNumberVcs;
    }

    public void setMaxNumberVcs(final int maxNumberVcs) {
        this.maxNumberVcs = maxNumberVcs;
    }

    public long getMaxBufferSize() {
        return maxBufferSize;
    }

    public void setMaxBufferSize(final long maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
    }

    public long getMaxRawSize() {
        return maxRawSize;
    }

    public void setMaxRawSize(final long maxRawSize) {
        this.maxRawSize = maxRawSize;
    }

    public byte[] getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(final byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public Flags<CapabilitiesFlags> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(
        final Flags<CapabilitiesFlags> capabilities) {
        this.capabilities = capabilities;
    }

    public byte[] getSystemTime() {
        return systemTime;
    }

    public void setSystemTime(final byte[] systemTime) {
        this.systemTime = systemTime;
    }

    public byte[] getServerTimeZone() {
        return serverTimeZone;
    }

    public void setServerTimeZone(final byte[] serverTimeZone) {
        this.serverTimeZone = serverTimeZone;
    }

    public byte[] getChallenge() {
        return challenge;
    }

    public void setChallenge(final byte[] challenge) {
        this.challenge = challenge;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(final String domainName) {
        this.domainName = domainName;
    }
}
