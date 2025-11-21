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
package local.mylan.common.utils;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import local.mylan.common.annotations.conf.ConfFile;
import local.mylan.common.annotations.conf.ConfProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ConfUtils.class);
    private static final Map<Class<?>, Class<?>> PRIMITIVE_CLASS_MAP = Map.of(
        Boolean.TYPE, Boolean.class, Byte.TYPE, Byte.class, Character.TYPE, Character.class,
        Short.TYPE, Short.class, Integer.TYPE, Integer.class, Long.TYPE, Long.class,
        Float.TYPE, Float.class, Double.TYPE, Double.class);

    private ConfUtils() {
        // utility class
    }

    public static <T extends Annotation> T loadConfiguration(final Class<T> confClass) {
        LOG.debug("Using default configuration of type {}", confClass);
        return loadConfiguration(confClass, new Properties());
    }

    public static <T extends Annotation> T loadConfiguration(final Class<T> confClass, final Path confPath) {
        LOG.info("Loading configuration of type {} from {}", confClass, confPath);
        final var filename = confClass.getAnnotation(ConfFile.class);
        final var props = confPath != null && Files.isDirectory(confPath) && filename != null
            ? loadProperties(confPath.resolve(filename.value())) : loadProperties(confPath);
        return loadConfiguration(confClass, props);
    }

    public static <T extends Annotation> T loadConfiguration(final Class<T> confClass, final String propsAsString) {
        final var props = new Properties();
        try (var in = new ByteArrayInputStream(propsAsString.getBytes(StandardCharsets.UTF_8))) {
            props.load(in);
        } catch (IOException e) {
            LOG.warn("Error loading properties from string input", e);
        }
        return loadConfiguration(confClass, props);
    }

    public static <T extends Annotation> T loadConfiguration(final Class<T> confClass, final Properties props) {
        final var propMap = Arrays.stream(confClass.getMethods())
            .filter(method -> method.isAnnotationPresent(ConfProperty.class))
            .collect(toUnmodifiableMap(Method::getName, method -> extractPropValue(method, props)));
        final var instance = Proxy.newProxyInstance(
            confClass.getClassLoader(),
            new Class<?>[]{confClass},
            (proxy, method, args) -> {
                final var methodName = method.getName();
                if (!propMap.containsKey(methodName)) {
                    throw new UnsupportedOperationException(
                        "Method %s has no configuration property associated".formatted(methodName));
                }
                return propMap.get(methodName);
            });
        return confClass.cast(instance);
    }

    private static Properties loadProperties(final Path filePath) {
        final var properties = new Properties();
        if (filePath != null && Files.isRegularFile(filePath)) {
            try (var in = new FileInputStream(filePath.toFile())) {
                properties.load(in);
            } catch (IOException e) {
                LOG.warn("Error loading properties from {} -> using default configuration", filePath, e);
            }
        }
        return properties;
    }

    private static Object extractPropValue(final Method method, final Properties loadedProps) {
        final var type = method.getReturnType();
        final var defaultValue = method.getDefaultValue();
        final var propKey = method.getAnnotation(ConfProperty.class).value();
        final var loadedValue = loadedProps.getProperty(propKey);
        LOG.trace("type: {}, loaded: {}, default: {}", type, loadedValue, defaultValue);

        if (loadedValue != null) {
            if (String.class.equals(type)) {
                return loadedValue;
            }
            final var targetType = type.isPrimitive() ? PRIMITIVE_CLASS_MAP.getOrDefault(type, type) : type;
            try {
                final var convertor = targetType.getMethod("valueOf", String.class);
                return convertor.invoke(null, loadedValue);
            } catch (ReflectiveOperationException | IllegalArgumentException e) {
                LOG.warn("Error parsing property {} of type {} -> falling back to default value", propKey, type, e);
            }
        }
        if (defaultValue == null) {
            throw new IllegalStateException("Configuration property %s has no value assigned.".formatted(propKey));
        }
        return defaultValue;
    }
}
