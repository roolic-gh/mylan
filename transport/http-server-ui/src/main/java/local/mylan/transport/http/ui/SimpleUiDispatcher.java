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
import java.util.Map;
import local.mylan.transport.http.ext.StaticContentDispatcher;

public class SimpleUiDispatcher extends StaticContentDispatcher {
    @VisibleForTesting
    static final String RESOURCE_PATH = "/mylan/ui-simple";
    @VisibleForTesting
    static final String INDEX_PATH = "/index.html";
    @VisibleForTesting
    static final String SELF_CONTEXT_REPLACE = "${SELF_CONTEXT}";
    @VisibleForTesting
    static final String REST_CONTEXT_REPLACE = "${REST_CONTEXT}";

    public SimpleUiDispatcher(final String uiContextPath, final String restContextPath) {
        super(uiContextPath, RESOURCE_PATH, SourceType.CLASSPATH);
        substitute(INDEX_PATH, Map.of(
            SELF_CONTEXT_REPLACE, contextPath,
            REST_CONTEXT_REPLACE, restContextPath));
    }
}
