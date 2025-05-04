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

import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.Map;

public final class ConfRegistry {
    public static final ConfRegistry INSTANCE = new ConfRegistry();

    private Path confDir;
    private Map<Class<? extends Annotation>, Object> confMap;

    private ConfRegistry() {
        // sigleton
    }

    public void setConfDir(final Path confDir) {
        this.confDir = confDir;
    }

    public <T extends Annotation> T getConfiguration(final Class<T> confClass) {
        return confClass.cast(
            confMap.computeIfAbsent(confClass, key -> ConfUtils.loadConfiguration(key, confDir))
        );
    }
}
