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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_cred")
public class UserCredEntity {

    @Id
    @Column(name = "user_id")
    private int user_id;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name="password")
    private String password;

    @Column(name="must_change")
    private boolean mustChange;

    public UserCredEntity(){
        // default
    }

    public UserCredEntity(final String password, final UserEntity user) {
        this.password = password;
        this.user = user;
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

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(final int user_id) {
        this.user_id = user_id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final UserCredEntity that)) {
            return false;
        }
        return user_id == that.user_id && mustChange == that.mustChange && Objects.equal(password,
            that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(user_id, password, mustChange);
    }
}
