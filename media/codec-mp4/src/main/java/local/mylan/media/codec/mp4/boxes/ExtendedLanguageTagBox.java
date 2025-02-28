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
package local.mylan.media.codec.mp4.boxes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.apache.commons.lang3.LocaleUtils;

/**
 * Extended Language Tag Box. Addresses ISO/IEC 14496-12 (8.4.6 Extended Language Tag).
 */
public class ExtendedLanguageTagBox extends FullBox {

    /*
    8.4.6.3 Semantics
        extended_language -- is a NULL‐terminated C string containing an RFC 4646 (BCP 47) compliant
        language tag string, such as "en‐US", "fr‐FR", or "zh‐CN".
     */
    private Locale extendedLanguage;

    ExtendedLanguageTagBox(long offset, long length) {
        super(offset, length);
    }

    @Override
    public BoxType boxType() {
        return BoxType.ExtendedLanguageTag;
    }

    @Override
    void readContent(BoxReader reader) throws IOException {
        super.readContent(reader);
        extendedLanguage = LocaleUtils.toLocale(
            reader.readNullTerminatedString(limit, StandardCharsets.US_ASCII));
    }
}
