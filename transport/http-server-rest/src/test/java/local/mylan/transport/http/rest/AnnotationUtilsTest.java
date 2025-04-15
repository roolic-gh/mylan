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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Method;
import java.util.function.Function;
import java.util.stream.Collectors;
import local.mylan.common.annotations.rest.RequestMapping;
import local.mylan.common.annotations.rest.ServiceDescriptor;
import org.junit.jupiter.api.Test;

class AnnotationUtilsTest {
    private static final String PATH_TOP = "/path/top";
    private static final String PATH_INTERFACE = "/path/if";
    private static final String PATH_ABSTRACT = "/path/abs";
    private static final String PATH_PARENT = "/path/parent";
    private static final String PATH_FINAL = "/path/final";
    private static final String METHOD = "GET";

    @Test
    void methodsAnnotated() {
        final var methods = AnnotationUtils.getAnnotatedMethods(TestFinal.class, RequestMapping.class);
        assertNotNull(methods);
        assertFalse(methods.isEmpty());
        final var byName = methods.stream().collect(Collectors.toMap(Method::getName, Function.identity()));
        assertMethodAnnotation(byName.get("method0"), PATH_TOP);
        assertMethodAnnotation(byName.get("method1"), PATH_INTERFACE);
        assertNull(byName.get("method2")); // has no annotation
        assertMethodAnnotation(byName.get("method3"), PATH_ABSTRACT);
        assertMethodAnnotation(byName.get("method4"), PATH_PARENT);
        assertMethodAnnotation(byName.get("method5"), PATH_PARENT);
        assertMethodAnnotation(byName.get("method6"), PATH_FINAL);
    }

    @Test
    void classAnnotation() {
        final var annotation = AnnotationUtils.getClassAnnotation(TestFinal.class, ServiceDescriptor.class);
        assertNotNull(annotation);
        assertEquals("TEST", annotation.id());
    }

    private static void assertMethodAnnotation(final Method method, final String expectedPathValue) {
        assertNotNull(method);
        final var annotation = method.getAnnotation(RequestMapping.class);
        assertNotNull(annotation);
        assertEquals(expectedPathValue, annotation.path());
    }

    @ServiceDescriptor(id = "TEST")
    interface TestTop {
        @RequestMapping(path = PATH_TOP, method = METHOD)
        default void method0(String param) {
        }
    }

    interface TestInterface extends TestTop {

        @RequestMapping(path = PATH_INTERFACE, method = METHOD)
        void method1(String param);

        void method2(String param);
    }

    abstract static class TestAbstract implements TestInterface {

        @Override
        public void method2(String param1) {
        }

        @RequestMapping(path = PATH_ABSTRACT, method = METHOD)
        public void method3(String param) {
        }

        @RequestMapping(path = PATH_ABSTRACT, method = METHOD)
        public abstract void method4(String param);
    }

    static class TestParent extends TestAbstract {

        @Override
        public void method1(final String param) {
        }

        @RequestMapping(path = PATH_PARENT, method = METHOD)
        @Override
        public void method4(final String param) {
        }

        @RequestMapping(path = PATH_PARENT, method = METHOD)
        public void method5(final String param) {
        }
    }

    static final class TestFinal extends TestParent {

        @RequestMapping(path = PATH_FINAL, method = METHOD)
        public void method6(final String param) {
        }
    }
}
