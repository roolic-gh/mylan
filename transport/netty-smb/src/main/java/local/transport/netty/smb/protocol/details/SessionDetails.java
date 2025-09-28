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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client Session Details. Addresses MS-SMB2 (#3.2.1.3 Per Session).
 */
public class SessionDetails {

    private final Map<String, TreeConnect> treeConnects = new ConcurrentHashMap<>();
    private final Map<Long, OpenDetails> opens = new ConcurrentHashMap<>();

    private Long sessionId;
    private byte[] sessionKey;
    private boolean signingRequired;
    private Connection connection;
    private UserCredentials userCredentials;
    private boolean anonymous;
    private boolean guest;

    //SMB 3.x +
    Set<ChannelDetails> channels;
    String channelSequence;
    boolean encryptData;
    byte[] encryptionKey;
    byte[] decryptionKey;
    byte[] signingKey;
    byte[] applicationKey;

    // SMB 3.1.1 +
    byte[] preauthIntegrityHashValue;
    byte[] fullSessionKey;

    public Long sessionId() {
        return sessionId;
    }

    public void setSessionId(final Long sessionId) {
        this.sessionId = sessionId;
    }

    public Map<String, TreeConnect> treeConnects() {
        return treeConnects;
    }

    public byte[] sessionKey() {
        return sessionKey;
    }

    public void setSessionKey(final byte[] sessionKey) {
        this.sessionKey = sessionKey;
    }

    public boolean signingRequired() {
        return signingRequired;
    }

    public void setSigningRequired(final boolean signingRequired) {
        this.signingRequired = signingRequired;
    }

    public Connection connection() {
        return connection;
    }

    public void setConnection(final Connection connection) {
        this.connection = connection;
    }

    public UserCredentials userCredentials() {
        return userCredentials;
    }

    public void setUserCredentials(final UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
    }

    public Map<Long, OpenDetails> opens() {
        return opens;
    }

    public boolean anonymous() {
        return anonymous;
    }

    public void setAnonymous(final boolean anonymous) {
        this.anonymous = anonymous;
    }

    public boolean guest() {
        return guest;
    }

    public void setGuest(final boolean guest) {
        this.guest = guest;
    }
}
