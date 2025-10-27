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
package local.transport.netty.smb.protocol.details;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.SmbDialect;
import local.transport.netty.smb.protocol.SmbRequest;
import local.transport.netty.smb.protocol.smb2.Smb2CapabilitiesFlags;
import local.transport.netty.smb.protocol.smb2.Smb2NegotiateFlags;
import local.transport.netty.smb.protocol.spnego.NegToken;

/**
 * SMB2 Transport connection details. Addresses MS_SMB2 (3.2.1.2 Per SMB2 Transport Connection).
 */
public class ConnectionDetails {

    private final int connectionId;

    final Map<Long, Session> sessions = new ConcurrentHashMap<>();
    final Map<Long, Session> preauthSessions = new ConcurrentHashMap<>();
    final Queue<SmbRequest> pendingRequests = new ConcurrentLinkedDeque<>();
    final SequenceWindow sequenceWindow = new SequenceWindow(); // messageId sequencer

    private NegToken negotiateToken;
    private int maxTransactSize;
    private int maxReadSize;
    private int maxWriteSize;

    // SMB 2.1 +
    boolean supportsFileLeasing;
    boolean supportsMultiCredit;
    private final UUID clientGuid;

    // SMB 3.0 +
    boolean supportsDirectoryLeasing;
    boolean supportsMultiChannel;
    boolean supportsPersistentHandles;
    boolean supportsEncryption;
    Flags<Smb2CapabilitiesFlags> clientCapabilities;
    Flags<Smb2NegotiateFlags> clientSecurityMode;
    ServerDetails server;
    List<SmbDialect> offeredDialects;

    // SMB 3.1.1+
    String preauthIntegrityHashId;
    byte[] preauthIntegrityHashValue;
    String cipherId;
    boolean supportsChainedCompression;
    List<String> rdmaTransformIds;
    String signingAlgorithmId;
    boolean acceptTransportSecurity;
    boolean supportsNotifications;

    // non-spec
    private int setupCreditsRequest = 1;

    public ConnectionDetails(final UUID clientGuid, final int connectionId) {
        this.connectionId = connectionId;
        this.clientGuid = clientGuid;
    }

    public int connectionId() {
        return connectionId;
    }

    public UUID clientGuid() {
        return clientGuid;
    }

    public Map<Long, Session> sessions() {
        return sessions;
    }

    public Map<Long, Session> preauthSessions() {
        return preauthSessions;
    }

    public Queue<SmbRequest> pendingRequests() {
        return pendingRequests;
    }

    public SequenceWindow sequenceWindow() {
        return sequenceWindow;
    }

    public int setupCreditsRequest() {
        return setupCreditsRequest;
    }

    public void setSetupCreditsRequest(final int setupCreditsRequest) {
        this.setupCreditsRequest = setupCreditsRequest;
    }

    public NegToken negotiateToken() {
        return negotiateToken;
    }

    public void setNegotiateToken(final NegToken negotiateToken) {
        this.negotiateToken = negotiateToken;
    }

    public int maxTransactSize() {
        return maxTransactSize;
    }

    public void setMaxTransactSize(final int maxTransactSize) {
        this.maxTransactSize = maxTransactSize;
    }

    public int maxReadSize() {
        return maxReadSize;
    }

    public void setMaxReadSize(final int maxReadSize) {
        this.maxReadSize = maxReadSize;
    }

    public int maxWriteSize() {
        return maxWriteSize;
    }

    public void setMaxWriteSize(final int maxWriteSize) {
        this.maxWriteSize = maxWriteSize;
    }

    public UUID serverGuid() {
        return server == null ? null : server.serverGuid();
    }

    public boolean requireSigning() {
        return server == null || server.securityMode() == null
            ? false : server.securityMode().get(Smb2NegotiateFlags.SMB2_NEGOTIATE_SIGNING_REQUIRED);
    }

    public String serverName() {
        return server == null ? null : server.serverName();
    }

    public SmbDialect dialect() {
        return server == null ? SmbDialect.Unknown : server.dialectRevision();
    }

    public boolean supportsFileLeasing() {
        return serverCapability(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_LEASING);
    }

    public boolean supportsMultiCredit() {
        return false;
    }

    public boolean supportsDirectoryLeasing() {
        return serverCapability(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_DIRECTORY_LEASING);
    }

    public boolean supportsMultiChannel() {
        return serverCapability(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_MULTI_CHANNEL);
    }

    public boolean supportsPersistentHandles() {
        return serverCapability(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_PERSISTENT_HANDLES);
    }

    public boolean supportsEncryption() {
        return serverCapability(Smb2CapabilitiesFlags.SMB2_GLOBAL_CAP_ENCRYPTION);
    }

    private boolean serverCapability(final Smb2CapabilitiesFlags flag) {
        return server == null || server.capabilities() == null ? false : server.capabilities().get(flag);
    }

    public Flags<Smb2CapabilitiesFlags> clientCapabilities() {
        return clientCapabilities;
    }

    public void setClientCapabilities(
        final Flags<Smb2CapabilitiesFlags> clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }

    public Flags<Smb2NegotiateFlags> clientSecurityMode() {
        return clientSecurityMode;
    }

    public void setClientSecurityMode(
        final Flags<Smb2NegotiateFlags> clientSecurityMode) {
        this.clientSecurityMode = clientSecurityMode;
    }

    public ServerDetails server() {
        return server;
    }

    public void setServer(final ServerDetails server) {
        this.server = server;
    }

    public List<SmbDialect> offeredDialects() {
        return offeredDialects;
    }

    public void setOfferedDialects(final List<SmbDialect> offeredDialects) {
        this.offeredDialects = offeredDialects;
    }

    public String preauthIntegrityHashId() {
        return preauthIntegrityHashId;
    }

    public void setPreauthIntegrityHashId(final String preauthIntegrityHashId) {
        this.preauthIntegrityHashId = preauthIntegrityHashId;
    }

    public byte[] preauthIntegrityHashValue() {
        return preauthIntegrityHashValue;
    }

    public void setPreauthIntegrityHashValue(final byte[] preauthIntegrityHashValue) {
        this.preauthIntegrityHashValue = preauthIntegrityHashValue;
    }

    public String cipherId() {
        return server == null ? null : server.cipherId();
    }

    public boolean supportsChainedCompression() {
        return supportsChainedCompression;
    }

    public void setSupportsChainedCompression(final boolean supportsChainedCompression) {
        this.supportsChainedCompression = supportsChainedCompression;
    }

    public List<String> rdmaTransformIds() {
        return rdmaTransformIds;
    }

    public void setRdmaTransformIds(final List<String> rdmaTransformIds) {
        this.rdmaTransformIds = rdmaTransformIds;
    }

    public String signingAlgorithmId() {
        return signingAlgorithmId;
    }

    public void setSigningAlgorithmId(final String signingAlgorithmId) {
        this.signingAlgorithmId = signingAlgorithmId;
    }

    public boolean acceptTransportSecurity() {
        return acceptTransportSecurity;
    }

    public void setAcceptTransportSecurity(final boolean acceptTransportSecurity) {
        this.acceptTransportSecurity = acceptTransportSecurity;
    }

    public boolean supportsNotifications() {
        return supportsNotifications;
    }

    public void setSupportsNotifications(final boolean supportsNotifications) {
        this.supportsNotifications = supportsNotifications;
    }
}
