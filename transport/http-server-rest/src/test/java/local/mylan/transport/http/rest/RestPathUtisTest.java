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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RestPathUtisTest {

    @Test
    void exactMatcher(){
        final var matcher = RestPathUtils.getMatcher("/path/match");
        assertNotNull(matcher);
        assertTrue(matcher.matches("/path/match"));
        assertEquals(Map.of(), matcher.pathParameters());
    }

    @ParameterizedTest
    @MethodSource
    void regexMatcher(final String pathDef, final String testPath, final Map<String, String> expectedParameters) {
        final var matcher = RestPathUtils.getMatcher(pathDef);
        assertNotNull(matcher);
        assertTrue(matcher.matches(testPath));
        assertEquals(expectedParameters, matcher.pathParameters());
    }

    private static Stream<Arguments> regexMatcher() {
        return Stream.of(
            Arguments.of("/path/{a}/{b}/end", "/path/1/2/end", Map.of("a", "1", "b", "2")),
            Arguments.of("/path-{i:\\d+}-{s:\\w+}", "/path-1-value1", Map.of("i", "1", "s", "value1")),
            Arguments.of("/path{path:/.+}", "/path/var/value/path", Map.of("path", "/var/value/path"))
        );
    }

}
