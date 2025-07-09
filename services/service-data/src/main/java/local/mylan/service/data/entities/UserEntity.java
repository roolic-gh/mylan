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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

@NamedQuery(name = Queries.GET_ALL_USERS, resultClass = UserEntity.class,
    query = "SELECT u FROM UserEntity u")
@NamedQuery(name = Queries.GET_ACTIVE_USERS,resultClass = UserEntity.class,
    query = "SELECT u FROM UserEntity u WHERE u.disabled=false")
@NamedQuery(name = Queries.GET_ACTIVE_ADMINS, resultClass = UserEntity.class,
    query = "SELECT u FROM UserEntity u WHERE u.disabled = false AND u.isAdmin = true")
@NamedQuery(name = Queries.UPDATE_USER_STATUS,
    query = "UPDATE UserEntity u SET u.disabled = :disabled WHERE u.userId = :userId")

@Entity
@Table(name = "user_main")
public class UserEntity {
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Integer userId;

    @Column(name = "username", unique = true, length = 32)
    private String username;

    @Column(name = "display_name", length = 64)
    private String displayName;

    @Column(name = "admin")
    private boolean isAdmin;

    @Column(name = "disabled")
    private boolean disabled;

    public UserEntity() {
        // default
    }

    public UserEntity(final String username, final boolean isAdmin) {
        this.username = username;
        displayName = username;
        this.isAdmin = isAdmin;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(final boolean admin) {
        isAdmin = admin;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(final boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserEntity that)) {
            return false;
        }
        return isAdmin == that.isAdmin && disabled == that.disabled && Objects.equal(userId,
            that.userId) && Objects.equal(username,
            that.username) && Objects.equal(displayName, that.displayName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId, username, displayName, isAdmin, disabled);
    }
}
