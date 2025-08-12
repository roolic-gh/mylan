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
    private DeviceProtocol protocol;
    private List<DeviceIpAddress> ipAddresses;

    public Device() {
        // default
    }

    public Device(final String identifier, final DeviceProtocol protocol) {
        this.identifier = identifier;
        this.protocol = protocol;
    }

    public Device(final String identifier, final DeviceProtocol protocol, final List<DeviceIpAddress> ipAddresses) {
        this.identifier = identifier;
        this.protocol = protocol;
        this.ipAddresses = List.copyOf(ipAddresses);
    }

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

    public DeviceProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(final DeviceProtocol protocol) {
        this.protocol = protocol;
    }

    public List<DeviceIpAddress> getIpAddresses() {
        return ipAddresses == null ? null : List.copyOf(ipAddresses);
    }

    public void setIpAddresses(final List<DeviceIpAddress> ipAddresses) {
        this.ipAddresses = ipAddresses == null ? null : List.copyOf(ipAddresses);
    }
}
