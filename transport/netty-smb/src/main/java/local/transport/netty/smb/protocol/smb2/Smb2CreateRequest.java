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
import local.transport.netty.smb.protocol.Smb2Command;
import local.transport.netty.smb.protocol.Smb2Header;
import local.transport.netty.smb.protocol.Smb2Request;
import local.transport.netty.smb.protocol.fscc.FileAttributeFlags;

/**
 * Addresses MS-SMB2 (#2.2.13 SMB2 CREATE Request).
 */
public final class Smb2CreateRequest extends Smb2Request {

    private String name;
    private Smb2OpLockLevel opLockLevel;
    private Smb2ImpersonationLevel impersonationLevel;
    private Flags<Smb2AccessMask> desiredAccess;
    private Flags<FileAttributeFlags> fileAttributes;
    private Flags<Smb2ShareAccessFlags> shareAccess;
    private Smb2CreateDisposition createDisposition;
    private Flags<Smb2CreateOptionsFlags> createOptions;

    public Smb2CreateRequest() {
        // default
    }

    public Smb2CreateRequest(final Smb2Header header) {
        super(header);
    }

    @Override
    protected Smb2Command command() {
        return Smb2Command.SMB2_CREATE;
    }

    public String name() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public Smb2OpLockLevel opLockLevel() {
        return opLockLevel;
    }

    public void setOpLockLevel(final Smb2OpLockLevel opLockLevel) {
        this.opLockLevel = opLockLevel;
    }

    public Smb2ImpersonationLevel impersonationLevel() {
        return impersonationLevel;
    }

    public void setImpersonationLevel(final Smb2ImpersonationLevel impersonationLevel) {
        this.impersonationLevel = impersonationLevel;
    }

    public Flags<Smb2AccessMask> desiredAccess() {
        return desiredAccess;
    }

    public void setDesiredAccess(
        final Flags<Smb2AccessMask> desiredAccess) {
        this.desiredAccess = desiredAccess;
    }

    public Flags<FileAttributeFlags> fileAttributes() {
        return fileAttributes;
    }

    public void setFileAttributes(
        final Flags<FileAttributeFlags> fileAttributes) {
        this.fileAttributes = fileAttributes;
    }

    public Flags<Smb2ShareAccessFlags> shareAccess() {
        return shareAccess;
    }

    public void setShareAccess(
        final Flags<Smb2ShareAccessFlags> shareAccess) {
        this.shareAccess = shareAccess;
    }

    public Smb2CreateDisposition createDisposition() {
        return createDisposition;
    }

    public void setCreateDisposition(final Smb2CreateDisposition createDisposition) {
        this.createDisposition = createDisposition;
    }

    public Flags<Smb2CreateOptionsFlags> createOptions() {
        return createOptions;
    }

    public void setCreateOptions(
        final Flags<Smb2CreateOptionsFlags> createOptions) {
        this.createOptions = createOptions;
    }
}
