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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import local.transport.netty.smb.protocol.SmbDialect;

/**
 * Client Details. Addresses MS_SMB2 (#3.2.1.1 Global).
 */
public class ClientDetails extends GlobalDetails {

    private final Map<Integer, Connection> connections = new ConcurrentHashMap<>();
    private boolean rejectGuestAccess;
    private boolean allowInsecureGuestAccess;

    // SMB 2.1 +
    private final Map<Object, FileDetails> globalFiles = new ConcurrentHashMap<>();
    private final UUID clientGuid = UUID.randomUUID();
    // SMB 3.x +
    private SmbDialect maxDialect;
    boolean requireSecureNegotiate;
    private final Map<Object, ServerDetails> servers = new ConcurrentHashMap<>();
    private final Map<Object, ShareDetails> shares = new ConcurrentHashMap<>();
    // SMB 3.1.1 +
    boolean compressAllRequests;
    boolean mutualAuthOverQUICSupported;
    final Map<Object, Object> clientCertificateMapping = new ConcurrentHashMap<>();
    // non-spec
    private SmbDialect minDialect;

    public UUID clientGuid() {
        return clientGuid;
    }

    public Map<Integer, Connection> connections() {
        return connections;
    }

    public Map<Object, ServerDetails> servers() {
        return servers;
    }

    public Map<Object, ShareDetails> shares() {
        return shares;
    }

    public Map<Object, FileDetails> globalFiles() {
        return globalFiles;
    }

    public Map<Object, Object> clientCertificateMapping() {
        return clientCertificateMapping;
    }


    public boolean isRejectGuestAccess() {
        return rejectGuestAccess;
    }

    public void setRejectGuestAccess(final boolean rejectGuestAccess) {
        this.rejectGuestAccess = rejectGuestAccess;
    }

    public boolean isAllowInsecureGuestAccess() {
        return allowInsecureGuestAccess;
    }

    public void setAllowInsecureGuestAccess(final boolean allowInsecureGuestAccess) {
        this.allowInsecureGuestAccess = allowInsecureGuestAccess;
    }

    public SmbDialect minDialect() {
        return minDialect;
    }

    public void setMinDialect(final SmbDialect minDialect) {
        this.minDialect = minDialect;
    }

    public SmbDialect maxDialect() {
        return maxDialect;
    }

    public void setMaxDialect(final SmbDialect maxDialect) {
        this.maxDialect = maxDialect;
    }

    public boolean isRequireSecureNegotiate() {
        return requireSecureNegotiate;
    }

    public void setRequireSecureNegotiate(final boolean requireSecureNegotiate) {
        this.requireSecureNegotiate = requireSecureNegotiate;
    }

    public boolean isCompressAllRequests() {
        return compressAllRequests;
    }

    public void setCompressAllRequests(final boolean compressAllRequests) {
        this.compressAllRequests = compressAllRequests;
    }

    public boolean isMutualAuthOverQUICSupported() {
        return mutualAuthOverQUICSupported;
    }

    public void setMutualAuthOverQUICSupported(final boolean mutualAuthOverQUICSupported) {
        this.mutualAuthOverQUICSupported = mutualAuthOverQUICSupported;
    }
}
