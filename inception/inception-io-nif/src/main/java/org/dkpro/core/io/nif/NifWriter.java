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
package org.dkpro.core.io.nif;

import java.io.OutputStream;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.dkpro.core.api.io.JCasFileWriter_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.parameter.MimeTypes;
import org.dkpro.core.io.nif.internal.DKPro2Nif;
import org.dkpro.core.io.nif.internal.ITS;
import org.dkpro.core.io.nif.internal.NIF;

import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Writer for the NLP Interchange Format (NIF).
 * 
 * @see <a href="http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html">NIF
 *      2.0 Core Ontology</a>
 */
@ResourceMetaData(name = "NLP Interchange Format (NIF) Writer")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_X_NIF_TURTLE })
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem",
        "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity" })
public class NifWriter
    extends JCasFileWriter_ImplBase
{
    /**
     * Specify the suffix of output files. Default value <code>.ttl</code>. The file format will be
     * chosen depending on the file suffice.
     * 
     * @see RDFLanguages
     */
    public static final String PARAM_FILENAME_EXTENSION = ComponentParameters.PARAM_FILENAME_EXTENSION;
    @ConfigurationParameter(name = PARAM_FILENAME_EXTENSION, mandatory = true, defaultValue = ".ttl")
    private String filenameSuffix;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException
    {
        OntModel model = ModelFactory.createOntologyModel();
        model.setNsPrefix(NIF.PREFIX_NIF, NIF.NS_NIF);
        model.setNsPrefix(ITS.PREFIX_ITS, ITS.NS_ITS);

        DKPro2Nif.convert(aJCas, model);

        try (OutputStream docOS = getOutputStream(aJCas, filenameSuffix)) {
            RDFDataMgr.write(docOS, model.getBaseModel(),
                    RDFLanguages.fileExtToLang(filenameSuffix));
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
}
