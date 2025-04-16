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
import static io.netty.handler.codec.http.HttpMethod.DELETE;
import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpMethod.PATCH;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static local.mylan.transport.http.common.HttpTestUtils.assertResponse;
import static local.mylan.transport.http.common.HttpTestUtils.executeRequest;
import static local.mylan.transport.http.common.HttpTestUtils.httpRequest;
import static local.mylan.transport.http.common.HttpTestUtils.setupChannel;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import local.mylan.service.api.UserContext;
import local.mylan.transport.http.api.ContextDispatcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RestServiceDispatcherTest {
    private static final String CONTEXT_PATH = "/test-rest";
    private static final AtomicInteger COUNT = new AtomicInteger(100);

    static TestServiceProxy proxy;
    static ContextDispatcher dispatcher;

    @Mock
    TestService testService;
    @Mock
    UserContext userCtx;

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
    void getWithRequestParams(final Encoding encoding) throws Exception {
        final var pojoList = nextPojoList();
        doReturn(pojoList).when(testService).getData(eq(500), eq(0));
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, restRequest(GET, "/data?limit=500", encoding));
        assertRestResponse(response, OK, encoding, pojoList);
    }

    @ParameterizedTest
    @EnumSource(Encoding.class)
    void getWithPathParams(final Encoding encoding) throws Exception {
        final var pojo = nextPojo();
        doReturn(pojo).when(testService).getData(pojo.getId());

        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, restRequest(GET, "/data/" + pojo.getId(), encoding));
        assertRestResponse(response, OK, encoding, pojo);
    }

    @ParameterizedTest
    @EnumSource(Encoding.class)
    void getWithUserContext(final Encoding encoding) throws Exception {
        final var pojoList = nextPojoList();
        doReturn(pojoList).when(testService).getData(userCtx);
        final var channel = setupChannel(dispatcher, userCtx);
        final var response = executeRequest(channel, restRequest(GET, "/data/by-user", encoding));
        assertRestResponse(response, OK, encoding, pojoList);
    }

    @ParameterizedTest
    @EnumSource(Encoding.class)
    void postWithPojo(final Encoding encoding) throws Exception {
        final var requestPojo = nextPojo();
        final var responsePojo = nextPojo();
        doReturn(responsePojo).when(testService).insertData(requestPojo);
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, restRequest(POST, "/data/insert", encoding, requestPojo));
        assertRestResponse(response, OK, encoding, responsePojo);
    }

    @ParameterizedTest
    @EnumSource(Encoding.class)
    void postWithPojoList(final Encoding encoding) throws Exception {
        final var pojoList = nextPojoList();
        doNothing().when(testService).insertData(anyList());
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, restRequest(POST, "/data/insert-all", encoding, pojoList));
        assertResponse(response, NO_CONTENT);
        verify(testService, times(1)).insertData(pojoList);
    }

    @ParameterizedTest
    @EnumSource(Encoding.class)
    void patch(final Encoding encoding) throws Exception {
        final var requestPojo = nextPojo();
        final var responsePojo = nextPojo();
        doReturn(responsePojo).when(testService).updateData("upd-id", requestPojo);
        final var channel = setupChannel(dispatcher);
        final var response = executeRequest(channel, restRequest(PATCH, "/data/upd-id", encoding, requestPojo));
        assertRestResponse(response, OK, encoding, responsePojo);
    }

    @Test
    void delete() throws Exception {
        doNothing().when(testService).deleteData(any());
        final var channel = setupChannel(dispatcher, userCtx);
        final var response = executeRequest(channel, httpRequest(DELETE, CONTEXT_PATH + "/data/del-id"));
        assertResponse(response, NO_CONTENT);
        verify(testService, times(1)).deleteData("del-id");
    }

    private static List<TestPojo> nextPojoList() {
        return List.of(nextPojo(), nextPojo(), nextPojo());
    }

    private static TestPojo nextPojo() {
        final var count = COUNT.incrementAndGet();
        return new TestPojo("id-" + count, "name-" + count, System.currentTimeMillis());
    }

    private static FullHttpRequest restRequest(final HttpMethod method, final String uri, final Encoding encoding) {
        return httpRequest(method, CONTEXT_PATH + uri, Map.of(ACCEPT, encoding.mediaType()));
    }

    private static FullHttpRequest restRequest(final HttpMethod method, final String uri, final Encoding encoding,
        final Object contentObj) throws IOException {
        return httpRequest(method, CONTEXT_PATH + uri, encoding.mediaType(), encoding.mediaType(),
            encoding.objectMapper().writeValueAsBytes(contentObj));
    }

    private static void assertRestResponse(final FullHttpResponse response, final HttpResponseStatus expectedStatus,
        final Encoding encoding, final Object expectedData) throws Exception {
        assertResponse(response, OK, encoding.mediaType().toString(),
            encoding.objectMapper().writeValueAsBytes(expectedData));
    }
}
