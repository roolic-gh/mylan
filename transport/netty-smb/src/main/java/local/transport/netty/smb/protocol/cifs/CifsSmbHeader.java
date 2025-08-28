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

import com.google.common.base.Objects;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.ProtocolVersion;
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbHeader;

/**
 * SMB Header. Addresses MS-CIFS (#2.2.3.1 The SMB Header)
 */
public class CifsSmbHeader implements SmbHeader {

    private SmbCommand command;
    private SmbError status;
    private Flags<SmbFlags> flags;
    private Flags<SmbFlags2> flags2;
    private byte[] securityFeatures;
    private int treeId;
    private int processId;
    private int userId;
    private int multiplexId;

    @Override
    public ProtocolVersion protocolVersion() {
        return ProtocolVersion.CIFS_SMB;
    }

    @Override
    public SmbCommand command() {
        return command;
    }

    public SmbCommand getCommand() {
        return command;
    }

    public void setCommand(final SmbCommand command) {
        this.command = command;
    }

    public SmbError getStatus() {
        return status;
    }

    public void setStatus(final SmbError status) {
        this.status = status;
    }

    public Flags<SmbFlags> getFlags() {
        return flags;
    }

    public void setFlags(final Flags<SmbFlags> flags) {
        this.flags = flags;
    }

    public Flags<SmbFlags2> getFlags2() {
        return flags2;
    }

    public void setFlags2(final Flags<SmbFlags2> flags2) {
        this.flags2 = flags2;
    }

    public byte[] getSecurityFeatures() {
        return securityFeatures;
    }

    public void setSecurityFeatures(final byte[] securityFeatures) {
        this.securityFeatures = securityFeatures;
    }

    public int getTreeId() {
        return treeId;
    }

    public void setTreeId(final int treeId) {
        this.treeId = treeId;
    }

    public int getProcessIdHigh() {
        return processId >> 8 & 0xFFFF;
    }

    public void setProcessIdHigh(final int processIdHigh) {
        processId &= 0xFFFF;
        processId |= processIdHigh << 8 & 0xFFFF0000;
    }

    public int getProcessIdLow() {
        return processId & 0xFFFF;
    }

    public void setProcessIdLow(final int processIdLow) {
        processId &= 0xFFFF0000;
        processId |= processIdLow & 0xFFFF;
    }

    public int getProcessId() {
        return processId;
    }

    public void setProcessId(final int processId) {
        this.processId = processId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public int getMultiplexId() {
        return multiplexId;
    }

    public void setMultiplexId(final int multiplexId) {
        this.multiplexId = multiplexId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CifsSmbHeader that)) {
            return false;
        }
        return treeId == that.treeId && processId == that.processId
            && userId == that.userId && multiplexId == that.multiplexId && command == that.command
            && status == that.status && Objects.equal(flags, that.flags) && Objects.equal(flags2, that.flags2)
            && Objects.equal(securityFeatures, that.securityFeatures);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(command, status, flags, flags2, securityFeatures, treeId, processId,
            userId, multiplexId);
    }

}
