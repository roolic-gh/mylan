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

import java.util.Map;
import java.util.function.Function;
import local.mylan.common.annotations.rest.PathParameter;
import local.mylan.common.annotations.rest.QueryParameter;
import local.mylan.common.annotations.rest.RequestBody;
import local.mylan.service.api.UserContext;
import local.mylan.transport.http.api.RequestContext;

final class RestArgUtils {

    private RestArgUtils() {
        // utility class
    }

    static RestArgBuilder getArgBuilder(final java.lang.reflect.Parameter methodParam) {
        final Class<?> targetType = methodParam.getType();
        if (methodParam.getAnnotation(RequestBody.class) != null) {
            return new RequestBodyArgBuilder(targetType);
        }
        final var queryParam = methodParam.getAnnotation(QueryParameter.class);
        if (queryParam != null) {
            final var paramName = queryParam.name();
            return new ParamArgBuilder(paramName,
                queryParam.required(),
                (ctx, pathParameters) -> ctx.requestParameters().get(paramName),
                RestConverter.INSTANCE.typeConverter(targetType));
        }
        final var pathParam = methodParam.getAnnotation(PathParameter.class);
        if (pathParam != null) {
            final var paramName = pathParam.value();
            return new ParamArgBuilder(paramName,
                true,
                (ctx, pathParameters) -> pathParameters.get(paramName),
                RestConverter.INSTANCE.typeConverter(targetType));
        }
        return argBuilderByType(targetType);
    }

    static class ParamArgBuilder implements RestArgBuilder {
        private final String paramName;
        private final boolean required;
        private final ParamVaueExtractor valueExtractor;
        private final Function<String, ?> valueConverter;

        private ParamArgBuilder(final String paramName, final boolean required, final ParamVaueExtractor valueExtractor,
            final Function<String, ?> valueConverter) {
            this.paramName = paramName;
            this.required = required;
            this.valueExtractor = valueExtractor;
            this.valueConverter = valueConverter;
        }

        @Override
        public Object buildArgObject(RequestContext ctx, Map<String, String> pathParameters) {
            final var value = valueExtractor.getParamVaue(ctx, pathParameters);
            if (value == null && required) {
                throw new IllegalArgumentException("Missing required parameter '%s'".formatted(paramName));
            }
            return valueConverter.apply(value);
        }
    }

    private static class RequestBodyArgBuilder implements RestArgBuilder {
        final Class<?> argType;

        private RequestBodyArgBuilder(final Class<?> argType) {
            this.argType = argType;
        }

        @Override
        public Object buildArgObject(final RequestContext ctx, final Map<String, String> pathParameters) {
            return RestConverter.fromRequestBody(argType, ctx.fullRequest());
        }
    }

    @FunctionalInterface
    private interface ParamVaueExtractor {
        String getParamVaue(RequestContext ctx, Map<String, String> pathParameters);

    }

    private static RestArgBuilder argBuilderByType(final Class<?> type) {
        if (type.isAssignableFrom(UserContext.class)) {
            return (ctx, pathParameters) -> ctx.userContext();
        }
        throw new IllegalArgumentException("No Arg Builder for type" + type);
    }
}
