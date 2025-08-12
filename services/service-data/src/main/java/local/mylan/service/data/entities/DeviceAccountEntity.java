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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NamedQuery(name = Queries.GET_ALL_ACCOUNTS, resultClass = DeviceAccountEntity.class,
    query = "SELECT a FROM DeviceAccountEntity a")
@NamedQuery(name = Queries.GET_USER_ACCOUNTS, resultClass = DeviceAccountEntity.class,
    query = "SELECT a FROM DeviceAccountEntity a WHERE a.userId = :userId")
@NamedQuery(name = Queries.GET_LOCAL_ACCOUNT, resultClass = DeviceAccountEntity.class,
    query = "SELECT a FROM DeviceAccountEntity a WHERE a.device.protocol = DeviceProtocol.LOCAL")

@Entity
@Table(name = "device_accounts", uniqueConstraints = {@UniqueConstraint(columnNames = {"device_id", "username"})})
public class DeviceAccountEntity {

    @Id
    @Column(name = "account_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    Integer accountId;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "device_id", insertable = false, updatable = false)
    private Integer deviceId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "device_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DeviceEntity device;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "crypt_key")
    private String key;

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(final Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(final UserEntity user) {
        this.user = user;
    }

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
        if (!(o instanceof final DeviceAccountEntity that)) {
            return false;
        }
        return Objects.equal(accountId, that.accountId) && Objects.equal(userId, that.userId)
            && Objects.equal(deviceId, that.deviceId) && Objects.equal(username, that.username)
            && Objects.equal(password, that.password) && Objects.equal(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(accountId, userId, deviceId, username, password, key);
    }
}
