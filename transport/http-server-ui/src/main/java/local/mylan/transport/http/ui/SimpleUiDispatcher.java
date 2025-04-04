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

import static local.mylan.transport.http.common.RequestUtils.isRootUri;

import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import local.mylan.transport.http.common.StaticContentDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleUiDispatcher extends StaticContentDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleUiDispatcher.class);
    private static final String INDEX_PATH = "/index.html";

    private final ContentSource indexCached;

    public SimpleUiDispatcher(final String uiContextPath, final String restContextPath) {
        super(uiContextPath, "/mylan/ui-simple", SourceType.CLASSPATH);
        indexCached = cacheIndex(restContextPath);
    }

    private ContentSource cacheIndex(final String restContextPath) {
        // substitute configurable paths, index.html only
        try (var in = getClass().getResourceAsStream(resourceBase + INDEX_PATH)){
           final var content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
           final var bytes = content
               .replace("${SELF_CONTEXT}", contextPath)
               .replace("${REST_CONTEXT}", restContextPath)
               .getBytes(StandardCharsets.UTF_8);
           final var modified = System.currentTimeMillis();
           final var etag = Long.toHexString(modified);
           return new ContentSource(bytes.length, modified, HttpHeaderValues.TEXT_HTML, etag, bytes, null);
        } catch(IOException e){
            throw new IllegalStateException("Could not cache index content", e);
        }
    }

    @Override
    protected ContentSource getContentSource(final String path) {
        return isRootUri(path) || INDEX_PATH.equals(path) ? indexCached : super.getContentSource(path);
    }
}
