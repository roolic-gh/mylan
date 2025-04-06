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

import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_HTML;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static local.mylan.transport.http.common.HttpTestUtils.assertResponse;
import static local.mylan.transport.http.common.HttpTestUtils.executeRequest;
import static local.mylan.transport.http.common.HttpTestUtils.httpRequest;
import static local.mylan.transport.http.common.HttpTestUtils.setupChannel;
import static local.mylan.transport.http.ui.SimpleUiDispatcher.INDEX_PATH;
import static local.mylan.transport.http.ui.SimpleUiDispatcher.RESOURCE_PATH;
import static local.mylan.transport.http.ui.SimpleUiDispatcher.REST_CONTEXT_REPLACE;
import static local.mylan.transport.http.ui.SimpleUiDispatcher.SELF_CONTEXT_REPLACE;

import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SimpleUiDispatcherTest {
    private static final String CONTEXT_PATH = "/test-ui";
    private static final String REST_CONTEXT_PATH = "/test-rest";

    static SimpleUiDispatcher dispatcher;
    static byte[] indexContent;

     @BeforeAll
     static void beforeAll()  throws IOException{
         dispatcher = new SimpleUiDispatcher(CONTEXT_PATH, REST_CONTEXT_PATH);
        try (var in = dispatcher.getClass().getResourceAsStream(RESOURCE_PATH + INDEX_PATH)) {
            indexContent = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .replace(SELF_CONTEXT_REPLACE, CONTEXT_PATH)
                .replace(REST_CONTEXT_REPLACE, REST_CONTEXT_PATH)
                .getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", INDEX_PATH})
    void valdateIndexContent(final String indexPath) {
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, httpRequest(GET, CONTEXT_PATH + indexPath));
        assertResponse(response, HttpResponseStatus.OK, TEXT_HTML.toString(), indexContent );
    }
}
