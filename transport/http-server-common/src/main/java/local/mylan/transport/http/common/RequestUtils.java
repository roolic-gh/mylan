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
package local.mylan.transport.http.common;

import static java.util.Objects.requireNonNull;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import local.mylan.transport.http.api.RequestContext;
import local.mylan.transport.http.api.UserContext;
import local.mylan.transport.http.spi.DefaultRequestContext;

public final class RequestUtils {

    private RequestUtils() {
        // utility class
    }

    public static boolean isRootUri(final String uri){
        requireNonNull(uri);
        return uri.isEmpty() || "/".equals(uri);
    }

    public static RequestContext buildRequestContext(final ChannelHandlerContext ctx, final FullHttpRequest request,
            final String basePath, final UserContext userContext) {
        requireNonNull(request);
        requireNonNull(basePath);
        final var decoder = new QueryStringDecoder(request.uri(), StandardCharsets.UTF_8);
        final var uriPath = decoder.path();
        final int cut = basePath.length();
        final var contextPath = uriPath.endsWith("/")
            ? uriPath.substring(cut, uriPath.length() - 1) : uriPath.substring(cut);
        final var params = remapParameters(decoder.parameters());
        return new DefaultRequestContext(ctx, request, params, contextPath, userContext);
    }

    private static Map<String, String> remapParameters(final Map<String, List<String>> allParams) {
        if (allParams == null || allParams.isEmpty()) {
            return Map.of();
        }
        final var map = new HashMap<String, String>();
        allParams.forEach((name, value) -> {
            if (value != null && !value.isEmpty()) {
                map.put(name, value.getFirst());
            }
        });
        return Map.copyOf(map);
    }

}
