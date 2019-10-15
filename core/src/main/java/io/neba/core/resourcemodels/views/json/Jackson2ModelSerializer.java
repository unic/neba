/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package io.neba.core.resourcemodels.views.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.neba.core.resourcemodels.mapping.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.stream;

/**
 * Writes arbitrary objects to JSON.
 */
class Jackson2ModelSerializer {
    private static final String SERIALIZATION_PREFIX = SerializationFeature.class.getSimpleName() + ".";
    private static final String MAPPER_PREFIX = MapperFeature.class.getSimpleName() + ".";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final JsonFactory jsonFactory;
    private final ObjectMapper mapper;

    Jackson2ModelSerializer(@Nonnull Supplier<Map<Object, Mapping<?>>> recordedMappingsSupplier, @Nonnull String[] jacksonConfigurations) {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JsonViewSupport(recordedMappingsSupplier));
        this.jsonFactory = new JsonFactory();

        stream(jacksonConfigurations)
                .map(config -> config.split("="))
                .forEach(setting -> {
                    if (setting.length != 2) {
                        logger.error("Invalid Jackson configuration {}, ignoring.", (Object) setting);
                        return;
                    }

                    try {
                        if (setting[0].startsWith(SERIALIZATION_PREFIX)) {
                            String enumName = setting[0].substring(SERIALIZATION_PREFIX.length());
                            SerializationFeature feature = SerializationFeature.valueOf(enumName);
                            mapper.configure(feature, Boolean.valueOf(setting[1]));
                            return;
                        }

                        if (setting[0].startsWith(MAPPER_PREFIX)) {
                            String enumName = setting[0].substring(MAPPER_PREFIX.length());
                            MapperFeature feature = MapperFeature.valueOf(enumName);
                            mapper.configure(feature, Boolean.valueOf(setting[1]));
                        }
                    } catch (Exception e) {
                        logger.error("Invalid Jackson configuration {}, ignoring.", (Object) setting);
                    }
                });
    }

    void serialize(@Nonnull PrintWriter writer, @Nonnull Object model) throws IOException {
        this.jsonFactory
                .createGenerator(writer)
                .setCodec(this.mapper)
                .writeObject(model);
    }
}
