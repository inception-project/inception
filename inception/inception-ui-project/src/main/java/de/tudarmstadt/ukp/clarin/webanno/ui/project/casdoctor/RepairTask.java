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
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.CURATION_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet.INITIAL_SET;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static java.util.Arrays.asList;

import java.io.FileNotFoundException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasStorageService;
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctorImpl;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class RepairTask
    extends CasDoctorTask_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "RepairTask";

    private @Autowired RepairsRegistry repairsRegistry;
    private @Autowired ChecksRegistry checksRegistry;
    private @Autowired DocumentService documentService;
    private @Autowired CasStorageService casStorageService;

    private final List<String> repairs;

    public RepairTask(Builder<? extends Builder<?>> aBuilder)
    {
        // Can currently not be cancelled because we're running it sync and the AJAX cancel request
        // won't get through
        super(aBuilder.withType(TYPE).withCancellable(false).withScope(PROJECT));

        repairs = aBuilder.repairs;
    }

    @Override
    public String getTitle()
    {
        return "Running CAS Doctor repairs...";
    }

    @Override
    public void execute()
    {
        var casDoctor = new CasDoctorImpl(checksRegistry, repairsRegistry);
        casDoctor.setFatalChecks(false);
        casDoctor.setActiveRepairs(repairs.toArray(String[]::new));

        var project = getProject();

        var sourceDocuments = documentService.listSourceDocuments(project);

        try (var progress = getMonitor().openScope("documents", sourceDocuments.size())) {
            for (var sd : sourceDocuments) {
                if (getMonitor().isCancelled()) {
                    return;
                }

                progress.update(up -> up.increment() //
                        .addMessage(LogMessage.info(this, "Processing [%s]...", sd.getName())));

                // Repair INITIAL CAS
                {
                    var messageSet = new LogMessageSet(sd.getName() + " [INITIAL]");

                    try {
                        casStorageService.forceActionOnCas(sd, INITIAL_SET,
                                (doc, set) -> createOrReadInitialCasWithoutSavingOrChecks(doc,
                                        messageSet),
                                (doc, set, cas) -> casDoctor.repair(doc, set.id(), cas,
                                        messageSet.getMessages()), //
                                true);
                    }
                    catch (Exception e) {
                        messageSet.add(LogMessage.error(getClass(),
                                "Error repairing initial CAS for [%s]: %s", sd.getName(),
                                e.getMessage()));
                        LOG.error("Error repairing initial CAS for [{}]", sd.getName(), e);
                    }

                    noticeIfThereAreNoMessages(messageSet);
                    getMessageSets().add(messageSet);
                }

                // Repair CURATION_USER CAS
                {
                    var messageSet = new LogMessageSet(sd.getName() + " [" + CURATION_USER + "]");
                    try {
                        casStorageService.forceActionOnCas(sd, CURATION_SET,
                                (doc, set) -> casStorageService.readCas(doc, set,
                                        UNMANAGED_NON_INITIALIZING_ACCESS),
                                (doc, set, cas) -> casDoctor.repair(doc, set.id(), cas,
                                        messageSet.getMessages()), //
                                true);
                    }
                    catch (FileNotFoundException e) {
                        if (asList(CURATION_IN_PROGRESS, CURATION_FINISHED)
                                .contains(sd.getState())) {
                            messageSet.add(LogMessage.error(getClass(), "Curation CAS missing."));
                        }
                        else {
                            // If there is no CAS for the curation user, then curation has not
                            // started
                            // yet. This is not a problem, so we can ignore it.
                            messageSet
                                    .add(LogMessage.info(getClass(), "Curation has not started."));
                        }
                    }
                    catch (Exception e) {
                        messageSet.add(LogMessage.error(getClass(),
                                "Error checking annotations for [%s] for [%s]: %s", CURATION_USER,
                                sd.getName(), e.getMessage()));
                        LOG.error("Error checking annotations for [{}] for [{}]", CURATION_USER,
                                sd.getName(), e);
                    }

                    noticeIfThereAreNoMessages(messageSet);
                    getMessageSets().add(messageSet);
                }

                // Repair regular annotator CASes
                for (var ad : documentService.listAnnotationDocuments(sd)) {
                    var messageSet = new LogMessageSet(sd.getName() + " [" + ad.getUser() + "]");
                    try {
                        if (documentService.existsCas(ad)) {
                            casStorageService.forceActionOnCas(sd,
                                    AnnotationSet.forUser(ad.getUser()),
                                    (doc, set) -> casStorageService.readCas(doc, set,
                                            UNMANAGED_NON_INITIALIZING_ACCESS),
                                    (doc, set, cas) -> casDoctor.repair(doc, set.id(), cas,
                                            messageSet.getMessages()), //
                                    true);
                        }
                    }
                    catch (Exception e) {
                        messageSet.add(LogMessage.error(getClass(),
                                "Error repairing annotations of user [%s] for [%s]: %s",
                                ad.getUser(), sd.getName(), e.getMessage()));
                        LOG.error("Error repairing annotations of user [{}] for [{}]", ad.getUser(),
                                sd.getName(), e);
                    }

                    noticeIfThereAreNoMessages(messageSet);
                    getMessageSets().add(messageSet);
                }
            }

            progress.update(up -> up.addMessage(LogMessage.info(this, "Repairs complete")));
        }
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends CasDoctorTask_ImplBase.Builder<T>
    {
        private List<String> repairs;

        protected Builder()
        {
        }

        @SuppressWarnings("unchecked")
        public T withRepairs(String... aRepairs)
        {
            repairs = asList(aRepairs);
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withRepairs(Iterable<String> aRepairs)
        {
            repairs = new ArrayList<>();
            aRepairs.forEach(repairs::add);
            return (T) this;
        }

        public RepairTask build()
        {
            Validate.notNull(project, "RepairTask requires a project");

            return new RepairTask(this);
        }
    }
}
