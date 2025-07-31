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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NamedQuery(name = Queries.UPDATE_DEVICE_CREDENTIALS,
    query = "UPDATE DeviceCredEntity dc SET dc.username = :username, dc.password = :password, dc.key = :key " +
            "WHERE dc.deviceId = :deviceId")

@Entity
@Table(name = "device_cred")
public class DeviceCredEntity {

    @Id
    @Column(name = "device_id", insertable = false, updatable = false)
    private Integer deviceId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "device_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DeviceEntity device;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "crypt_key")
    private String key;

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(final Integer deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceEntity getDevice() {
        return device;
    }

    public void setDevice(final DeviceEntity device) {
        this.device = device;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DeviceCredEntity that)) {
            return false;
        }
        return Objects.equal(deviceId, that.deviceId) && Objects.equal(
            username, that.username) && Objects.equal(password,
            that.password) && Objects.equal(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(deviceId, username, password, key);
    }
}
