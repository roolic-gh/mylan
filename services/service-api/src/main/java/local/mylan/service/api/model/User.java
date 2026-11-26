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
package local.mylan.service.api.model;

import local.mylan.common.api.ServiceUser;

public class User implements ServiceUser {
    private Integer userId;
    private String username;
    private String displayName;
    private boolean isAdmin;
    private boolean disabled;

    public User() {
        // default
    }

    public User(final Integer userId, final String username, final String displayName, final boolean isAdmin) {
        this(username, displayName, isAdmin);
        this.userId = userId;
    }

    public User(final String username, final String displayName, final boolean isAdmin) {
        this.username = username;
        this.displayName = displayName;
        this.isAdmin = isAdmin;
    }

    @Override
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(final Integer userId) {
        this.userId = userId;
    }

    @Override
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

    @Override
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
}
