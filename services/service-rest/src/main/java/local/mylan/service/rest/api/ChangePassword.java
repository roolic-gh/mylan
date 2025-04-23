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
package local.mylan.service.rest.api;

import com.google.common.base.Objects;

public class ChangePassword {
    private Integer userId;
    private String oldPassword;
    private String newPassword;

    public ChangePassword() {
        // default;
    }

    public ChangePassword(final Integer userId, final String oldPassword, final String newPassword) {
        this.userId = userId;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(final String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(final String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final ChangePassword that)) {
            return false;
        }
        return Objects.equal(userId, that.userId) && Objects.equal(
            oldPassword, that.oldPassword) && Objects.equal(newPassword, that.newPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(userId, oldPassword, newPassword);
    }
}
