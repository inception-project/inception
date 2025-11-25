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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.casdoctor;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.UNMANAGED_NON_INITIALIZING_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.api.export.DocumentImportExportService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.documents.api.DocumentStorageService;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public abstract class CasDoctorTask_ImplBase
    extends Task
    implements ProjectTask
{
    private @Autowired CasStorageService casStorageService;
    private @Autowired DocumentStorageService documentStorageService;
    private @Autowired DocumentImportExportService importExportService;

    private final List<LogMessageSet> messageSets = new ArrayList<>();

    public CasDoctorTask_ImplBase(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withCancellable(true).withScope(PROJECT));
    }

    public List<LogMessageSet> getMessageSets()
    {
        return messageSets;
    }

    protected CAS createOrReadInitialCasWithoutSavingOrChecks(SourceDocument aDocument,
            LogMessageSet aMessageSet)
        throws IOException, UIMAException
    {
        if (casStorageService.existsCas(aDocument, INITIAL_SET)) {
            return casStorageService.readCas(aDocument, INITIAL_SET,
                    UNMANAGED_NON_INITIALIZING_ACCESS);
        }

        var cas = importExportService.importCasFromFileNoChecks(
                documentStorageService.getSourceDocumentFile(aDocument), aDocument);
        aMessageSet.add(
                LogMessage.info(getClass(), "Created initial CAS for [%s]", aDocument.getName()));
        return cas;
    }

    protected void noticeIfThereAreNoMessages(LogMessageSet aSet)
    {
        if (aSet.getMessages().isEmpty()) {
            aSet.add(LogMessage.info(getClass(), "Nothing to report."));
        }
    }

    protected static class Builder<T extends Builder<?>>
        extends Task.Builder<T>
    {
        protected Builder()
        {
        }
    }
}
