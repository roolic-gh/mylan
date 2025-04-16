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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

final class AnnotationUtils {

    private AnnotationUtils() {
        // utility class
    }

    static List<Method> getAnnotatedMethods(Class<?> cls, final Class<? extends Annotation> annotationClass) {
        final var exclude = new HashSet<String>();
        final var result = new ArrayList<Method>(collectAnnotatedMethods(cls, annotationClass, exclude));
        Stream.of(cls.getInterfaces())
            .forEach(iface -> result.addAll(collectAnnotatedMethods(iface, annotationClass, exclude)));
        return List.copyOf(result);
    }

    private static List<Method> collectAnnotatedMethods(final Class<?> cls,
        final Class<? extends Annotation> annotationClass, final Set<String> exclude) {
        final var result = new ArrayList<Method>();
        for (var method : cls.getMethods()) {
            final var methodId = method.getName() + Stream.of(method.getParameterTypes()).map(Class::getName).toList();
            if (!exclude.contains(methodId) && method.getAnnotation(annotationClass) != null) {
                result.add(method);
                exclude.add(methodId);
            }
        }
        final var parent = cls.getSuperclass();
        if (parent != null && !Object.class.equals(parent)) {
            result.addAll(collectAnnotatedMethods(parent, annotationClass, exclude));
        }
        return result;
    }

    static <T extends Annotation> T getClassAnnotation(final Class<?> cls, final Class<T> annotationClass) {
        final var annotation = cls.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        final var parent = cls.getSuperclass();
        if (parent != null && !Object.class.equals(parent)) {
            final var parentAnnotation = getClassAnnotation(parent, annotationClass);
            if (parentAnnotation != null) {
                return parentAnnotation;
            }
        }
        for (var iface : cls.getInterfaces()) {
            var ifaceAnnotation = getClassAnnotation(iface, annotationClass);
            if (ifaceAnnotation != null) {
                return ifaceAnnotation;
            }
        }
        return null;
    }
}
