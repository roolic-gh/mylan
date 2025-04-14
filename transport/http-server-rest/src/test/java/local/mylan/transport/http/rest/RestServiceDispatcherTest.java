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

import static io.netty.handler.codec.http.HttpHeaderNames.ACCEPT;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_XML;
import static local.mylan.transport.http.common.HttpTestUtils.executeRequest;
import static local.mylan.transport.http.common.HttpTestUtils.httpRequest;
import static local.mylan.transport.http.common.HttpTestUtils.setupChannel;
import static local.mylan.transport.http.common.HttpTestUtils.uriOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import local.mylan.transport.http.api.ContextDispatcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RestServiceDispatcherTest {
    private static final String CONTEXT_PATH = "/test-rest";

    static TestServiceProxy proxy;
    static ContextDispatcher dispatcher;

    @Mock
    TestService testService;

    @BeforeAll
    static void beforeAll() {
        proxy = new TestServiceProxy();
        dispatcher = new RestServiceDispatcher(CONTEXT_PATH, proxy);
    }

    @BeforeEach
    void beforeEach() {
        // service mock is being refreshed for every test,
        // proxy delegate allows service instance update within same dispatcher
        proxy.setDelegate(testService);
    }

    @ParameterizedTest
    @EnumSource(Encoding.class)
    void getWithRequestParams(final Encoding encoding) {
        final var dataList1 = List.of(testPojo("get1"), testPojo("get2"));
        doReturn(dataList1).when(testService).getData(eq(500), eq(0));

        final var channel = setupChannel(dispatcher);
        final var uri = uriOf(CONTEXT_PATH + "/data", Map.of("limit", "500"));
        final var request = httpRequest(HttpMethod.GET, uri, Map.of(ACCEPT, encoding.mediaType));
        final var response = executeRequest(channel, request);
        assertEquals(HttpResponseStatus.OK, response.status());
        assertEquals(encoding.mediaType.toString(), response.headers().get(CONTENT_TYPE));

        System.out.println(response);
        System.out.println(new String(ByteBufUtil.getBytes(response.content()), StandardCharsets.UTF_8));
    }

    enum Encoding {
        XML(APPLICATION_XML), JSON(APPLICATION_JSON);

        final CharSequence mediaType;

        Encoding(final CharSequence mediaType) {
            this.mediaType = mediaType;
        }
    }

    private static TestPojo testPojo(final String id) {
        return new TestPojo(id, "name-" + id, System.currentTimeMillis());
    }
}
