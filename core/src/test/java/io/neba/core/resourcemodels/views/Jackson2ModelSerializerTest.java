package io.neba.core.resourcemodels.views;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;


public class Jackson2ModelSerializerTest {

    @Test
    public void name() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory jsonFactory = new JsonFactory();

        StringWriter writer = new StringWriter();


        TestModel model = new TestModel();
        JsonGenerator generator = jsonFactory
                .createGenerator(writer)
                .setCodec(mapper);
        generator
                .writeStartObject(model);
        generator.writeEmbeddedObject(model);
        generator.writeEndObject();

        assertThat(writer.toString()).isEqualTo("{\"testProperty\":\"Hello, world\"}");
    }


    private static class TestModel {
        private String testProperty = "Hello, world";

        public String getTestProperty() {
            return testProperty;
        }
    }
}
