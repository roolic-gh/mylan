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

final class AnnotationUtils {

    private AnnotationUtils() {
        // utility class
    }

    static List<Method> getAnnotatedMethods(Class<?> cls, final Class<? extends Annotation> annotation) {
        final var originClasses = new HashSet<Class<?>>(List.of(cls.getInterfaces()));
        final var result = new ArrayList<Method>();
        for (var method : cls.getMethods()) {
            if (method.getAnnotation(annotation) != null) {
                result.add(method);
            } else {
                final var origin = method.getDeclaringClass();
                if (!cls.equals(origin) && !Object.class.equals(origin)) {
                    originClasses.add(origin);
                }
            }
        }
        // TODO proper iteration over classes
        originClasses.forEach(origin -> result.addAll(getAnnotatedMethods(origin, annotation)));
        return List.copyOf(result);
    }
}
