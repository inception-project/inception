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
package de.tudarmstadt.ukp.clarin.webanno.support;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.FileUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.SerializableString;
import com.fasterxml.jackson.core.io.CharacterEscapes;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JSONUtil
{
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
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
        StringWriter out = new StringWriter();

        JsonGenerator jsonGenerator = aMapper.getFactory().createGenerator(out);
        if (aPretty) {
            jsonGenerator.setPrettyPrinter(new DefaultPrettyPrinter()
                    .withObjectIndenter(new DefaultIndenter().withLinefeed("\n")));
        }

        jsonGenerator.writeObject(aObject);
        return out.toString();
    }

    public static <T> T fromJsonString(Class<T> aClass, String aJSON) throws IOException
    {
        if (aJSON == null) {
            return null;
        }
        else {
            return getObjectMapper().readValue(aJSON, aClass);
        }
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
        return OBJECT_MAPPER;
    }

    public static String toInterpretableJsonString(Object aObject) throws IOException
    {
        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = JSONUtil.getObjectMapper().getFactory().createGenerator(out);
        jsonGenerator.setCharacterEscapes(JavaScriptCharacterEscapes.get());
        jsonGenerator.writeObject(aObject);
        return out.toString();
    }

    public static String toInterpretableJsonString(JsonNode aTree) throws IOException
    {
        StringWriter out = new StringWriter();
        JsonGenerator jsonGenerator = JSONUtil.getObjectMapper().getFactory().createGenerator(out);
        jsonGenerator.setCharacterEscapes(JavaScriptCharacterEscapes.get());
        jsonGenerator.writeTree(aTree);
        return out.toString();
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
