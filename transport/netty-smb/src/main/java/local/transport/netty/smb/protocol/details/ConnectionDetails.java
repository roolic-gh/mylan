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
import java.util.UUID;
import local.transport.netty.smb.protocol.SmbDialect;

/**
 * SMB2 Transport connection details. Addresses MS_SMB2 (3.2.1.2 Per SMB2 Transport Connection).
 */
public class ConnectionDetails {

    private final int connectionId;

    Map<Long, Session> sessions;
    Map<Long, Session> preauthSessions; // A table of sessions that have not completed authentication,
    Map<Object, PendingRequestDetails> outstandingRequests; // a table of requests, as specified in section 3.2.1.7, that have
    //been issued on this connection and are awaiting a response. The table MUST allow lookup by
    //Request.CancelId and by MessageId, and each request MUST store the time at which the
    //request was sent.

    Map<Object, Object> sequenceWindow; // A table of available sequence numbers for sending requests to the server
    byte[] gssNegotiateToken;
    int maxTransactSize;
    int maxReadSize;
    int maxWriteSize;
    UUID serverGuid;
    boolean requireSigning;
    String serverName;

    // SMB 2.1 +
    SmbDialect dialect;
    boolean supportsFileLeasing;
    boolean supportsMultiCredit;
    private final UUID clientGuid;

    // SMB 3.0 +
    boolean supportsDirectoryLeasing;
    boolean supportsMultiChannel;
    boolean supportsPersistentHandles;
    boolean supportsEncryption;
    Object clientCapabilities;
    Object ServerCapabilities;
    Object clientSecurityMode;
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
}
