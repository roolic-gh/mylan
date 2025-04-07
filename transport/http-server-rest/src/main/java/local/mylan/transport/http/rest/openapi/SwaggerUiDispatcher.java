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
package local.mylan.transport.http.rest.openapi;

import static local.mylan.transport.http.common.RequestUtils.isRootUri;

import local.mylan.transport.http.common.StaticContentDispatcher;

public class SwaggerUiDispatcher extends StaticContentDispatcher {

    static final String RESOURCE_PATH = "/mylan/swagger-ui";
    static final String INDEX_PATH = "/index.html";
    static final String INITIALIZER_PATH = "/swagger-initializer.js";

    private final String restContextPath;
    private final String initializerContent;
    private final ContentSource apiContent;

    public SwaggerUiDispatcher(final String contextPath, final String restContextPath,
            final Class<?> ... serviceClasses) {
        super(contextPath, RESOURCE_PATH);
        this.restContextPath = restContextPath;
        initializerContent = loadInitializerContent();
        apiContent = buildApiContent(serviceClasses);
    }

    @Override
    protected ContentSource getContentSource(final String path) {
        if(INITIALIZER_PATH.equals(restContextPath)){
            // TODO
        }
        return super.getContentSource(isRootUri(path) ? INDEX_PATH : path);
    }

    private String loadInitializerContent() {
        return null;
    }

    private ContentSource buildApiContent(final Class<?>[] serviceClasses) {
        return null;
    }

}
