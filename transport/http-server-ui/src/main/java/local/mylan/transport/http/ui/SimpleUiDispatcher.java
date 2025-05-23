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
package local.mylan.transport.http.ui;

import com.google.common.annotations.VisibleForTesting;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import local.mylan.transport.http.common.StaticContentDispatcher;

public class SimpleUiDispatcher extends StaticContentDispatcher {
    @VisibleForTesting
    static final String RESOURCE_PATH = "/mylan/ui-simple";
    @VisibleForTesting
    static final String INDEX_PATH = "/index.html";
    @VisibleForTesting
    static final String SELF_CONTEXT_REPLACE = "${SELF_CONTEXT}";
    @VisibleForTesting
    static final String REST_CONTEXT_REPLACE = "${REST_CONTEXT}";

    private final ContentSource indexCached;

    public SimpleUiDispatcher(final String uiContextPath, final String restContextPath) {
        super(uiContextPath, RESOURCE_PATH, SourceType.CLASSPATH);
        indexCached = cacheIndex(restContextPath);
    }

    private ContentSource cacheIndex(final String restContextPath) {
        // substitute configurable paths, index.html only
        try (var in = getClass().getResourceAsStream(resourceBase + INDEX_PATH)){
           final var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
           final var bytes = content
               .replace(SELF_CONTEXT_REPLACE, contextPath)
               .replace(REST_CONTEXT_REPLACE, restContextPath)
               .getBytes(StandardCharsets.UTF_8);
           final var modified = System.currentTimeMillis();
           final var etag = Long.toHexString(modified);
           return new ContentSource(bytes.length, modified, HttpHeaderValues.TEXT_HTML, etag, bytes, null);
        } catch(IOException e){
            throw new IllegalStateException("Could not cache index content", e);
        }
    }

    @Override
    protected String emptyRedirectPath() {
        return INDEX_PATH;
    }

    @Override
    protected ContentSource getContentSource(final String path) {
        return INDEX_PATH.equals(path) ? indexCached : super.getContentSource(path);
    }
}
