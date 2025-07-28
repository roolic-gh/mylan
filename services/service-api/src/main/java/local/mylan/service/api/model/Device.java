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
package local.mylan.service.api.model;

import java.util.List;

public class Device {

    private Integer deviceId;
    private String identifier;
    private Integer userId;
    private String displayName;
    private List<DeviceIpAddress> ipAddresses;
    private DeviceProtocol protocol;
    private boolean keyLocked;
    private String rootPath;

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(final Integer deviceId) {
        this.deviceId = deviceId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public List<DeviceIpAddress> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(final List<DeviceIpAddress> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    public DeviceProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(final DeviceProtocol protocol) {
        this.protocol = protocol;
    }

    public boolean isKeyLocked() {
        return keyLocked;
    }

    public void setKeyLocked(final boolean keyLocked) {
        this.keyLocked = keyLocked;
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(final String rootPath) {
        this.rootPath = rootPath;
    }
}
