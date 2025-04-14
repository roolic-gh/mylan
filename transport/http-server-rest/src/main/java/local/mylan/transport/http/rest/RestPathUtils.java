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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RestPathUtils {
    private static final Logger LOG = LoggerFactory.getLogger(RestPathUtils.class);
    private static final Pattern PATH_VAR_PATTERN = Pattern.compile("[^{]*(\\{[^}]+}).*");

    private RestPathUtils() {
        // utility class
    }

    static RestPathMatcher getMatcher(final String path) {
        final List<String> paramNames = new ArrayList<>();
        var pathRegex = path;
        boolean replaced;
        do {
            replaced = false;
            final var matcher = PATH_VAR_PATTERN.matcher(pathRegex);
            if (matcher.matches()) {
                final var toReplace = matcher.group(1);
                var parts = toReplace.substring(1, toReplace.length() - 1).split(":");
                paramNames.add(parts[0]);
                final var replaceBy = parts.length > 1 ? '(' + parts[1] + ')' : "([^/]+)";
                pathRegex = pathRegex.replace(toReplace, replaceBy);
                replaced = true;
            }
        } while (replaced);
        LOG.debug("Path {} parsed to -> regex: {} parameters: {}", path, pathRegex, paramNames);
        return paramNames.isEmpty()
            ? new ExactMatcher(path)
            : new RegexMatcher(Pattern.compile(pathRegex), paramNames.toArray(String[]::new));
    }

    static final class ExactMatcher implements RestPathMatcher {
        private final String matchingPath;

        private ExactMatcher(final String matchingPath) {
            this.matchingPath = matchingPath;
        }

        @Override
        public RestPathMatcher newInstance() {
            return this;
        }

        @Override
        public boolean matches(final String path) {
            return matchingPath.equals(path);
        }

        @Override
        public Map<String, String> pathParameters() {
            return Map.of();
        }
    }

    static final class RegexMatcher implements RestPathMatcher {
        private final Pattern pattern;
        private final String[] paramNames;
        private final AtomicReference<Map<String, String>> paramsRef = new AtomicReference<>();

        private RegexMatcher(final Pattern pattern, final String[] paramNames) {
            this.pattern = pattern;
            this.paramNames = paramNames;
        }

        @Override
        public RestPathMatcher newInstance() {
            return new RegexMatcher(pattern, paramNames);
        }

        @Override
        public boolean matches(final String path) {
            final var matcher = pattern.matcher(path);
            if (matcher.matches()) {
                final var paramsMap = new HashMap<String, String>();
                for (int i = 0; i < paramNames.length; i++) {
                    paramsMap.put(paramNames[i], matcher.group(i + 1));
                }
                paramsRef.set(Map.copyOf(paramsMap));
                return true;
            }
            return false;
        }

        @Override
        public Map<String, String> pathParameters() {
            return paramsRef.get();
        }
    }
}
