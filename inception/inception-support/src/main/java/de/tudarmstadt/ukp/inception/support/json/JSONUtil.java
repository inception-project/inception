/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.support.json;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialects;

import de.tudarmstadt.ukp.inception.support.logging.LogMessage;
import tools.jackson.core.SerializableString;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SerializedString;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

public class JSONUtil
{
    private static final ObjectMapper JSON_MAPPER;

    static {
        JSON_MAPPER = JsonMapper.builder() //
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS) //
                // .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY) //
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES) //
                .enable(EnumFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE) //
                .disable(EnumFeature.READ_ENUMS_USING_TO_STRING) //
                .disable(EnumFeature.WRITE_ENUMS_USING_TO_STRING) //
                .enable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS) //
                .build();
    }

    private static final JsonMapper J3_MAPPER = JsonMapper.builder().build();
    private static final com.fasterxml.jackson.databind.ObjectMapper J2_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    public static com.fasterxml.jackson.databind.JsonNode adaptJackson3To2(JsonNode aNode)
    {
        try {
            var json = J3_MAPPER.writeValueAsString(aNode);
            return J2_MAPPER.readTree(json);
        }
        catch (Exception e) {
            throw new RuntimeException("Conversion failed", e);
        }
    }

    public static ObjectNode adaptJackson2To3(com.fasterxml.jackson.databind.JsonNode aNode)
    {
        try {
            var json = J2_MAPPER.writeValueAsString(aNode);
            return (ObjectNode) J3_MAPPER.readTree(json);
        }
        catch (Exception e) {
            throw new RuntimeException("Conversion failed", e);
        }
    }

    /**
     * Convert Java objects into JSON format and write it to a file
     * 
     * @param aMapper
     *            the object mapper to be used
     * @param aObject
     *            the object.
     * @param aFile
     *            the file
     * @throws IOException
     *             if an I/O error occurs.
     */
    public static void generatePrettyJson(ObjectMapper aMapper, Object aObject, File aFile)
        throws IOException
    {
        FileUtils.writeStringToFile(aFile, toPrettyJsonString(aMapper, aObject), "UTF-8");
    }

    public static void generatePrettyJson(Object aObject, File aFile) throws IOException
    {
        FileUtils.writeStringToFile(aFile, toPrettyJsonString(aObject), "UTF-8");
    }

    public static String toPrettyJsonString(ObjectMapper aMapper, Object aObject) throws IOException
    {
        return toJsonString(aMapper, true, aObject);
    }

    public static String toJsonString(Object aObject) throws IOException
    {
        return toJsonString(getObjectMapper(), false, aObject);
    }

    public static String toJsonString(ObjectMapper aMapper, boolean aPretty, Object aObject)
        throws IOException
    {
        if (aPretty) {
            return aMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aObject);
        }
        return aMapper.writeValueAsString(aObject);
    }

    public static <T> T fromJsonString(Class<T> aClass, String aJSON) throws IOException
    {
        if (aJSON == null) {
            return null;
        }

        return getObjectMapper().readValue(aJSON, aClass);
    }

    public static <T> T fromValidatedJsonString(Class<T> aClass, String aJSON, Schema aSchema)
        throws IOException
    {
        if (aJSON == null) {
            return null;
        }

        var mapper = getObjectMapper();
        var jsonNode = mapper.readTree(aJSON);
        var errors = aSchema.validate(jsonNode);
        if (!errors.isEmpty()) {
            throw new IOException(
                    "JSON does not match JSON schema: " + errors.iterator().next().getMessage());
        }

        return mapper.treeToValue(jsonNode, aClass);
    }

    public static <T> T fromJsonStream(Class<T> aClass, InputStream aSrc) throws IOException
    {
        return getObjectMapper().readValue(aSrc, aClass);
    }

    public static String toPrettyJsonString(Object aObject) throws IOException
    {
        return toPrettyJsonString(getObjectMapper(), aObject);
    }

    public static ObjectMapper getObjectMapper()
    {
        return JSON_MAPPER;
    }

    public static String toInterpretableJsonString(Object aObject) throws IOException
    {
        var out = new StringWriter();
        try (var jsonGenerator = JSONUtil.getObjectMapper().createGenerator(out)) {
            jsonGenerator.setCharacterEscapes(JavaScriptCharacterEscapes.get());
            jsonGenerator.writePOJO(aObject);
        }
        return out.toString();
    }

    public static String toInterpretableJsonString(JsonNode aTree) throws IOException
    {
        var out = new StringWriter();
        try (var jsonGenerator = JSONUtil.getObjectMapper().createGenerator(out)) {
            jsonGenerator.setCharacterEscapes(JavaScriptCharacterEscapes.get());
            jsonGenerator.writeTree(aTree);
        }
        return out.toString();
    }

    public static Schema loadJsonSchema(InputStream aSource)
    {
        return SchemaRegistry.withDialect(Dialects.getDraft202012()).getSchema(aSource);
    }

    public static List<LogMessage> validateJsonString(URL aSchemaUrl, String aJsonString)
        throws IOException
    {
        try (var schemaStream = aSchemaUrl.openStream()) {
            var jsonNode = getObjectMapper().readTree(aJsonString);
            var jsonSchema = loadJsonSchema(schemaStream);
            return jsonSchema.validate(jsonNode).stream() //
                    .map(e -> LogMessage.error(JSONUtil.class, "%s", e.getMessage())) //
                    .collect(toList());
        }
    }

    private static class JavaScriptCharacterEscapes
        extends CharacterEscapes
    {
        private static final long serialVersionUID = -2189758484099286957L;
        private final int[] asciiEscapes = standardAsciiEscapesForJSON();

        public static final JavaScriptCharacterEscapes INSTANCE = new JavaScriptCharacterEscapes();

        public static JavaScriptCharacterEscapes get()
        {
            return INSTANCE;
        }

        @Override
        public SerializableString getEscapeSequence(int aCh)
        {
            switch (aCh) {
            case '\u2028':
                return new SerializedString("\\u2028");
            case '\u2029':
                return new SerializedString("\\u2029");
            default:
                return null;
            }
        }

        @Override
        public int[] getEscapeCodesForAscii()
        {
            return asciiEscapes;
        }
    }
}
