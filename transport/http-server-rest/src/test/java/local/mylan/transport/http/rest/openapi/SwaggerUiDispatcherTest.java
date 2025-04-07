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
package local.mylan.transport.http.rest.openapi;

import local.mylan.transport.http.api.ContextDispatcher;
import local.mylan.transport.http.rest.TestRestService;
import org.junit.jupiter.api.BeforeAll;

public class SwaggerUiDispatcherTest {
    private static final String CONTEXT_PATH = "/test-swagger-ui";
    private static final String REST_PATH = "/test-rest";

    static ContextDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() {
        dispatcher = new SwaggerUiDispatcher(CONTEXT_PATH, REST_PATH, TestRestService.class);
    }
}
