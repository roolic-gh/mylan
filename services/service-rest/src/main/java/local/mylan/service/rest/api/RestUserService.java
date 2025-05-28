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

import java.util.List;
import local.mylan.common.annotations.rest.PathParameter;
import local.mylan.common.annotations.rest.RequestBody;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.common.annotations.rest.ServiceDescriptor;
import local.mylan.service.api.UserContext;
import local.mylan.service.api.UserService;
import local.mylan.service.api.model.User;
import local.mylan.service.api.model.UserCredentials;
import local.mylan.service.rest.spi.DefaultRestUserService;

@ServiceDescriptor(id = "UserService", description = "Operations related to registered users")
public interface RestUserService {

    static RestUserService defaultInstance(final UserService service){
        return new DefaultRestUserService(service);
    }

    UserContext authenticate(String authHeader);

    @RequestMapping(method = "POST", path = "/authenticate", description = "Authenticate user")
    UserAuthResult authenticate(@RequestBody UserCredentials credentials);

    @RequestMapping(method = "POST", path = "/user/change-password", description = "Change own password")
    void changePassword(@RequestBody ChangePassword passwordChange, UserContext userCtx);

    @RequestMapping(method = "POST", path = "/user/{id}/reset-password", description = "Reset user password")
    void resetPassword(@PathParameter("id") Integer userId, UserContext userCtx);

    @RequestMapping(method = "POST", path = "/user/create", description = "Create user")
    User createUser(@RequestBody User newUser, UserContext userCtx);

    @RequestMapping(method = "PATCH", path = "/user/update", description = "Update user")
    User updateUser(@RequestBody User user, UserContext userCtx);

    @RequestMapping(method = "DELETE", path = "/user/{id}", description = "Delete user")
    void deleteUser(@PathParameter("id") Integer userId, UserContext userCtx);

    @RequestMapping(method = "GET", path = "/user/{id}", description = "Get user by ID")
    User getUser(@PathParameter("id") Integer userId, UserContext userCtx);

    @RequestMapping(method = "GET", path = "/user/list", description = "Get user list")
    List<User> getUserList(UserContext userCtx);

    @RequestMapping(method = "GET", path = "/user", description = "Get current user by auth token")
    User getCurrentUser(UserContext userCtx);
}
