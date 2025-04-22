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
package local.mylan.service.rest.api.user;

import com.google.common.base.Objects;

public class ChangePasswordRequest {
    private String username;
    private String oldPassword;
    private String newPassword;

    public ChangePasswordRequest() {
        // default;
    }

    public ChangePasswordRequest(final String username, final String oldPassword, final String newPassword) {
        this.username = username;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
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
        if (!(o instanceof final ChangePasswordRequest that)) {
            return false;
        }
        return Objects.equal(username, that.username) && Objects.equal(
            oldPassword, that.oldPassword) && Objects.equal(newPassword, that.newPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username, oldPassword, newPassword);
    }
}
