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
import local.transport.netty.smb.protocol.Flags;
import local.transport.netty.smb.protocol.fscc.FileAttributeFlags;
import local.transport.netty.smb.protocol.smb2.Smb2AccessMask;
import local.transport.netty.smb.protocol.smb2.Smb2CreateDisposition;
import local.transport.netty.smb.protocol.smb2.Smb2CreateOptionsFlags;
import local.transport.netty.smb.protocol.smb2.Smb2OpLockLevel;
import local.transport.netty.smb.protocol.smb2.Smb2ShareAccessFlags;

/**
 * Application Open of a File Details. Addresses MS-SMB2 (#3.2.1.6 Per Application Open of a File).
 */
public class OpenDetails {
    private UUID fileId;
    private TreeConnect treeConnect;
    private Connection connection;
    private Session session;
    private Smb2OpLockLevel opLockLevel;
    private boolean durable;
    private String fileName;
    private boolean resilientHandle;
    private long lastDisconnectTime;
    private long resilientTimeout;
    private Object[] operationBuckets;
    private Flags<Smb2AccessMask> desiredAccess;
    private Flags<Smb2ShareAccessFlags> shareAccess;
    Flags<Smb2CreateOptionsFlags> createOptions;
    Flags<FileAttributeFlags> fileAttributes;
    Smb2CreateDisposition createDisposition = Smb2CreateDisposition.FILE_OPEN;

    // SMB 3.0 +
    Long durableTimeout;
    Map<Object, PendingRequestDetails> outstandingRequests;
    UUID createGuid;
    boolean persistent;

    public UUID fileId() {
        return fileId;
    }

    public void setFileId(final UUID fileId) {
        this.fileId = fileId;
    }

    public TreeConnect treeConnect() {
        return treeConnect;
    }

    public void setTreeConnect(final TreeConnect treeConnect) {
        this.treeConnect = treeConnect;
    }

    public Smb2OpLockLevel opLockLevel() {
        return opLockLevel;
    }

    public void setOpLockLevel(final Smb2OpLockLevel opLockLevel) {
        this.opLockLevel = opLockLevel;
    }

    public String fileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }

    public Flags<Smb2CreateOptionsFlags> createOptions() {
        return createOptions;
    }

    public void setCreateOptions(final Flags<Smb2CreateOptionsFlags> createOptions) {
        this.createOptions = createOptions;
    }

    public Flags<FileAttributeFlags> fileAttributes() {
        return fileAttributes;
    }

    public void setFileAttributes(final Flags<FileAttributeFlags> fileAttributes) {
        this.fileAttributes = fileAttributes;
    }

    public Flags<Smb2AccessMask> desiredAccess() {
        return desiredAccess;
    }

    public void setDesiredAccess(final Flags<Smb2AccessMask> desiredAccess) {
        this.desiredAccess = desiredAccess;
    }

    public Flags<Smb2ShareAccessFlags> shareAccess() {
        return shareAccess;
    }

    public void setShareAccess(final Flags<Smb2ShareAccessFlags> shareAccess) {
        this.shareAccess = shareAccess;
    }

    public Smb2CreateDisposition createDisposition() {
        return createDisposition;
    }

    public void setCreateDisposition(final Smb2CreateDisposition createDisposition) {
        this.createDisposition = createDisposition;
    }
}
