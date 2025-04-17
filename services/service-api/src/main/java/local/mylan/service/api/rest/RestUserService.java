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
package local.mylan.service.api.rest;

import java.util.List;
import local.mylan.common.annotations.rest.PathParameter;
import local.mylan.common.annotations.rest.RequestBody;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.common.annotations.rest.ServiceDescriptor;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.model.User;
import local.mylan.service.api.rest.user.ChangePasswordRequest;
import local.mylan.service.api.rest.user.UserAuthRequest;
import local.mylan.service.api.rest.user.UserAuthResponse;

@ServiceDescriptor(id = "UserService", description = "Operations related to registered users")
public interface RestUserService {

    @RequestMapping(method = "POST", path = "/authenticate", description = "Authenticate user")
    UserAuthResponse authenticate(@RequestBody UserAuthRequest auth);

    @RequestMapping(method = "POST", path = "/user/change-password", description = "Change user password")
    void changePassword(@RequestBody ChangePasswordRequest passwordChange, UserContext userCtx);

    @RequestMapping(method = "POST", path = "/user/create", description = "Create user")
    User createUser(@RequestBody User newUser, UserContext userCtx);

    @RequestMapping(method = "PATCH", path = "/user/{id}", description = "Update user")
    User updateUser(@RequestBody User user, @PathParameter("id") String userId, UserContext userCtx);

    @RequestMapping(method = "DELETE", path = "/user/{id}", description = "Delete user")
    void deleteUser(@PathParameter("id") String userId, UserContext userCtx);

    @RequestMapping(method = "GET", path = "/user/{id}", description = "Get user by ID")
    User getUser(@PathParameter("id") String userId, UserContext userCtx);

    @RequestMapping(method = "GET", path = "/user/list", description = "Get user list")
    List<User> getUserList(@PathParameter("id") String userId, UserContext userCtx);
}
