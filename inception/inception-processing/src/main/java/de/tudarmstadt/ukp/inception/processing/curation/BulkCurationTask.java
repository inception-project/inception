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
package de.tudarmstadt.ukp.inception.processing.curation;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.EXCLUSIVE_WRITE_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.FORCE_CAS_UPGRADE;
import static de.tudarmstadt.ukp.clarin.webanno.curation.casdiff.CasDiff.doDiff;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentStateChangeFlag.EXPLICIT_ANNOTATOR_USER_ACTION;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState.CURATION_IN_PROGRESS;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_DOCUMENTS;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.inception.curation.api.DiffAdapterRegistry;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategy;
import de.tudarmstadt.ukp.inception.curation.merge.strategy.MergeStrategyFactory;
import de.tudarmstadt.ukp.inception.curation.model.CurationWorkflow;
import de.tudarmstadt.ukp.inception.curation.service.CurationDocumentService;
import de.tudarmstadt.ukp.inception.curation.service.CurationMergeService;
import de.tudarmstadt.ukp.inception.curation.service.CurationService;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.tasks.RecommendationTask_ImplBase;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class BulkCurationTask
    extends RecommendationTask_ImplBase
    implements ProjectTask
{
    public static final String TYPE = "BulkCurationTask";

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @Autowired DocumentService documentService;
    private @Autowired CurationDocumentService curationDocumentService;
    private @Autowired CurationMergeService curationMergeService;
    private @Autowired CurationService curationService;
    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired DiffAdapterRegistry diffAdapterRegistry;

    private final List<AnnotationLayer> annotationLayers;
    private final CurationWorkflow curationWorkflow;
    private final String targetUser;

    public BulkCurationTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE).withCancellable(true).withScope(PROJECT));

        targetUser = aBuilder.targetUser;
        annotationLayers = aBuilder.annotationLayers;
        curationWorkflow = aBuilder.curationWorkflow;
    }

    @Override
    public String getTitle()
    {
        var mergeStrategyFactory = curationService.getMergeStrategyFactory(curationWorkflow);
        return "Auto-curating documents using " + mergeStrategyFactory.getLabel() + "...";
    }

    @Override
    public void execute() throws IOException, UIMAException
    {
        var mergeStrategy = createMergeStrategy();

        var curatableDocuments = curationDocumentService.listCuratableSourceDocuments(getProject());
        try (var progress = getMonitor().openScope(SCOPE_DOCUMENTS, curatableDocuments.size())) {
            for (var doc : curatableDocuments) {
                progress.update(up -> up.increment() //
                        .addMessage(LogMessage.info(this, "%s", doc.getName())));

                try (var session = CasStorageSession.openNested()) {
                    var users = curationDocumentService.listCuratableUsers(doc);
                    users.removeIf(u -> targetUser.equals(u.getUsername()));

                    var targetCas = documentService.readAnnotationCas(doc,
                            AnnotationSet.forUser(targetUser), FORCE_CAS_UPGRADE,
                            EXCLUSIVE_WRITE_ACCESS);

                    var annotatorCasses = documentService.readAllCasesSharedNoUpgrade(doc, users);

                    // FIXME: should merging not overwrite the current users annos? (can result in
                    // deleting the users annotations!!!), currently fixed by warn message to user
                    // prepare merged CAS
                    curationMergeService.mergeCasses(doc, targetUser, targetCas, annotatorCasses,
                            mergeStrategy, annotationLayers, true);

                    var targetAnnDoc = documentService.createOrGetAnnotationDocument(doc,
                            AnnotationSet.forUser(targetUser));
                    documentService.writeAnnotationCas(targetCas, targetAnnDoc,
                            EXPLICIT_ANNOTATOR_USER_ACTION);

                    var allIsCurated = noUncuratedDifferencesRemaining(targetCas, annotatorCasses);
                    if (allIsCurated) {
                        LOG.info("{} has been fully curated", doc);
                        documentService.setAnnotationDocumentState(targetAnnDoc, FINISHED,
                                EXPLICIT_ANNOTATOR_USER_ACTION);
                        documentService.setSourceDocumentState(doc, CURATION_FINISHED);
                    }
                    else {
                        LOG.info("{} has remaining differences that need to be curated manually",
                                doc);
                        documentService.setAnnotationDocumentState(targetAnnDoc, IN_PROGRESS,
                                EXPLICIT_ANNOTATOR_USER_ACTION);
                        documentService.setSourceDocumentState(doc, CURATION_IN_PROGRESS);
                    }
                }
            }

            progress.update(up -> up.addMessage(LogMessage.info(this, "Curation complete")));
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private MergeStrategy createMergeStrategy()
    {
        MergeStrategyFactory mergeStrategyFactory = curationService
                .getMergeStrategyFactory(curationWorkflow);
        var traits = mergeStrategyFactory.readTraits(curationWorkflow);
        var mergeStrategy = mergeStrategyFactory.makeStrategy(traits);
        return mergeStrategy;
    }

    private boolean noUncuratedDifferencesRemaining(CAS targetCas, Map<String, CAS> annotatorCasses)
    {
        var allCasses = new HashMap<>(annotatorCasses);
        allCasses.put(targetUser, targetCas);

        var adapters = diffAdapterRegistry.getDiffAdapters(annotationLayers);
        var diff = doDiff(adapters, allCasses).toResult();

        if (LOG.isTraceEnabled()) {
            for (var cfgSet : diff.getConfigurationSets()) {
                var type = StringUtils.substringAfterLast(cfgSet.getPosition().getType(), ".");
                var pos = cfgSet.getPosition().toMinimalString();
                var cfgCount = cfgSet.getConfigurations().size();
                if (cfgSet.getCasGroupIds().contains(targetUser)) {
                    LOG.trace("Curated: {} {} {} {}", type, pos, cfgCount, cfgSet.getCasGroupIds());
                }
                else {
                    LOG.trace("Not curated: {} {} {} {}", type, pos, cfgCount,
                            cfgSet.getCasGroupIds());
                }
            }
        }

        return diff.getConfigurationSets().stream()
                .allMatch($ -> $.getCasGroupIds().contains(targetUser));
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends RecommendationTask_ImplBase.Builder<T>
    {
        private CurationWorkflow curationWorkflow;
        private List<AnnotationLayer> annotationLayers;
        private String targetUser;

        @SuppressWarnings("unchecked")
        public T withCurationWorkflow(CurationWorkflow aCurationWorkflow)
        {
            curationWorkflow = aCurationWorkflow;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withAnnotationLayers(List<AnnotationLayer> aAnnotationLayers)
        {
            annotationLayers = aAnnotationLayers;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withTargetUser(String aTargetUser)
        {
            targetUser = aTargetUser;
            return (T) this;
        }

        public BulkCurationTask build()
        {
            requireNonNull(project, "Parameter [project] must be specified");
            requireNonNull(targetUser, "Parameter [targetUser] must be specified");
            requireNonNull(annotationLayers, "Parameter [annotationLayers] must be specified");
            requireNonNull(curationWorkflow, "Parameter [curationWorkflow] must be specified");

            return new BulkCurationTask(this);
        }
    }
}
