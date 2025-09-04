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

/**
 * Client Session Details. Addresses MS-SMB2 (#3.2.1.3 Per Session).
 */
public class SessionDetails {
    Long sessionId;
    Map<String, TreeConnect> treeConnects;
    byte[] sessionKey;
    boolean signingRequired;
    Connection connection;
    String userCredentials;
    Map<Long, OpenDetails> opens;
    boolean anonymous;
    boolean geurst;

    //SMB 3.x +
    List<ChannelDetails> channels;
    String channelSequence;
    boolean encryptData;
    byte[] encryptionKey;
    byte[] decryptionKey;
    byte[] signingKey;
    byte[] applicationKey;

    // SMB 3.1.1 +
    byte[] preauthIntegrityHashValue;
    byte[] fullSessionKey;
}
