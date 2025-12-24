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

import local.transport.netty.smb.protocol.smb2.Smb2ShareType;

/**
 * Tree Connect Details. Addresses MS-SMB2 (#3.2.1.4 Per Tree Connect & #3.2.1.10 Per Share).
 */
public class TreeConnectDetails {

    private Integer treeConnectId;
    private String shareName;
    private String sharePath;

    private Session session;
    private boolean dfsShare;

    // SMB 3.0 +
    private boolean caShare;
    private boolean encryptData;
    private boolean scaleoutShare;

    // SMB 3.1.1 +
    private boolean compressData;

    // non-spec
    private Smb2ShareType shareType;

    public Integer treeConnectId() {
        return treeConnectId;
    }

    public void setTreeConnectId(final Integer treeConnectId) {
        this.treeConnectId = treeConnectId;
    }

    public String shareName() {
        return shareName;
    }

    public void setShareName(final String shareName) {
        this.shareName = shareName;
    }

    public String sharePath() {
        return sharePath;
    }

    public void setSharePath(final String sharePath) {
        this.sharePath = sharePath;
    }

    public Smb2ShareType shareType() {
        return shareType;
    }

    public void setShareType(final Smb2ShareType shareType) {
        this.shareType = shareType;
    }

    public Session session() {
        return session;
    }

    public void setSession(final Session session) {
        this.session = session;
    }

    public boolean dfsShare() {
        return dfsShare;
    }

    public void setDfsShare(final boolean dfsShare) {
        this.dfsShare = dfsShare;
    }

    public boolean caShare() {
        return caShare;
    }

    public void setCaShare(final boolean caShare) {
        this.caShare = caShare;
    }

    public boolean encryptData() {
        return encryptData;
    }

    public void setEncryptData(final boolean encryptData) {
        this.encryptData = encryptData;
    }

    public boolean scaleoutShare() {
        return scaleoutShare;
    }

    public void setScaleoutShare(final boolean scaleoutShare) {
        this.scaleoutShare = scaleoutShare;
    }

    public boolean compressData() {
        return compressData;
    }

    public void setCompressData(final boolean compressData) {
        this.compressData = compressData;
    }
}
