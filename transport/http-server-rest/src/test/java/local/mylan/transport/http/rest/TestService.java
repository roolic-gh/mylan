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
package local.mylan.transport.http.rest;

import java.util.List;
import local.mylan.common.annotations.rest.PathParameter;
import local.mylan.common.annotations.rest.QueryParameter;
import local.mylan.common.annotations.rest.RequestBody;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.common.annotations.rest.ServiceDescriptor;
import local.mylan.service.api.UserContext;

@ServiceDescriptor(id = "test", description = "test description")
interface TestService {

    @RequestMapping(method = "GET", path = "/data", description = "List items")
    List<TestPojo> getData(@QueryParameter(name = "limit") int limit, @QueryParameter(name = "offset") int offset);

    @RequestMapping(method = "GET", path = "/data/{id}", description = "Get item by id")
    TestPojo getData(@PathParameter("id") String id);

    @RequestMapping(method = "GET", path = "/data/by-user", description = "Get data item by id")
    TestPojo getData(UserContext userContext);

    @RequestMapping(method = "POST", path = "/data/{id}", description = "Add data item")
    TestPojo insertData(@RequestBody TestPojo data);

    @RequestMapping(method = "PATCH", path = "/data/{id}", description = "Update item")
    TestPojo updateData(@PathParameter("id") String id, @RequestBody TestPojo data);

    @RequestMapping(method = "DELETE", path = "/data/{id}", description = "Delete item")
    void deleteData(@PathParameter("id") String id);
}
