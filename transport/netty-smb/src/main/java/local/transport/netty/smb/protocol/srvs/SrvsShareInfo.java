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
 * Share info entry aggregate object holdaing data for all the levels.
 * Addressing MS-SRVS ## 2.2.4.22 SHARE_INFO_0 .. 2.2.4.27 SHARE_INFO_503_I.
 */
public final class SrvsShareInfo {
    private String netName;
    private SrvsShareType type;
    private String remark;
    private int maxUses;
    private int currentUses;
    private String path;
    private String serverName;
    private String securityDescriptor;

    public String netName() {
        return netName;
    }

    public void setNetName(final String netName) {
        this.netName = netName;
    }

    public SrvsShareType type() {
        return type;
    }

    public void setType(final SrvsShareType type) {
        this.type = type;
    }

    public String remark() {
        return remark;
    }

    public void setRemark(final String remark) {
        this.remark = remark;
    }

    public int maxUses() {
        return maxUses;
    }

    public void setMaxUses(final int maxUses) {
        this.maxUses = maxUses;
    }

    public int currentUses() {
        return currentUses;
    }

    public void setCurrentUses(final int currentUses) {
        this.currentUses = currentUses;
    }

    public String path() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String serverName() {
        return serverName;
    }

    public void setServerName(final String serverName) {
        this.serverName = serverName;
    }

    public String securityDescriptor() {
        return securityDescriptor;
    }

    public void setSecurityDescriptor(final String securityDescriptor) {
        this.securityDescriptor = securityDescriptor;
    }
}
