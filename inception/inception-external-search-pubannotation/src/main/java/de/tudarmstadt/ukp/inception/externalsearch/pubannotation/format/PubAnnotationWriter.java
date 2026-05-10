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
package de.tudarmstadt.ukp.inception.externalsearch.pubannotation.format;

import java.io.IOException;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;

/**
 * Writes a {@link JCas} as a PubAnnotation JSON document. Inverse of
 * {@link PubAnnotationSectionsReader}-but-for-annotations.
 */
public class PubAnnotationWriter
    extends JCasFileWriter_ImplBase
{
    public static final String PARAM_SPAN_TYPES = "spanTypes";
    @ConfigurationParameter(name = PARAM_SPAN_TYPES, mandatory = true, defaultValue = {})
    private Set<String> spanTypes;

    public static final String PARAM_RELATION_TYPES = "relationTypes";
    @ConfigurationParameter(name = PARAM_RELATION_TYPES, mandatory = true, defaultValue = {})
    private Set<String> relationTypes;

    public static final String PARAM_SOURCEDB = "sourcedb";
    @ConfigurationParameter(name = PARAM_SOURCEDB, mandatory = false)
    private String sourcedb;

    public static final String PARAM_SOURCEID = "sourceid";
    @ConfigurationParameter(name = PARAM_SOURCEID, mandatory = false)
    private String sourceid;

    public static final String PARAM_PRETTY = "prettyPrint";
    @ConfigurationParameter(name = PARAM_PRETTY, mandatory = true, defaultValue = "true")
    private boolean prettyPrint;

    /**
     * Emit the simple name of a type when it is unambiguous in the source CAS instead of the
     * fully-qualified name. Matches PubAnnotation conventions (TextAE etc.) and stays
     * round-trip-safe via the importer's suffix-match step. Falls back to FQN when ambiguous.
     */
    public static final String PARAM_SHORT_TYPE_NAMES = "shortTypeNames";
    @ConfigurationParameter(name = PARAM_SHORT_TYPE_NAMES, mandatory = true, defaultValue = "true")
    private boolean shortTypeNames;

    public static final String FILENAME_SUFFIX = ".json";

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        var converter = new CasToPubAnnotationConverter(spanTypes, relationTypes, shortTypeNames);
        var doc = converter.convert(aJCas.getCas(), sourcedb, sourceid);

        try (var os = getOutputStream(aJCas, FILENAME_SUFFIX)) {
            var mapper = new ObjectMapper();
            if (prettyPrint) {
                mapper.writer().with(SerializationFeature.INDENT_OUTPUT).writeValue(os, doc);
            }
            else {
                mapper.writeValue(os, doc);
            }
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
