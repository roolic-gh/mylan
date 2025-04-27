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
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@NamedQuery(name = Queries.GET_USER_BY_CREDENTIALS,
    query = "SELECT uc.user FROM UserCredEntity uc WHERE uc.user.username = :username " +
            "AND uc.password = :password AND uc.user.deleted = false",
    resultClass = UserEntity.class)

@Entity
@Table(name = "user_cred")
public class UserCredEntity {

    @Id
    @MapsId
    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "password")
    private String password;

    @Column(name = "must_change")
    private boolean mustChange;

    public UserCredEntity() {
        // default
    }

    public UserCredEntity(final String password, final UserEntity user, boolean mustChange) {
        this.password = password;
        this.user = user;
        this.mustChange = mustChange;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(final UserEntity user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public boolean isMustChange() {
        return mustChange;
    }

    public void setMustChange(final boolean mustChange) {
        this.mustChange = mustChange;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final UserCredEntity that)) {
            return false;
        }
        return mustChange == that.mustChange && Objects.equal(user,
            that.user) && Objects.equal(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(user, password, mustChange);
    }
}
