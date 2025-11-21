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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import local.mylan.common.annotations.conf.ConfFile;
import local.mylan.common.annotations.conf.ConfProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ConfUtilsTest {

    private static final String STR_PROP_KEY = "test.prop.str";
    private static final String STR_PROP_DEFAULT = "str-default";
    private static final String STR_PROP_CUSTOM = "str-custom";

    private static final String INT_PROP_KEY = "test.prop.int";
    private static final int INT_PROP_DEFAULT = 101;
    private static final int INT_PROP_CUSTOM = 202;

    private static final String FLOAT_PROP_KEY = "test.prop.float";
    private static final float FLOAT_PROP_DEFAULT = 1.1f;
    private static final float FLOAT_PROP_CUSTOM = 2.2f;

    private static final String BOOL_PROP_KEY = "test.prop.bool";
    private static final boolean BOOL_PROP_DEFAULT = false;
    private static final boolean BOOL_PROP_CUSTOM = true;

    private static final String ENUM_PROP_KEY = "test.prop.enum";
    private static final String CUSTOM_PROP_FILENAME = "custom.properties";

    private static final String PARTIAL_CONTENT =  """
            test.prop.int = 202
            test.prop.float = 2.2""";

    private static final String CUSTOM_CONTENT = """
            test.prop.str = str-custom
            test.prop.int = 202
            test.prop.float = 2.2
            test.prop.bool = true
            test.prop.enum = CUSTOM""";

    @TempDir
    static Path confDir;

    @Test
    void confDefaults() {
        assertAllDefaults(ConfUtils.loadConfiguration(TestConfig.class));
    }

    @ParameterizedTest
    @MethodSource
    void loadConfDefaults(final Path filePath) {
        assertAllDefaults(ConfUtils.loadConfiguration(TestConfig.class, filePath));
    }

    private static Stream<Path> loadConfDefaults() throws IOException {
        final var noFile = confDir.resolve("non-existent.properties");
        final var emptyFile = createConfFile("empty.properties", "");
        return Stream.of(null, noFile, emptyFile);
    }

    private static void assertAllDefaults(final TestConfig conf) {
        assertNotNull(conf);
        assertEquals(STR_PROP_DEFAULT, conf.stingProp());
        assertEquals(INT_PROP_DEFAULT, conf.intProp());
        assertEquals(FLOAT_PROP_DEFAULT, conf.floatProp());
        assertEquals(BOOL_PROP_DEFAULT, conf.booleanProp());
        assertEquals(TestEnum.DEFAULT, conf.enumProp());
    }

    @Test
    void readConfCustom() throws IOException {
        assertAllCustom(ConfUtils.loadConfiguration(TestConfig.class, CUSTOM_CONTENT));
    }

    @ParameterizedTest
    @MethodSource
    void loadConfCustom(final Path confPath) throws IOException {
        createConfFile(CUSTOM_PROP_FILENAME, CUSTOM_CONTENT);
        assertAllCustom(ConfUtils.loadConfiguration(TestConfig.class, confPath));
    }

    private static Stream<Path> loadConfCustom(){
        return Stream.of(confDir, confDir.resolve(CUSTOM_PROP_FILENAME));
    }

    private static void assertAllCustom(final TestConfig conf) {
        assertNotNull(conf);
        assertEquals(STR_PROP_CUSTOM, conf.stingProp());
        assertEquals(INT_PROP_CUSTOM, conf.intProp());
        assertEquals(FLOAT_PROP_CUSTOM, conf.floatProp());
        assertEquals(BOOL_PROP_CUSTOM, conf.booleanProp());
        assertEquals(TestEnum.CUSTOM, conf.enumProp());
    }

    @Test
    void readConfPartialOverride() throws IOException {
        assertPartial(ConfUtils.loadConfiguration(TestConfig.class, PARTIAL_CONTENT));
    }

    @Test
    void loadConfPartialOverride() throws IOException {
        final var confPath = createConfFile("partial.properties", PARTIAL_CONTENT);
        assertPartial(ConfUtils.loadConfiguration(TestConfig.class, confPath));
    }

    private static void assertPartial(final TestConfig conf) {
        assertNotNull(conf);
        assertEquals(STR_PROP_DEFAULT, conf.stingProp());
        assertEquals(INT_PROP_CUSTOM, conf.intProp());
        assertEquals(FLOAT_PROP_CUSTOM, conf.floatProp());
        assertEquals(BOOL_PROP_DEFAULT, conf.booleanProp());
        assertEquals(TestEnum.DEFAULT, conf.enumProp());
    }

    @Test
    void loadConfParseErrorToDefaults() throws IOException {
        final var confPath = createConfFile("non-parseable.properties", """
            test.prop.int = not-integer
            test.prop.enum = not-enum""");
        final var conf = ConfUtils.loadConfiguration(TestConfig.class, confPath);
        assertNotNull(conf);
        assertEquals(INT_PROP_DEFAULT, conf.intProp());
        assertEquals(TestEnum.DEFAULT, conf.enumProp());
    }

    @Test
    void loadConfWithMandatoryProp() throws IOException {
        final var confPath = createConfFile("mandatory-configured.properties", "mandatory.prop = configured");
        final var conf = ConfUtils.loadConfiguration(TestConfigNodDefaults.class, confPath);
        assertEquals("configured", conf.mandatoryProp());

        assertThrows(IllegalStateException.class,
            ()-> ConfUtils.loadConfiguration(TestConfigNodDefaults.class, (Path) null));
    }

    @Test
    void loadConfNoAnnotations() {
        final var conf = ConfUtils.loadConfiguration(TestConfigNoAnnotation.class, (Path) null);
        assertThrows(UnsupportedOperationException.class, conf::noAnnotationProp);
    }

    private static Path createConfFile(final String fileName, final String content) throws IOException {
        final var path = confDir.resolve(fileName);
        Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE);
        return path;
    }

    @ConfFile(CUSTOM_PROP_FILENAME)
    private @interface TestConfig {

        @ConfProperty(STR_PROP_KEY)
        String stingProp() default STR_PROP_DEFAULT;

        @ConfProperty(INT_PROP_KEY)
        int intProp() default INT_PROP_DEFAULT;

        @ConfProperty(FLOAT_PROP_KEY)
        float floatProp() default FLOAT_PROP_DEFAULT;

        @ConfProperty(BOOL_PROP_KEY)
        boolean booleanProp() default BOOL_PROP_DEFAULT;

        @ConfProperty(ENUM_PROP_KEY)
        TestEnum enumProp() default TestEnum.DEFAULT;
    }

    private @interface TestConfigNodDefaults {
        @ConfProperty("mandatory.prop")
        String mandatoryProp();
    }

    private @interface TestConfigNoAnnotation {
        String noAnnotationProp() default "default";
    }

    private enum TestEnum {
        CUSTOM, DEFAULT
    }
}
