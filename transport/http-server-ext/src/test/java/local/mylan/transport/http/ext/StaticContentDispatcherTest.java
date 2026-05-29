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
package local.mylan.transport.http.ext;

import static io.netty.handler.codec.http.HttpHeaderNames.ETAG;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_NONE_MATCH;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static java.nio.charset.StandardCharsets.UTF_8;
import static local.mylan.transport.http.common.HttpTestUtils.assertResponse;
import static local.mylan.transport.http.common.HttpTestUtils.executeRequest;
import static local.mylan.transport.http.common.HttpTestUtils.httpRequest;
import static local.mylan.transport.http.common.HttpTestUtils.setupChannel;
import static local.mylan.transport.http.ext.StaticContentDispatcher.SourceType.FILE_SYSTEM;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import local.mylan.transport.http.common.HttpTestUtils;
import local.mylan.transport.http.common.api.ContextDispatcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class StaticContentDispatcherTest {

    private static final String CONTEXT_PATH = "/test";
    private static final String CLASSPATH_PATH = "/classpath-content";
    private static final String TEXT_FILE_NAME = "test-file.txt";
    private static final byte[] TEXT_FILE_CONTENT = "text content".getBytes(UTF_8);
    private static final String BIN_FILE_NAME = "test-file.bin";
    private static final byte[] BIN_FILE_CONTENT = "bin content".getBytes(UTF_8);

    private static final Map<String, String> SUBSTITUTE_MAP = Map.of("${test}", "T", "http://replace-me", "/replaced");
    private static final String SUBSTITUTE_FILE_NAME = "substitute.txt";
    private static final byte[] SUBSTITUTE_FILE_CONTENT = "A=${test} B=${test} C=http://replace-me".getBytes(UTF_8);
    private static final byte[] SUBSTITUTED_CONTENT = "A=T B=T C=/replaced".getBytes(UTF_8);
    private static final byte[] UPDATED_FILE_CONTENT = "X=${test} Y=http://replace-me".getBytes(UTF_8);
    private static final byte[] UPDATED_SUBSTITUTED_CONTENT = "X=T Y=/replaced".getBytes(UTF_8);

    private static final String[] EMPTY_PATHS = {CONTEXT_PATH, CONTEXT_PATH + '/'};
    private static final String REDIRECT = "http://" + HttpTestUtils.DEFAUT_HOST + CONTEXT_PATH + "/index.html";

    @TempDir
    static Path contentDir;

    static StaticContentDispatcher classpathDispatcher;
    static StaticContentDispatcher filesystemDispatcher;

    @BeforeAll
    static void beforeAll() throws IOException {
        Files.write(contentDir.resolve(TEXT_FILE_NAME), TEXT_FILE_CONTENT);
        Files.write(contentDir.resolve(BIN_FILE_NAME), BIN_FILE_CONTENT);
        Files.write(contentDir.resolve(SUBSTITUTE_FILE_NAME), SUBSTITUTE_FILE_CONTENT);
        classpathDispatcher = new StaticContentDispatcher(CONTEXT_PATH, CLASSPATH_PATH);
        filesystemDispatcher = new StaticContentDispatcher(CONTEXT_PATH, contentDir.toString(), FILE_SYSTEM);
    }

    private static List<StaticContentDispatcher> dispatchers() {
        return List.of(classpathDispatcher, filesystemDispatcher);
    }

    @ParameterizedTest
    @MethodSource("dispatchers")
    void fetchTextFile(final ContextDispatcher dispatcher) {
        assertOkResponse(dispatcher, TEXT_FILE_NAME, TEXT_PLAIN.toString(), TEXT_FILE_CONTENT);
    }

    @ParameterizedTest
    @MethodSource("dispatchers")
    void fetchBinFile(final ContextDispatcher dispatcher) {
        assertOkResponse(dispatcher, BIN_FILE_NAME, APPLICATION_OCTET_STREAM.toString(), BIN_FILE_CONTENT);
    }

    private static void assertOkResponse(final ContextDispatcher dispatcher, final String filename,
        final String expectedMeiaType, final byte[] expectedContent) {

        final var uri = CONTEXT_PATH + '/' + filename;
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, httpRequest(GET, uri));
        assertResponse(response, HttpResponseStatus.OK, expectedMeiaType, expectedContent);

        final var etag = response.headers().get(ETAG);
        Assertions.assertNotNull(etag);
        final var response2 = executeRequest(channel, httpRequest(GET, uri, Map.of(IF_NONE_MATCH, etag)));
        assertResponse(response2, HttpResponseStatus.NOT_MODIFIED);
    }

    @ParameterizedTest
    @MethodSource("dispatchers")
    void notFound(final ContextDispatcher dispatcher) {
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, httpRequest(GET, CONTEXT_PATH + "/unknown-file.txt"));
        assertResponse(response, HttpResponseStatus.NOT_FOUND);
    }

    @ParameterizedTest
    @MethodSource("dispatchers")
    void emptyRedirect(final ContextDispatcher dispatcher) {
        for (var emptyPath : EMPTY_PATHS) {
            final var channel = setupChannel(dispatcher);
            final var response = executeRequest(channel, httpRequest(GET, emptyPath));
            assertResponse(response, HttpResponseStatus.FOUND);
            Assertions.assertEquals(REDIRECT, response.headers().get(HttpHeaderNames.LOCATION));
        }
    }

    @ParameterizedTest
    @MethodSource("dispatchers")
    void substituteAndCheckUpdated(final StaticContentDispatcher dispatcher) throws Exception {
        dispatcher.substitute('/' + SUBSTITUTE_FILE_NAME, SUBSTITUTE_MAP);

        final var uri = CONTEXT_PATH + '/' + SUBSTITUTE_FILE_NAME;
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, httpRequest(GET, uri));
        assertResponse(response, HttpResponseStatus.OK, TEXT_PLAIN.toString(), SUBSTITUTED_CONTENT);

        final var etag = response.headers().get(ETAG);
        Assertions.assertNotNull(etag);
        final var response2 = executeRequest(channel, httpRequest(GET, uri, Map.of(IF_NONE_MATCH, etag)));
        assertResponse(response2, HttpResponseStatus.NOT_MODIFIED);

        // modify file content, verify cache invalidated and updated with new content
        if (dispatcher.type == FILE_SYSTEM) {
            dispatcher.setCheckFileUpdates(true);
            Files.write(contentDir.resolve(SUBSTITUTE_FILE_NAME), UPDATED_FILE_CONTENT);
            final var response3 = executeRequest(channel, httpRequest(GET, uri, Map.of(IF_NONE_MATCH, etag)));
            assertResponse(response3, HttpResponseStatus.OK, TEXT_PLAIN.toString(), UPDATED_SUBSTITUTED_CONTENT);

            final var etag2 = response3.headers().get(ETAG);
            Assertions.assertNotNull(etag2);
            final var response4 = executeRequest(channel, httpRequest(GET, uri, Map.of(IF_NONE_MATCH, etag2)));
            assertResponse(response4, HttpResponseStatus.NOT_MODIFIED);
        }
    }
}
