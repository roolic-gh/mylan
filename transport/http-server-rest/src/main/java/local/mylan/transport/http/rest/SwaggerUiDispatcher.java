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

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.swagger.v3.core.util.Json;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import local.mylan.transport.http.api.RequestContext;
import local.mylan.transport.http.common.StaticContentDispatcher;

public class SwaggerUiDispatcher extends StaticContentDispatcher {

    static final String RESOURCE_PATH = "/mylan/swagger-ui";
    static final String INITIALIZER_PATH = "/swagger-initializer.js";
    static final String API_PATH = "/api.js";
    static final String URL_TO_REPLACE = "https://petstore.swagger.io/v2/swagger.json";

    private final ContentSource initializerContentSource;
    private final ContentSource apiContentSource;

    public SwaggerUiDispatcher(final String contextPath, final String restContextPath,
        final Class<?>... serviceClasses) {
        super(contextPath, RESOURCE_PATH);
        initializerContentSource = buildInitialiserContentSource();
        apiContentSource = buildApiContentSource(restContextPath, serviceClasses);
    }

    private ContentSource buildInitialiserContentSource() {
        // substitute configurable paths, index.html only
        try (var in = getClass().getResourceAsStream(resourceBase + INITIALIZER_PATH)) {
            final var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            final var bytes = content
                .replace(URL_TO_REPLACE, contextPath + API_PATH)
                .getBytes(StandardCharsets.UTF_8);
            final var modified = System.currentTimeMillis();
            final var etag = Long.toHexString(modified);
            return new ContentSource(bytes.length, modified, HttpHeaderValues.APPLICATION_JSON, etag, bytes, null);
        } catch (IOException e) {
            throw new IllegalStateException("Could not cache initializer content", e);
        }
    }

    private static ContentSource buildApiContentSource(final String restContextPath, final Class<?>... serviceClasses) {
        final var openApi = new OpenApiBuilder(restContextPath).process(List.of(serviceClasses)).build();
        final var bytes = Json.mapper().convertValue(openApi, ObjectNode.class)
            .toString().getBytes(StandardCharsets.UTF_8);
        final var modified = System.currentTimeMillis();
        final var etag = Long.toHexString(modified);
        return new ContentSource(bytes.length, modified,
            HttpHeaderValues.APPLICATION_JSON.toString(), etag,
            null, () -> new ByteArrayInputStream(bytes));
    }

    @Override
    protected ContentSource getContentSource(final String path) {
        return switch (path) {
            case INITIALIZER_PATH -> initializerContentSource;
            case API_PATH -> apiContentSource;
            default -> super.getContentSource(path);
        };
    }

    @Override
    public boolean dispatch(final RequestContext ctx) {
        return super.dispatch(ctx);
    }

}
