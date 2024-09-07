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
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.CURATION_USER;
import static de.tudarmstadt.ukp.inception.support.WebAnnoConst.INITIAL_CAS_PSEUDO_USER;
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
import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.diag.ChecksRegistry;
import de.tudarmstadt.ukp.clarin.webanno.diag.RepairsRegistry;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.scheduling.TaskState;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class CheckTask
    extends CasDoctorTask_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "CheckTask";

    private @Autowired RepairsRegistry repairsRegistry;
    private @Autowired ChecksRegistry checksRegistry;
    private @Autowired DocumentService documentService;
    private @Autowired CasStorageService casStorageService;

    private final List<String> checks;

    private int objectCount = 0;

    public CheckTask(Builder<? extends Builder<?>> aBuilder)
    {
        // Can currently not be cancelled because we're running it sync and the AJAX cancel request
        // won't get through
        super(aBuilder.withType(TYPE).withCancellable(false).withScope(PROJECT));

        checks = aBuilder.checks;
    }

    @Override
    public String getTitle()
    {
        return "Running CAS Doctor checks...";
    }

    @Override
    public void execute()
    {
        var casDoctor = new CasDoctor(checksRegistry, repairsRegistry);
        casDoctor.setActiveChecks(checks.toArray(String[]::new));

        var project = getProject();

        var sourceDocuments = documentService.listSourceDocuments(project);

        var monitor = getMonitor();
        var progress = 0;
        var maxProgress = sourceDocuments.size();

        for (var sd : sourceDocuments) {
            progress++;

            monitor.setProgressWithMessage(progress, maxProgress,
                    LogMessage.info(this, "Processing [%s]...", sd.getName()));
            if (monitor.isCancelled()) {
                monitor.setState(TaskState.CANCELLED);
            }

            // Check INITIAL CAS
            {
                var messageSet = new LogMessageSet(sd.getName() + " [INITIAL]");

                try {
                    objectCount++;
                    casStorageService.forceActionOnCas(sd, INITIAL_CAS_PSEUDO_USER,
                            (doc, user) -> createOrReadInitialCasWithoutSavingOrChecks(doc,
                                    messageSet),
                            (cas) -> casDoctor.analyze(project, cas, messageSet.getMessages()), //
                            false);
                }
                catch (Exception e) {
                    messageSet.add(
                            LogMessage.error(getClass(), "Error checking initial CAS for [%s]: %s",
                                    sd.getName(), e.getMessage()));
                    LOG.error("Error checking initial CAS for [{}]", sd.getName(), e);
                }

                noticeIfThereAreNoMessages(messageSet);
                getMessageSets().add(messageSet);
            }

            // Check CURATION_USER CAS
            {
                var messageSet = new LogMessageSet(sd.getName() + " [" + CURATION_USER + "]");
                try {
                    objectCount++;
                    casStorageService.forceActionOnCas(sd, CURATION_USER,
                            (doc, user) -> casStorageService.readCas(doc, user,
                                    UNMANAGED_NON_INITIALIZING_ACCESS),
                            (cas) -> casDoctor.analyze(project, cas, messageSet.getMessages()), //
                            false);
                }
                catch (FileNotFoundException e) {
                    // If there is no CAS for the curation user, then curation has not started yet.
                    // This is not a problem, so we can ignore it.
                    messageSet.add(
                            LogMessage.info(getClass(), "Curation seems to have not yet started."));
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

            // Check regular annotator CASes
            for (var ad : documentService.listAnnotationDocuments(sd)) {
                var messageSet = new LogMessageSet(sd.getName() + " [" + ad.getUser() + "]");
                try {
                    if (documentService.existsCas(ad)) {
                        objectCount++;
                        casStorageService.forceActionOnCas(ad.getDocument(), ad.getUser(),
                                (doc, user) -> casStorageService.readCas(doc, user,
                                        UNMANAGED_NON_INITIALIZING_ACCESS),
                                (cas) -> casDoctor.analyze(project, cas, messageSet.getMessages()), //
                                false);
                    }
                }
                catch (Exception e) {
                    messageSet.add(LogMessage.error(getClass(),
                            "Error checking annotations of user [%s] for [%s]: %s", ad.getUser(),
                            sd.getName(), e.getMessage()));
                    LOG.error("Error checking annotations of user [{}] for [{}]", ad.getUser(),
                            sd.getName(), e);
                }

                noticeIfThereAreNoMessages(messageSet);
                getMessageSets().add(messageSet);
            }
        }

        monitor.setProgressWithMessage(progress, maxProgress,
                LogMessage.info(this, "Checks complete"));
    }

    public int getObjectCount()
    {
        return objectCount;
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends CasDoctorTask_ImplBase.Builder<T>
    {
        private List<String> checks;

        protected Builder()
        {
        }

        @SuppressWarnings("unchecked")
        public T withChecks(String... aChecks)
        {
            checks = asList(aChecks);
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withChecks(Iterable<String> aChecks)
        {
            checks = new ArrayList<>();
            aChecks.forEach(checks::add);
            return (T) this;
        }

        public CheckTask build()
        {
            Validate.notNull(project, "Parameter [project] must be specified");
            Validate.notNull(checks, "Parameter [checks] must be specified");

            return new CheckTask(this);
        }
    }
}
