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
package de.tudarmstadt.ukp.inception.processing.tagset;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.scheduling.ProgressScope.SCOPE_DOCUMENTS;
import static de.tudarmstadt.ukp.inception.scheduling.TaskScope.PROJECT;
import static java.util.Arrays.asList;
import static org.apache.uima.cas.CAS.TYPE_NAME_STRING_ARRAY;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.util.FSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.recommendation.tasks.RecommendationTask_ImplBase;
import de.tudarmstadt.ukp.inception.scheduling.ProjectTask;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.logging.LogMessage;

public class TagSetExtractionTask
    extends Task
    implements ProjectTask
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "TagsetExtractionTask";

    private static final List<String> supportedTypes = asList(CAS.TYPE_NAME_STRING,
            CAS.TYPE_NAME_STRING_ARRAY);

    private @Autowired DocumentService documentService;
    private @Autowired AnnotationSchemaService schemaService;

    private final boolean addTagsetToFeature;
    private AnnotationFeature feature;
    private TagSet tagSet;

    public TagSetExtractionTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE).withCancellable(true).withScope(PROJECT));

        feature = aBuilder.feature;
        tagSet = aBuilder.tagSet;
        addTagsetToFeature = aBuilder.addTagsetToFeature;
    }

    @Override
    public String getTitle()
    {
        return "Extracting tagset...";
    }

    @Override
    public void execute()
    {
        var extractedTags = extractTags();

        updateTagset(extractedTags);
    }

    private void updateTagset(Set<String> discoveredTags)
    {
        if (tagSet == null) {
            tagSet = new TagSet(getProject(),
                    feature.getLayer().getUiName() + " - " + feature.getUiName());
            schemaService.createTagSet(tagSet);
        }

        var existingTags = schemaService.listTags(tagSet);
        int nextRank = 0;
        for (var tag : existingTags) {
            discoveredTags.remove(tag.getName());
            nextRank = Math.max(nextRank, tag.getRank());
        }

        var newTags = new ArrayList<Tag>();
        for (var tag : discoveredTags.stream().sorted().toList()) {
            var newTag = new Tag(tagSet, tag);
            newTag.setRank(nextRank);
            newTags.add(newTag);
            nextRank++;
        }

        schemaService.createTags(newTags.toArray(Tag[]::new));

        if (addTagsetToFeature) {
            feature = schemaService.getFeature(feature.getName(), feature.getLayer());
            feature.setTagset(tagSet);
            schemaService.createFeature(feature);
        }
    }

    private Set<String> extractTags()
    {
        var tags = new HashSet<String>();

        var documents = documentService.listSourceDocuments(getProject());
        try (var progress = getMonitor().openScope(SCOPE_DOCUMENTS, documents.size())) {
            for (var srcDoc : documents) {
                progress.update(up -> up.increment());

                extractTagsFromDocument(tags, srcDoc);
            }

            progress.update(up -> up.addMessage(LogMessage.info(this, "Tag extraction complete")));
        }

        return tags;
    }

    private void extractTagsFromDocument(HashSet<String> tags, SourceDocument srcDoc)
    {
        LOG.trace("Extracting tags from document {}", srcDoc);

        var annDocs = documentService.listAnnotationDocuments(srcDoc);

        try (var session = CasStorageSession.openNested()) {
            if (annDocs.isEmpty()) {
                var cas = documentService.createOrReadInitialCas(srcDoc, AUTO_CAS_UPGRADE,
                        SHARED_READ_ONLY_ACCESS);

                extractTagsFromCas(cas, tags);
            }
            else {
                for (var annDoc : annDocs) {
                    var cas = documentService.readAnnotationCas(annDoc, AUTO_CAS_UPGRADE,
                            SHARED_READ_ONLY_ACCESS);

                    extractTagsFromCas(cas, tags);
                }
            }
        }
        catch (IOException e) {
            getMonitor().update(up -> up.addMessage(
                    LogMessage.error(this, "Unable to process document [%s].", srcDoc.getName())));
        }
    }

    private void extractTagsFromCas(CAS aCas, Set<String> aTags)
    {
        var adapter = schemaService.getAdapter(feature.getLayer());

        var uimaType = aCas.getTypeSystem().getType(adapter.getAnnotationTypeName());
        if (uimaType == null) {
            return;
        }

        var uimaFeature = uimaType.getFeatureByBaseName(feature.getName());
        if (!supportedTypes.contains(uimaFeature.getRange().getName())) {
            return;
        }

        var isMultiValue = TYPE_NAME_STRING_ARRAY.equals(uimaFeature.getRange().getName());
        for (var ann : aCas.select(uimaType)) {
            if (isMultiValue) {
                var labels = FSUtil.getFeature(ann, uimaFeature, String[].class);
                if (labels != null) {
                    Stream.of(labels) //
                            .filter(label -> label != null) //
                            .forEach(aTags::add);
                }
            }
            else {
                var label = ann.getFeatureValueAsString(uimaFeature);
                if (label != null) {
                    var added = aTags.add(label);
                    if (added) {
                        LOG.trace("Found new tag: [{}]", label);
                    }
                }
            }
        }
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends RecommendationTask_ImplBase.Builder<T>
    {
        private TagSet tagSet;
        private AnnotationFeature feature;
        private boolean addTagsetToFeature;

        @SuppressWarnings("unchecked")
        public T withAnnotationFeature(AnnotationFeature aFeature)
        {
            feature = aFeature;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withTagSet(TagSet aTagSet)
        {
            tagSet = aTagSet;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withAddTagsetToFeature(boolean aAddTagsetToFeature)
        {
            addTagsetToFeature = aAddTagsetToFeature;
            return (T) this;
        }

        public TagSetExtractionTask build()
        {
            Validate.notNull(sessionOwner, "TagsetExtractionTask requires a session owner");
            Validate.notNull(project, "TagsetExtractionTask requires a project");
            Validate.notNull(feature, "TagsetExtractionTask requires a feature");

            return new TagSetExtractionTask(this);
        }
    }
}
