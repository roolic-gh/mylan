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

/**
 * Application Open of a File Details. Addresses MS-SMB2 (#3.2.1.6 Per Application Open of a File).
 */
public class OpenDetails {
    Object fileId;
    TreeConnect treeConnect;
    Connection connection;
    Session session;
    Object oplockLevel;
    boolean durable;
    String fileName;
    boolean resilientHandle;
    Long lastDisconnectTime;
    Long resilientTimeout;
    Object[] operationBuckets;
    Object desiredAccess;
    Object shareMode;
    Object createOptions;
    Object fileAttributes;
    Object createDisposition;

    // SMB 3.0 +
    Long durableTimeout;
    Map<Object, PendingRequestDetails> outstandingRequests;
    UUID createGuid;
    boolean persistent;
}
