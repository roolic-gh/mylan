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
package local.mylan.transport.http.rest.service;

import java.util.List;
import local.mylan.transport.http.api.ContextDispatcher;
import local.mylan.transport.http.api.RequestContext;

public class RestServiceDispatcher implements ContextDispatcher {

    private final String contextPath;
    private final List<Object> serviceInstances;

    public RestServiceDispatcher(final String contextPath, final Object ... serviceInstances) {
        this.contextPath = contextPath;
        this.serviceInstances = List.of(serviceInstances);
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public boolean dispatch(final RequestContext ctx) {
        return false;
    }
}
