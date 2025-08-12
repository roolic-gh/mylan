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
package local.mylan.service.data.entities;

import com.google.common.base.Objects;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import local.mylan.service.api.model.DeviceProtocol;

@NamedQuery(name = Queries.GET_ALL_DEVICES, resultClass = DeviceEntity.class, query = "SELECT d FROM DeviceEntity d")
@NamedQuery(name = Queries.GET_LOCAL_DEVICE, resultClass = DeviceEntity.class,
    query = "SELECT d FROM DeviceEntity d WHERE d.protocol = DeviceProtocol.LOCAL")

@Entity
@Table(name = "devices")
public class DeviceEntity {

    @Id
    @Column(name = "device_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer deviceId;

    @Column(name = "identifier", unique = true)
    private String identifier;

    @Column(name = "protocol")
    @Enumerated(EnumType.STRING)
    private DeviceProtocol protocol;

    @OneToMany(mappedBy = "device", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<DeviceIpAddressEntity> ipAddresses;

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

    public List<DeviceIpAddressEntity> getIpAddresses() {
        return ipAddresses;
    }

    public void setIpAddresses(final List<DeviceIpAddressEntity> ipAddresses) {
        this.ipAddresses = ipAddresses;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeviceEntity that)) {
            return false;
        }
        return Objects.equal(deviceId, that.deviceId) && Objects.equal(identifier, that.identifier)
            && protocol == that.protocol && Objects.equal(ipAddresses, that.ipAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, identifier, protocol, ipAddresses);
    }
}
