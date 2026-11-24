/*
 * Copyright 2026 Ruslan Kashapov
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
package local.mylan.transport.http.stream;

import static local.mylan.transport.http.common.utils.ResponseUtils.allowResponse;
import static local.mylan.transport.http.common.utils.ResponseUtils.notFoundResponse;
import static local.mylan.transport.http.common.utils.ResponseUtils.unsupportedMethodResponse;

import java.io.InputStream;
import java.util.function.Function;
import local.mylan.transport.http.common.api.ContextDispatcher;
import local.mylan.transport.http.common.api.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamDispatcher implements ContextDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(StreamDispatcher.class);
    private static final String ALLOWED_METHODS = "GET, OPTIONS";

    private final String contextPath;
    private final Function<String, InputStream> streamProvider;

    public StreamDispatcher(final String contextPath, final Function<String, InputStream> streamProvider) {
        this.contextPath = contextPath;
        this.streamProvider = streamProvider;
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public boolean dispatch(final RequestContext ctx) {
        switch (ctx.method().name()) {
            case "OPTIONS" -> ctx.sendResponse(allowResponse(ctx.protocolVersion(), ALLOWED_METHODS));
            case "GET" -> handleContent(ctx);
            default -> ctx.sendResponse(unsupportedMethodResponse(ctx.protocolVersion()));
        }
        return true;
    }

    private void handleContent(final RequestContext ctx) {
        LOG.info("Stream --> {}", ctx.contextPath());
        // TODO proper implementation
        ctx.sendResponse(notFoundResponse(ctx.protocolVersion()));
    }
}
