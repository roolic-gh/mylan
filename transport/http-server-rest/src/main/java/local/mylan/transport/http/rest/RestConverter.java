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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

final class RestConverter {
    static final RestConverter INSTANCE = new RestConverter();
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final XmlMapper XML_MAPPER = new XmlMapper();
    private final Map<Type, Function<String, ?>> convertersByType = new HashMap<>();

    private RestConverter() {
        // singleton
    }

    static CharSequence mediaTypeFrom(final String headerValue) {
        return isXml(headerValue) ? HttpHeaderValues.APPLICATION_XML : HttpHeaderValues.APPLICATION_JSON;
    }

    static boolean isXml(final CharSequence headerValue) {
        final var mediaType = headerValue == null ? "" : headerValue.toString();
        return mediaType.contains("xml");
    }

    static <T> T fromRequestBody(final Class<T> type, final FullHttpRequest request) {
        final var isXml = isXml(request.headers().get(HttpHeaderNames.CONTENT_TYPE));
        try (InputStream in = new ByteBufInputStream(request.content())) {
            return isXml ? XML_MAPPER.readValue(in, type) : JSON_MAPPER.readValue(in, type);
        } catch (IOException e) {
            throw new IllegalArgumentException("Request body is not a valid %s".formatted(isXml ? "XML" : "JSON"), e);
        }
    }

    static ByteBuf toResponseBody(final Object obj, final CharSequence mediaType) {
        final var isXml = isXml(mediaType);
        try {
            final var bytes = isXml ? XML_MAPPER.writeValueAsBytes(obj) : JSON_MAPPER.writeValueAsBytes(obj);
            return Unpooled.wrappedBuffer(bytes);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                "Error on converting of %s to %s".formatted(obj, isXml ? "XML" : "JSON"), e);
        }
    }

    Function<String, ?> typeConverter(final Type type) {
        final var cached = convertersByType.get(type);
        if (cached != null) {
            return cached;
        }
        final var primitive = Primitive.byType(type);
        if (primitive != null) {
            convertersByType.put(type, primitive.converter);
            return primitive.converter;
        }
        final var valueOfConverter = valueOfCoverter(type);
        if (valueOfConverter != null) {
            convertersByType.put(type, valueOfConverter);
            return valueOfConverter;
        }
        throw new IllegalStateException("Cannot find string to object converter for type " + type);
    }

    private static Function<String, ?> valueOfCoverter(final Type type) {
        if (type instanceof Class<?> cls) {
            final Method convertMethod;
            try {
                convertMethod = cls.getMethod("valueOf", String.class);
            } catch (NoSuchMethodException e) {
                return null;
            }
            if (Modifier.isStatic(convertMethod.getModifiers())) {
                return str -> {
                    try {
                        return str == null ? null : convertMethod.invoke(null, str);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        // not expected
                        throw new IllegalStateException("Exception invoking static valueOf() method", e);
                    }
                };
            }
        }
        return null;
    }

    enum Primitive {
        BOOLEAN(Boolean::valueOf, Boolean.FALSE, Boolean.TYPE, Boolean.class),
        BYTE(Byte::valueOf, Byte.MIN_VALUE, Byte.TYPE, Byte.class),
        CHAR(str -> str.charAt(0), Character.MIN_VALUE, Character.TYPE, Character.class),
        INTEGER(Integer::valueOf, Integer.valueOf(0), Integer.TYPE, Integer.class),
        LONG(Long::valueOf, Long.valueOf(0), Long.TYPE, Long.class),
        STRING(Function.identity(), null, String.class);

        final Function<String, ?> converter;
        final Set<Type> types;

        Primitive(Function<String, ?> nonNullConverter, Object defaultValue, Type... supportedTypes) {
            converter = str -> str == null || str.isEmpty() ? defaultValue : nonNullConverter.apply(str);
            types = Set.of(supportedTypes);
        }

        static Primitive byType(Type type) {
            for (var primitive : values()) {
                if (primitive.types.contains(type)) {
                    return primitive;
                }
            }
            return null;
        }

    }
}
