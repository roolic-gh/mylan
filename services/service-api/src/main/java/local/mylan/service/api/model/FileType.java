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
package local.mylan.service.api.model;

import static java.util.Objects.requireNonNull;

import java.util.Locale;
import java.util.Set;

public enum FileType {
    VIDEO(Set.of("mp4", "mkv")),
    AUDIO(Set.of("mp3")),
    IMAGE(Set.of("jpg", "jpeg", "png", "gif")),
    COMIC_BOOK(Set.of("cbr", "cbz")),
    OTHER(Set.of());

    private final Set<String> extentions;

    FileType(final Set<String> extentions) {
        this.extentions = extentions;
    }

    static FileType fromFilename(final String filename) {
        requireNonNull(filename);
        final var idx = filename.lastIndexOf('.');
        final var ext = idx > 0 && idx < filename.length() - 2
            ? filename.substring(idx + 1).toLowerCase(Locale.US) : "";
        if (!ext.isEmpty()) {
            for( var ft: values()){
                if(ft.extentions.contains(ext)){
                    return ft;
                }
            }
        }
        return OTHER;
    }

}
