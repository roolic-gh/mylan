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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import local.mylan.transport.http.ext.StaticContentDispatcher;
import tools.jackson.databind.json.JsonMapper;

public class SwaggerUiDispatcher extends StaticContentDispatcher {

    static final String RESOURCE_PATH = "/mylan/swagger-ui";
    static final String INITIALIZER_PATH = "/swagger-initializer.js";
    static final String API_PATH = "/api.js";
    static final String URL_TO_REPLACE = "https://petstore.swagger.io/v2/swagger.json";

    private final ContentSource apiContentSource;

    public SwaggerUiDispatcher(final String contextPath, final String restContextPath,
        final Class<?>... serviceClasses) {
        super(contextPath, RESOURCE_PATH);
        substitute(INITIALIZER_PATH, Map.of(URL_TO_REPLACE, contextPath + API_PATH));
        apiContentSource = buildApiContentSource(restContextPath, serviceClasses);
    }

    private static ContentSource buildApiContentSource(final String restContextPath, final Class<?>... serviceClasses) {
        final var openApi = new OpenApiBuilder(restContextPath).process(List.of(serviceClasses)).build();
        final var mapper = JsonMapper.builder()
            .addModule(new OpenApiJsonModule())
            .changeDefaultPropertyInclusion(
                incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL)
                    .withValueInclusion(JsonInclude.Include.NON_NULL)).build();
        final var bytes = mapper.writeValueAsString(openApi).getBytes(StandardCharsets.UTF_8);
        final var modified = System.currentTimeMillis();
        final var etag = Long.toHexString(modified);
        return new ContentSource(bytes.length, modified,
            HttpHeaderValues.APPLICATION_JSON.toString(), etag,
            null, () -> new ByteArrayInputStream(bytes));
    }

    @Override
    protected ContentSource getContentSource(final String path) {
        if (API_PATH.equals(path)) {
            return apiContentSource;
        }
        return super.getContentSource(path);
    }

}
