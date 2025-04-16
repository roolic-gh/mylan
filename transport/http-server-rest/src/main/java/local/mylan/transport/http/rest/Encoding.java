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

import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_XML;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;

enum Encoding {
    XML(APPLICATION_XML, new XmlMapper().configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true)),
    JSON(APPLICATION_JSON, new ObjectMapper());

    private final ObjectMapper objectMapper;
    private final CharSequence mediaType;

    Encoding(final CharSequence mediaType, final ObjectMapper objectMapper) {
        this.mediaType = mediaType;
        this.objectMapper = objectMapper;
    }

    ObjectMapper objectMapper() {
        return objectMapper;
    }

    CharSequence mediaType() {
        return mediaType;
    }

    static Encoding fromMediaType(final CharSequence mediaType) {
        return XML.mediaType.toString().equals(mediaType) ? XML : JSON;
    }

}
