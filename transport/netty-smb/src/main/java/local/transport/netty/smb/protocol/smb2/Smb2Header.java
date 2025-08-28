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
import local.transport.netty.smb.protocol.ProtocolVersion;
import local.transport.netty.smb.protocol.SmbCommand;
import local.transport.netty.smb.protocol.SmbError;
import local.transport.netty.smb.protocol.SmbHeader;

public class Smb2Header implements SmbHeader {

    private SmbCommand command;
    private SmbError status;
    private int channelSequence;
    private int creditCharge;
    private int creditRequest;
    private int creditResponse;
    private Flags<Smb2Flags> flags;
    private int nextCommandOffset;
    private long messageId;
    private long asyncId;
    private int treeId;
    private long sessionId;
    private byte[] signature;

    @Override
    public ProtocolVersion protocolVersion() {
        return ProtocolVersion.SMB2;
    }

    @Override
    public SmbCommand command() {
        return command;
    }

    public void setCommand(final SmbCommand command) {
        this.command = command;
    }

    public SmbError status() {
        return status;
    }

    public void setStatus(final SmbError status) {
        this.status = status;
    }

    public int channelSequence() {
        return channelSequence;
    }

    public void setChannelSequence(final int channelSequence) {
        this.channelSequence = channelSequence;
    }

    public int creditCharge() {
        return creditCharge;
    }

    public void setCreditCharge(final int creditCharge) {
        this.creditCharge = creditCharge;
    }

    public int creditRequest() {
        return creditRequest;
    }

    public void setCreditRequest(final int creditRequest) {
        this.creditRequest = creditRequest;
    }

    public int creditResponse() {
        return creditResponse;
    }

    public void setCreditResponse(final int creditResponse) {
        this.creditResponse = creditResponse;
    }

    public Flags<Smb2Flags> flags() {
        return flags;
    }

    public void setFlags(final Flags<Smb2Flags> flags) {
        this.flags = flags;
    }

    public int nextCommandOffset() {
        return nextCommandOffset;
    }

    public void setNextCommandOffset(final int nextCommandOffset) {
        this.nextCommandOffset = nextCommandOffset;
    }

    public long messageId() {
        return messageId;
    }

    public void setMessageId(final long messageId) {
        this.messageId = messageId;
    }

    public long asyncId() {
        return asyncId;
    }

    public void setAsyncId(final long asyncId) {
        this.asyncId = asyncId;
    }

    public int treeId() {
        return treeId;
    }

    public void setTreeId(final int treeId) {
        this.treeId = treeId;
    }

    public long sessionId() {
        return sessionId;
    }

    public void setSessionId(final long sessionId) {
        this.sessionId = sessionId;
    }

    public byte[] signature() {
        return signature;
    }

    public void setSignature(final byte[] signature) {
        this.signature = signature;
    }

    boolean isResponse() {
        return flagValue(Smb2Flags.SMB2_FLAGS_SERVER_TO_REDIR);
    }

    public boolean isAsync() {
        return flagValue(Smb2Flags.SMB2_FLAGS_ASYNC_COMMAND);
    }

    private boolean flagValue(final Smb2Flags bit){
        return flags != null && flags.get(bit);
    }
}
