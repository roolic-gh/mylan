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

import com.google.common.base.Objects;
import java.util.List;
import local.mylan.common.annotations.rest.PathParameter;
import local.mylan.common.annotations.rest.QueryParameter;
import local.mylan.common.annotations.rest.RequestBody;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.common.annotations.rest.ServiceDescriptor;
import local.mylan.service.api.UserContext;

@ServiceDescriptor(id = "test", description = "test description")
public interface TestRestService {

    @RequestMapping(method = "GET", path = "/file/{path:.+}", description = "List items")
    List<Data> getFile(@PathParameter("path") String filePath);

    @RequestMapping(method = "GET", path = "/data", description = "List items")
    List<Data> getData(@QueryParameter int limit, @QueryParameter int offset);

    @RequestMapping(method = "GET", path = "/data/{id}", description = "Get item by id")
    Data getData(@PathParameter("id") String id);

    @RequestMapping(method = "GET", path = "/data/by-user", description = "Get data item by id")
    Data getData(UserContext userContext);

    @RequestMapping(method = "POST", path = "/data/{id}", description = "Add data item")
    Data insertData(@RequestBody Data data);

    @RequestMapping(method = "PATCH", path = "/data/{id}", description = "Update item")
    Data updateData(@PathParameter("id") String id, @RequestBody Data data);

    @RequestMapping(method = "DELETE", path = "/data/{id}", description = "Delete item")
    void deleteData(@PathParameter("id") String id);

    final class Data {

        public Data() {
            // default;
        }

        public Data(final String id, final String name, final long timestamp) {
            this.id = id;
            this.name = name;
            this.timestamp = timestamp;
        }

        private String id;
        private String name;
        private long timestamp;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(final long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof final Data data)) {
                return false;
            }
            return timestamp == data.timestamp && Objects.equal(id,
                data.id) && Objects.equal(name, data.name);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(id, name, timestamp);
        }
    }
}
