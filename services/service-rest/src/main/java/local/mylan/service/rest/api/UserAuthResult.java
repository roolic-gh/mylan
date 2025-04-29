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
import local.mylan.service.api.model.User;

public final class UserAuthResult {
    private User user;
    private String authToken;
    private boolean mustChangePassword;

    public UserAuthResult() {
        // default
    }

    public UserAuthResult(final User user, final String authToken, final boolean mustChangePassword) {
        this.user = user;
        this.authToken = authToken;
        this.mustChangePassword = mustChangePassword;
    }

    public User getUser() {
        return user;
    }

    public void setUser(final User user) {
        this.user = user;
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(final String authToken) {
        this.authToken = authToken;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(final boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof final UserAuthResult that)) {
            return false;
        }
        return mustChangePassword == that.mustChangePassword && Objects.equal(user,
            that.user) && Objects.equal(authToken, that.authToken);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(user, authToken, mustChangePassword);
    }
}
