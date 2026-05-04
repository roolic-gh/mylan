/*
 * Copyright 2026 Ruslan Kashapov
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
package local.transport.netty.smb.protocol.srvs;

/**
 * Addresses MS-SRVS #3.1.4.8 NetrShareEnum (Opnum 15).
 */
public final class SrvsNetrShareEnum implements SrvsMessage {
    public static final int OPNUM = 15;

    private String serverName;
    private SrvShareEnumStruct infoStruct;
    private int preferedMaximumLength = -1;
    private int totalEntries;
    private int resumeHandle;
    private SrvsError error;

    @Override
    public int opnum() {
        return OPNUM;
    }

    public String serverName() {
        return serverName;
    }

    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    public SrvShareEnumStruct infoStruct() {
        return infoStruct;
    }

    public void setInfoStruct(final SrvShareEnumStruct infoStruct) {
        this.infoStruct = infoStruct;
    }

    public int preferedMaximumLength() {
        return preferedMaximumLength;
    }

    public void setPreferedMaximumLength(final int preferedMaximumLength) {
        this.preferedMaximumLength = preferedMaximumLength;
    }

    public int totalEntries() {
        return totalEntries;
    }

    public void setTotalEntries(final int totalEntries) {
        this.totalEntries = totalEntries;
    }

    public int resumeHandle() {
        return resumeHandle;
    }

    public void setResumeHandle(final int resumeHandle) {
        this.resumeHandle = resumeHandle;
    }

    public SrvsError error() {
        return error;
    }

    public void setError(final SrvsError error) {
        this.error = error;
    }
}
