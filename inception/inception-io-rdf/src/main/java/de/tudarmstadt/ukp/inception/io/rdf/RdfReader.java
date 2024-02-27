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
package de.tudarmstadt.ukp.inception.io.rdf;

import static org.dkpro.core.api.resources.CompressionUtils.getInputStream;
import java.io.IOException;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.vocabulary.RDF;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.MimeTypeCapability;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import org.dkpro.core.api.parameter.MimeTypes;
import org.dkpro.core.api.resources.CompressionUtils;
import de.tudarmstadt.ukp.inception.io.rdf.internal.Rdf2Uima;
import de.tudarmstadt.ukp.inception.io.rdf.internal.RdfCas;
import eu.openminted.share.annotations.api.DocumentationResource;

/**
 * Reads a CAS serialized as RDF.
 */
@ResourceMetaData(name = "UIMA CAS RDF Reader")
@DocumentationResource("${docbase}/format-reference.html#format-${command}")
@MimeTypeCapability({ MimeTypes.APPLICATION_X_UIMA_RDF })
public class RdfReader
    extends JCasResourceCollectionReader_ImplBase
{
    private Resource res;
    private Model model;
    private StmtIterator contextIterator;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException
    {
        super.initialize(aContext);

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
        initCas(aJCas, res);

        var context = contextIterator.next();
        try {
            Rdf2Uima.convert(context, aJCas);
        }
        catch (CASException e) {
            throw new CollectionException(e);
        }

        // inFileCount++;
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
                    // inFileCount = 0;
                    try (var is = getInputStream(res.getLocation(), res.getInputStream())) {
                        model = ModelFactory.createOntologyModel();
                        RDFDataMgr.read(model, is, RDFLanguages.filenameToLang(
                                CompressionUtils.stripCompressionExtension(res.getLocation())));
                    }
                    contextIterator = model.listStatements(null, RDF.type,
                            model.createResource(RdfCas.TYPE_VIEW));
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
