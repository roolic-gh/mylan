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
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import local.mylan.service.api.model.ShareType;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NamedQuery(name = Queries.GET_ALL_SHARED_RESOURCES, resultClass = NavResourceShareEntity.class,
    query = "SELECT s from NavResourceShareEntity s")
@NamedQuery(name = Queries.GET_LOCAL_SHARED_RESOURCES, resultClass = NavResourceShareEntity.class,
    query = "SELECT s from NavResourceShareEntity s WHERE s.account.device.protocol = DeviceProtocol.LOCAL")
@NamedQuery(name = Queries.GET_SHARED_RESOURCES_FOR_GUEST, resultClass = NavResourceShareEntity.class,
    query = "SELECT s from NavResourceShareEntity s WHERE s.shareType = ShareType.ALL")
@NamedQuery(name = Queries.GET_SHARED_RESOURCES_FOR_USER, resultClass = NavResourceShareEntity.class,
    query = "SELECT s from NavResourceShareEntity s WHERE s.shareType IN (ShareType.ALL, ShareType.REGISTERED) " +
            "OR s.userId = :userId")

@Entity
@Table(name = "nav_shares")
public class NavResourceShareEntity {

    @Id
    @Column(name = "share_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long shareId;

    @Column(name = "account_id", insertable = false, updatable = false)
    private Integer accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DeviceAccountEntity account;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "path", nullable = false)
    private String path;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "share_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ShareType shareType;

    public Long getShareId() {
        return shareId;
    }

    public void setShareId(final Long shareId) {
        this.shareId = shareId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(final Integer accountId) {
        this.accountId = accountId;
    }

    public DeviceAccountEntity getAccount() {
        return account;
    }

    public void setAccount(final DeviceAccountEntity account) {
        this.account = account;
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

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public ShareType getShareType() {
        return shareType;
    }

    public void setShareType(final ShareType shareType) {
        this.shareType = shareType;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NavResourceShareEntity that)) {
            return false;
        }
        return Objects.equal(shareId, that.shareId) && Objects.equal(accountId, that.accountId)
               && Objects.equal(userId, that.userId) && Objects.equal(path, that.path)
               && Objects.equal(displayName, that.displayName) && shareType == that.shareType;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shareId, accountId, userId, path, displayName, shareType);
    }
}
