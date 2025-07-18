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

public interface Queries {
    String GET_USER_BY_CREDENTIALS = "get-user-by-credentials";
    String GET_USER_MUST_CHANGE_PASSWORD = "get-user-must-change-password";
    String GET_ALL_USERS = "get-all-users";
    String GET_ACTIVE_USERS = "get-active-users";
    String GET_ACTIVE_ADMINS_COUNT = "get-active-admins-count";
    String RESET_USER_PASSWORD = "reset-user-password";
    String UPDATE_USER_PASSWORD = "update-user-password";
    String UPDATE_USER_STATUS = "update-user-status";
}
