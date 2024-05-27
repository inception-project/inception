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

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.TypeSystemUtil;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.api.format.FormatSupport;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.schema.service.AnnotationSchemaServiceImpl;

public class UimaJsonCasFormatSupport
    implements FormatSupport
{
    public static final String ID = "jsoncas";
    public static final String NAME = "UIMA CAS JSON 0.4.0";

    private final DocumentImportExportService documentImportExportService;

    @Autowired
    public UimaJsonCasFormatSupport(DocumentImportExportService aDocumentImportExportService)
    {
        documentImportExportService = aDocumentImportExportService;
    }

    @Override
    public String getId()
    {
        return ID;
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public boolean isReadable()
    {
        return true;
    }

    @Override
    public boolean isWritable()
    {
        return true;
    }

    @Override
    public boolean isProneToInconsistencies()
    {
        return true;
    }

    @Override
    public AnalysisEngineDescription getWriterDescription(Project aProject,
            TypeSystemDescription aTSD, CAS aCAS)
        throws ResourceInitializationException
    {
        return createEngineDescription(UimaJsonCasWriter.class, aTSD);
    }

    @Override
    public void read(Project aProject, CAS aCas, File aFile)
        throws ResourceInitializationException, IOException, CollectionException
    {
        // We need to perform a little hack here because the JSON CAS IO does not support lenient
        // de-serialization yet. INCEpTION includes some types in exports that are specific only to
        // exports. They are usually discarded by the lenient XMI import, but here they bite us.
        var tsd = mergeTypeSystems(asList( //
                TypeSystemUtil.typeSystem2TypeSystemDescription(aCas.getTypeSystem()),
                documentImportExportService.getExportSpecificTypes()));

        upgradeCas(aCas, tsd);

        FormatSupport.super.read(aProject, aCas, aFile);

        // No need to go back to the original CAS TS because another upgrade happens after
        // project import anyway.
    }

    private void upgradeCas(CAS aCas, TypeSystemDescription tsd)
        throws IOException, ResourceInitializationException, CollectionException
    {
        try {
            AnnotationSchemaServiceImpl._upgradeCas(aCas, aCas, tsd);
        }
        catch (ResourceInitializationException e) {
            throw e;
        }
    }

    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription(UimaJsonCasReader.class, aTSD);
    }
}
