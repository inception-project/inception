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

import static org.dkpro.core.api.resources.MappingProviderFactory.createPosMappingProvider;

import java.io.IOException;
import java.io.InputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.ComponentParameters;
import org.dkpro.core.api.parameter.MimeTypes;
import org.dkpro.core.api.resources.CompressionUtils;
import org.dkpro.core.api.resources.MappingProvider;
import org.dkpro.core.io.nif.internal.NIF;
import org.dkpro.core.io.nif.internal.Nif2DKPro;

import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Reader for the NLP Interchange Format (NIF). The file format (e.g. TURTLE, etc.) is automatically
 * chosen depending on the name of the file(s) being read. Compressed files are supported.
 */
@ResourceMetaData(name = "NLP Interchange Format (NIF) Reader")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_X_NIF_TURTLE })
@TypeCapability(outputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Heading",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Stem",
        "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity" })
public class NifReader
    extends JCasResourceCollectionReader_ImplBase
{
    /**
     * Use this part-of-speech tag set to use to resolve the tag set mapping instead of using the
     * tag set defined as part of the model meta data. This can be useful if a custom model is
     * specified which does not have such meta data, or it can be used in readers.
     */
    public static final String PARAM_POS_TAG_SET = ComponentParameters.PARAM_POS_TAG_SET;
    @ConfigurationParameter(name = PARAM_POS_TAG_SET, mandatory = false)
    private String posTagset;

    /**
     * Enable/disable type mapping.
     */
    public static final String PARAM_MAPPING_ENABLED = ComponentParameters.PARAM_MAPPING_ENABLED;
    @ConfigurationParameter(name = PARAM_MAPPING_ENABLED, mandatory = true, //
            defaultValue = ComponentParameters.DEFAULT_MAPPING_ENABLED)
    protected boolean mappingEnabled;

    /**
     * Load the part-of-speech tag to UIMA type mapping from this location instead of locating the
     * mapping automatically.
     */
    public static final String PARAM_POS_MAPPING_LOCATION = ComponentParameters.PARAM_POS_MAPPING_LOCATION;
    @ConfigurationParameter(name = PARAM_POS_MAPPING_LOCATION, mandatory = false)
    private String posMappingLocation;

    private MappingProvider posMappingProvider;

    private Resource res;
    private Model model;
    private StmtIterator contextIterator;
    private int inFileCount;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

        posMappingProvider = createPosMappingProvider(this, posMappingLocation, posTagset,
                getLanguage());

        // Seek first article
        try {
            step();
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        try {
            posMappingProvider.configure(aJCas.getCas());
        }
        catch (AnalysisEngineProcessException e) {
            throw new IOException(e);
        }

        initCas(aJCas, res);

        // FIXME The reader is already designed in such a way that multiple documents per NIF file
        // are supported. However, presently adding a qualifier to initCas would generate document
        // URIs in the NIF with two fragments, e.g. "urn:a01-cooked.ttl#0#offset_0_1234".
        // initCas(aJCas, res, String.valueOf(inFileCount));

        Statement context = contextIterator.next();

        Nif2DKPro converter = new Nif2DKPro();
        converter.setPosMappingProvider(posMappingProvider);
        converter.convert(context, aJCas);

        inFileCount++;
        step();
    }

    private void closeAll()
    {
        res = null;
        contextIterator = null;
    }

    @Override
    public void destroy()
    {
        closeAll();
        super.destroy();
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException
    {
        // If there is still an iterator, then there is still data. This requires that we call
        // step() already during initialization.
        return contextIterator != null;
    }

    /**
     * Seek article in file. Stop once article element has been found without reading it.
     */
    private void step() throws IOException
    {
        // Open next file
        while (true) {
            if (res == null) {
                // Call to super here because we want to know about the resources, not the articles
                if (getResourceIterator().hasNext()) {
                    // There are still resources left to read
                    res = nextFile();
                    inFileCount = 0;
                    try (InputStream is = CompressionUtils.getInputStream(res.getLocation(),
                            res.getInputStream())) {
                        model = ModelFactory.createOntologyModel();
                        RDFDataMgr.read(model, is, RDFLanguages.filenameToLang(
                                CompressionUtils.stripCompressionExtension(res.getLocation())));
                    }
                    contextIterator = model.listStatements(null, RDF.type,
                            model.createResource(NIF.TYPE_CONTEXT));
                }
                else {
                    // No more files to read
                    return;
                }
            }

            if (contextIterator.hasNext()) {
                return;
            }

            // End of file reached
            closeAll();
        }
    }
}
