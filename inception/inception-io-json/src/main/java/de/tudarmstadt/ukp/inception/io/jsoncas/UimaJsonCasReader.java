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
package de.tudarmstadt.ukp.inception.io.jsoncas;

import static com.fasterxml.jackson.core.JsonToken.FIELD_NAME;
import static com.fasterxml.jackson.core.JsonToken.START_ARRAY;
import static com.fasterxml.jackson.core.JsonToken.START_OBJECT;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.FEATURE_STRUCTURES_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.HEADER_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.TYPES_FIELD;
import static org.apache.uima.json.jsoncas2.JsonCas2Names.VIEWS_FIELD;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.json.jsoncas2.JsonCas2Deserializer;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

import com.fasterxml.jackson.core.JsonFactory;

/**
 * UIMA JSON CAS format reader.
 */
public class UimaJsonCasReader
    extends JCasResourceCollectionReader_ImplBase
{
    private static final List<String> TOP_LEVEL_FIELDS = List.of(HEADER_FIELD, TYPES_FIELD,
            VIEWS_FIELD, FEATURE_STRUCTURES_FIELD);

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        try (var is = res.getInputStream()) {
            validateTopLevelStructure(is);
        }

        var jds = new JsonCas2Deserializer();
        jds.setFsMode(FeatureStructuresMode.AS_ARRAY);

        try (var is = res.getInputStream()) {
            jds.deserialize(is, aJCas.getCas());
        }
    }

    /**
     * The deserializer does not terminate when it meets an unknown top-level field or a top-level
     * field whose value is not an object or array (e.g. {@code "%VIEWS": null}), and it silently
     * ignores anything after the first top-level object. So we check the top-level structure in a
     * separate streaming pass before handing the data to the deserializer.
     */
    static void validateTopLevelStructure(InputStream aInputStream) throws IOException
    {
        try (var parser = new JsonFactory().createParser(aInputStream)) {
            if (parser.nextToken() != START_OBJECT) {
                throw new IOException(
                        "Not a UIMA CAS JSON document: the data must start with a JSON object");
            }

            while (parser.nextToken() == FIELD_NAME) {
                var fieldName = parser.currentName();
                if (!TOP_LEVEL_FIELDS.contains(fieldName)) {
                    throw new IOException(
                            "Not a UIMA CAS JSON document: unexpected top-level field [" + fieldName
                                    + "], expected only " + TOP_LEVEL_FIELDS);
                }

                var expected = FEATURE_STRUCTURES_FIELD.equals(fieldName) ? START_ARRAY
                        : START_OBJECT;
                if (parser.nextToken() != expected) {
                    throw new IOException("Not a UIMA CAS JSON document: top-level field ["
                            + fieldName + "] must be a JSON "
                            + (expected == START_ARRAY ? "array" : "object"));
                }

                parser.skipChildren();
            }

            if (parser.nextToken() != null) {
                throw new IOException("Not a UIMA CAS JSON document: the data contains more than "
                        + "one JSON value, but a UIMA CAS JSON file must contain exactly one");
            }
        }
    }
}
