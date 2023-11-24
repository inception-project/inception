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
package de.tudarmstadt.ukp.inception.support.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YamlUtil
{
    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper(new YAMLFactory());
    }

    public static ObjectMapper getObjectMapper()
    {
        return OBJECT_MAPPER;
    }

    public static <T> T fromYamlStream(Class<T> aClass, InputStream aSrc) throws IOException
    {
        return getObjectMapper().readValue(aSrc, aClass);
    }

    public static String toYamlString(Object aObject) throws IOException
    {
        return toYamlString(getObjectMapper(), false, aObject);
    }

    public static String toYamlString(ObjectMapper aMapper, boolean aPretty, Object aObject)
        throws IOException
    {
        StringWriter out = new StringWriter();

        JsonGenerator yamlGenerator = aMapper.getFactory().createGenerator(out);
        // if (aPretty) {
        // yamlGenerator.setPrettyPrinter(new DefaultPrettyPrinter()
        // .withObjectIndenter(new DefaultIndenter().withLinefeed("\n")));
        // }

        yamlGenerator.writeObject(aObject);
        return out.toString();
    }
}
