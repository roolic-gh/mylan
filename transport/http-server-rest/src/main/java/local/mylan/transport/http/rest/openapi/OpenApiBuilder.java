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

import io.netty.handler.codec.http.HttpHeaderValues;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.PathUtils;
import io.swagger.v3.core.util.PrimitiveType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import local.mylan.common.annotations.rest.PathParameter;
import local.mylan.common.annotations.rest.QueryParameter;
import local.mylan.common.annotations.rest.RequestBody;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.common.annotations.rest.ServiceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OpenApiBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(OpenApiBuilder.class);

    private final Map<Type, Schema<?>> schemaMap = new HashMap<>();
    private final List<String> supportedEncodings;
    private final OpenAPI openApi = new OpenAPI();
    private Components components = new Components();
    private Paths paths = new Paths();
    private Set<Tag> tags = new HashSet<>();

    private OpenApiBuilder(final String rootPath) {
        this(rootPath, HttpHeaderValues.APPLICATION_JSON.toString());
    }

    OpenApiBuilder(final String rootPath, final String... supportedEncodings) {
        this.supportedEncodings = List.of(supportedEncodings);
        final var server = new Server();
        server.setUrl(rootPath);
        openApi.setServers(List.of(server));
        // TODO security
    }

    OpenAPI build() {
        openApi.setInfo(defaultInfo());
        openApi.setPaths(paths);
        openApi.setComponents(components);
        return openApi;
    }

    private static Info defaultInfo() {
        // TODO make configurable
        final var info = new Info();
        info.setTitle("MyLAN REST Services - OpenAPI 3.0");
        final var license = new License();
        license.setName("Apache 2.0");
        license.setUrl("http://www.apache.org/licenses/LICENSE-2.0.html");
        info.setLicense(license);
        return info;
    }

    OpenApiBuilder process(final Collection<Class<?>> classes) {
        classes.forEach(this::process);
        return this;
    }

    OpenApiBuilder process(final Class<?> cls) {
        final var classDeprecated = cls.getAnnotation(Deprecated.class) != null;
        final var tag = extractTag(cls);
        int parsedCount = 0;
        for (var method : cls.getMethods()) {
            final var mapping = method.getAnnotation(RequestMapping.class);
            if (mapping == null) {
                continue;
            }
            final var regexMap = new HashMap<String, String>();
            final var path = PathUtils.parsePath(mapping.path(), regexMap);
            final PathItem.HttpMethod httpMethod;
            try {
                httpMethod = PathItem.HttpMethod.valueOf(mapping.method());
            } catch (IllegalArgumentException e) {
                LOG.warn("Unsupported HTTP method defined for {}.{}() -> {}} -> method ignored",
                    cls.getSimpleName(), method.getName(), mapping.method());
                continue;
            }

            final var operation = new Operation();
            operation.setOperationId(method.getName());
            operation.setSummary(nonEmptyOrNull(mapping.description()));
            operation.addTagsItem(tag.getName());
            if (classDeprecated || method.getAnnotation(Deprecated.class) != null) {
                operation.setDeprecated(true);
            }

            final var params = new ArrayList<Parameter>();
            for (var methodParam : method.getParameters()) {
                final var param = buildParameter(methodParam);
                if (param == null) {
                    continue;
                }
                final var schema = resolveSchema(methodParam.getType());
                schema.setPattern(regexMap.get(param.getName()));
                param.setSchema(schema);
                params.add(param);
            }
            if (!params.isEmpty()) {
                operation.setParameters(List.copyOf(params));
            }

            // TODO check response annotations

            final var responses = new ApiResponses();
            final var returnType = method.getGenericReturnType();
            final var responseSchema = Void.TYPE.equals(returnType) ? null : resolveSchema(returnType);
            final var successCode = responseSchema == null ? "200" : "204";
            final var response = new ApiResponse();
            response.setDescription("Success");
            if (responseSchema != null) {
                final var content = new Content();
                supportedEncodings.forEach(
                    encoding -> {
                        final var mediaType = new MediaType();
                        mediaType.setSchema(responseSchema);
                        content.addMediaType(encoding, mediaType);
                    }
                );
                response.setContent(content);
            }
            responses.addApiResponse(successCode, response);
            operation.setResponses(responses);

            if (!paths.containsKey(path)) {
                paths.put(path, new PathItem());
            }
            paths.get(path).operation(httpMethod, operation);
            parsedCount++;
        }

        if (parsedCount > 0) {
            tags.add(tag);
        }
        return this;
    }

    private static Parameter buildParameter(java.lang.reflect.Parameter methodParam) {
        if (methodParam.getAnnotation(RequestBody.class) != null) {
            final var result = new Parameter();
            result.setName(methodParam.getName());
            result.setIn("body");
            result.setRequired(true);
            return result;
        }
        final var queryParam = methodParam.getAnnotation(QueryParameter.class);
        if (queryParam != null) {
            final var result = new io.swagger.v3.oas.models.parameters.QueryParameter();
            if (queryParam.required()) {
                result.setRequired(true);
            }
            result.setName(nonEmptyOrDefault(queryParam.name(), methodParam.getName()));
            return result;
        }
        final var pathParam = methodParam.getAnnotation(PathParameter.class);
        if (pathParam != null) {
            final var result = new io.swagger.v3.oas.models.parameters.PathParameter();
            result.setName(nonEmptyOrDefault(pathParam.value(), methodParam.getName()));
            return result;
        }
        return null;
    }

    private static Tag extractTag(final Class<?> cls) {
        final var tag = new Tag();
        final var serviceDesc = cls.getAnnotation(ServiceDescriptor.class);
        if (serviceDesc != null) {
            tag.setName(serviceDesc.id());
            tag.setDescription(serviceDesc.description());
        } else {
            tag.setName(cls.getSimpleName());
        }
        return tag;
    }

    private Schema<?> resolveSchema(final Type type) {
        final var primitiveType = PrimitiveType.fromType(type);
        if (primitiveType != null) {
            return primitiveType.createProperty();
        }
        final var cached = schemaMap.get(type);
        if (cached != null) {
            return cached;
        }
        final var map = ModelConverters.getInstance().readAll(type);
        map.forEach(components::addSchemas);
        final var resolvedSchema = ModelConverters.getInstance()
            .resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true).components(components));
        final var schema = resolvedSchema.schema;
        schemaMap.put(type, schema);
        return schema;
    }

    private static String nonEmptyOrNull(final String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String nonEmptyOrDefault(final String value, final String defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}
