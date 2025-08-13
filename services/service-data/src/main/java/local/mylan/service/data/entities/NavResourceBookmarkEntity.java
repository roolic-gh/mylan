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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@NamedQuery(name = Queries.GET_USER_BOOKMARKS, resultClass = NavResourceBookmarkEntity.class,
query = "SELECT b FROM NavResourceBookmarkEntity b WHERE b.userId = :userId")

@Entity
@Table(name = "nav_bookmarks")
public class NavResourceBookmarkEntity {

    @Id
    @Column(name = "bookmark_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long bookmarkId;

    @Column(name = "account_id", insertable = false, updatable = false)
    private Integer accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private DeviceAccountEntity account;

    @Column(name = "share_id", insertable = false, updatable = false)
    private Long shareId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "share_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private NavResourceShareEntity share;

    @Column(name = "user_id", insertable = false, updatable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "path")
    private String path;

    public Long getBookmarkId() {
        return bookmarkId;
    }

    public void setBookmarkId(final Long bookmarkId) {
        this.bookmarkId = bookmarkId;
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

    public Long getShareId() {
        return shareId;
    }

    public void setShareId(final Long shareId) {
        this.shareId = shareId;
    }

    public NavResourceShareEntity getShare() {
        return share;
    }

    public void setShare(final NavResourceShareEntity share) {
        this.share = share;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NavResourceBookmarkEntity that)) {
            return false;
        }
        return Objects.equal(bookmarkId, that.bookmarkId) && Objects.equal(accountId, that.accountId)
               && Objects.equal(shareId, that.shareId) && Objects.equal(userId, that.userId)
               && Objects.equal(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bookmarkId, accountId, shareId, userId, path);
    }
}
