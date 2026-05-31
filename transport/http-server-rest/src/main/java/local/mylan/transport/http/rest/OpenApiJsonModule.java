/*
 * Copyright 2026 Ruslan Kashapov
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

import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import java.io.Serial;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * Open API model specific JSON serialization adopted for Jackson v3+.
 *
 * @see io.swagger.v3.core.util.ObjectMapperFactory
 */
class OpenApiJsonModule extends SimpleModule {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void setupModule(final JacksonModule.SetupContext context) {
        super.setupModule(context);
        context.addSerializerModifier(new ValueSerializerModifier() {

            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            public ValueSerializer<?> modifySerializer(final SerializationConfig config,
                final BeanDescription.Supplier beanDesc, final ValueSerializer<?> serializer) {

                final var beanClass = beanDesc.getBeanClass();
                if (Schema.class.isAssignableFrom(beanClass)) {
                    return new DelegateValueSerialzer<>(serializer, OpenApiJsonModule::serializeSchema);
                }
                if (MediaType.class.isAssignableFrom(beanClass)) {
                    return new DelegateValueSerialzer<>(serializer, OpenApiJsonModule::serializeMediaType);
                }
                if (Example.class.isAssignableFrom(beanClass)) {
                    return new DelegateValueSerialzer<>(serializer, OpenApiJsonModule::serializeExample);
                }
                return serializer;
            }
        });
    }

    private static class DelegateValueSerialzer<T> extends ValueSerializer<T> {
        private final ValueSerializer<T> delegator;
        private final DelegateSerializer<T> delegateSerializer;

        protected DelegateValueSerialzer(final ValueSerializer<?> delegator,
            final DelegateSerializer<T> delegateSerializer) {

            this.delegator = (ValueSerializer<T>) delegator;
            this.delegateSerializer = delegateSerializer;
        }

        @Override
        public void resolve(final SerializationContext ctxt) {
            delegator.resolve(ctxt);
        }

        @Override
        public void serialize(final T value, final JsonGenerator gen, final SerializationContext ctxt) {
            delegateSerializer.serialize(value, gen, ctxt, delegator);
        }
    }

    @FunctionalInterface
    interface DelegateSerializer<T> {

        void serialize(T value, JsonGenerator gen, SerializationContext ctxt, ValueSerializer<T> delegate);
    }

    // replicates io.swagger.v3.core.jackson.SchemaSerializer
    private static void serializeSchema(final Schema<?> schema, final JsonGenerator gen,
        final SerializationContext ctxt, final ValueSerializer<Schema<?>> delegator) {

        final var ref = schema.get$ref();
        if (ref != null && !ref.isEmpty()) {
            gen.writeStartObject();
            gen.writeStringProperty("$ref", ref);
            gen.writeEndObject();
        } else if (schema.getExampleSetFlag() && schema.getExample() == null) {
            gen.writeStartObject();
            delegator.unwrappingSerializer(null).serialize(schema, gen, ctxt);
            gen.writeNullProperty("example");
            gen.writeEndObject();
        } else {
            delegator.serialize(schema, gen, ctxt);
        }
    }

    // replicates io.swagger.v3.core.jackson.MediaTypeSerializer
    private static void serializeMediaType(final MediaType mediaType, final JsonGenerator gen,
        final SerializationContext ctxt, final ValueSerializer<MediaType> delegator) {
        if (mediaType.getExampleSetFlag() && mediaType.getExample() == null) {
            gen.writeStartObject();
            delegator.unwrappingSerializer(null).serialize(mediaType, gen, ctxt);
            gen.writeNullProperty("example");
            gen.writeEndObject();
        } else {
            delegator.serialize(mediaType, gen, ctxt);
        }
    }

    // replicates io.swagger.v3.core.jackson.ExampleSerializer
    private static void serializeExample(final Example example, final JsonGenerator gen,
        final SerializationContext ctxt, final ValueSerializer<Example> delegator) {

        if (example.getValueSetFlag() && example.getValue() == null) {
            gen.writeStartObject();
            delegator.unwrappingSerializer(null).serialize(example, gen, ctxt);
            gen.writeNullProperty("value");
            gen.writeEndObject();
        } else {
            delegator.serialize(example, gen, ctxt);
        }
    }
}

