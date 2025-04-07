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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import java.util.List;
import local.mylan.transport.http.api.UserContext;

@Tag(name = "test", description = "test")
@Path("/test")
public interface TestRestService {

    @Operation(description = "List items", responses = {@ApiResponse(responseCode = "200")})
    @GET
    @Path("/data")
    List<Data> getData(
        @Parameter(in = ParameterIn.QUERY, name = "limit") int limit,
        @Parameter(in = ParameterIn.QUERY, name = "offset") int offset);

    @Operation(description = "Get item by id", responses = {@ApiResponse(responseCode = "200")})
    @GET
    @Path("/data/{id}")
    Data getData(@Parameter(in = ParameterIn.PATH, name = "id") String id);

    @Operation(description = "Get data item by id", responses = {@ApiResponse(responseCode = "200")})
    @GET
    @Path("/data/by-user")
    Data getData(@Parameter(hidden = true) UserContext userContext);

    @Operation(description = "Add data item", responses = {@ApiResponse(responseCode = "201")})
    @POST
    @Path("/data/{id}")
    Data insertData(@RequestBody Data data);

    @Operation(description = "Update item", responses = {@ApiResponse(responseCode = "200")})
    @PATCH
    @Path("/data/{id}")
    Data updateData(@Parameter(in = ParameterIn.PATH, name = "id") String id, @RequestBody Data data);

    @Operation(description = "add item", responses = {@ApiResponse(responseCode = "201")})
    @DELETE
    @Path("/data/{id}")
    void deleteData(@RequestBody Data data);

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
