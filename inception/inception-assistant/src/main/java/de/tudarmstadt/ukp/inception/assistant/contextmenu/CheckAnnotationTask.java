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
package de.tudarmstadt.ukp.inception.assistant.contextmenu;

import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasAccessMode.SHARED_READ_ONLY_ACCESS;
import static de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasUpgradeMode.AUTO_CAS_UPGRADE;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.SYSTEM;
import static de.tudarmstadt.ukp.inception.assistant.model.MChatRoles.USER;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.NO_MATCH;
import static de.tudarmstadt.ukp.inception.scheduling.MatchResult.QUEUE_THIS;
import static de.tudarmstadt.ukp.inception.support.json.JSONUtil.toPrettyJsonString;
import static de.tudarmstadt.ukp.inception.support.uima.ICasUtil.selectAnnotationByAddr;
import static java.lang.String.join;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.normalizeSpace;
import static org.apache.uima.cas.CAS.TYPE_NAME_BOOLEAN;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.Validate;
import org.apache.uima.jcas.tcas.Annotation;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.session.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationSet;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.inception.assistant.AssistantService;
import de.tudarmstadt.ukp.inception.assistant.model.MTextMessage;
import de.tudarmstadt.ukp.inception.assistant.sidebar.WatchAnnotationTask;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;
import de.tudarmstadt.ukp.inception.scheduling.MatchResult;
import de.tudarmstadt.ukp.inception.scheduling.MatchableTask;
import de.tudarmstadt.ukp.inception.scheduling.Task;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class CheckAnnotationTask
    extends Task
    implements MatchableTask
{

    public static final String TYPE = "CheckAnnotationTask";
    private static final String ACTOR = "Annotation checker";

    private @Autowired AnnotationSchemaService schemaService;
    private @Autowired AssistantService assistantService;
    private @Autowired DocumentService documentService;

    private final SourceDocument document;
    private final String dataOwner;
    private final VID annotation;

    public CheckAnnotationTask(Builder<? extends Builder<?>> aBuilder)
    {
        super(aBuilder.withType(TYPE));

        requireNonNull(getSessionOwner().orElse(null), "Session owner must be set");

        document = aBuilder.document;
        dataOwner = aBuilder.dataOwner;
        annotation = aBuilder.annotation;
    }

    @Override
    public MatchResult matches(Task aTask)
    {
        if (aTask instanceof WatchAnnotationTask) {
            if (Objects.equals(getProject().getId(), aTask.getProject().getId()) && Objects
                    .equals(getSessionOwner().get(), aTask.getSessionOwner().orElse(null))) {
                return QUEUE_THIS;
            }
        }

        return NO_MATCH;
    }

    @Override
    public void execute() throws Exception
    {
        try (var session = CasStorageSession.open()) {
            var cas = documentService.readAnnotationCas(document, AnnotationSet.forUser(dataOwner),
                    AUTO_CAS_UPGRADE, SHARED_READ_ONLY_ACCESS);

            var ann = selectAnnotationByAddr(cas, annotation.getId());
            if (ann == null) {
                return;
            }

            var contextSentence = cas.select(Sentence.class).covering(ann).nullOK().get();
            if (contextSentence == null) {
                return;
            }

            var inquiryMsgId = UUID.randomUUID();
            var sessionOwner = getSessionOwner().get().getUsername();
            assistantService.dispatchMessage(sessionOwner, getProject(), MTextMessage.builder() //
                    .withId(inquiryMsgId) //
                    .withActor(getSessionOwner().get().getUiName()) //
                    .withRole(USER) //
                    .notDone() //
                    .build());

            var instance = annotationToJson(ann, contextSentence);

            var rewriteQuestionTask = MTextMessage.builder() //
                    .withActor(ACTOR) //
                    .withRole(USER).internal().ephemeral() //
                    .withMessage(join("\n", //
                            "Rewrite into a question about whether the annotation is correct with respect to the "
                                    + "span marked in the context.", //
                            "Do not answer the question yet.", //
                            "\n", //
                            "```json", //
                            instance, //
                            "```")) //
                    .build();

            var rewrittenQuestion = assistantService.processInternalMessageSync(sessionOwner,
                    getProject(), rewriteQuestionTask);

            var inquiryContext = MTextMessage.builder() //
                    .withActor(ACTOR) //
                    .withRole(SYSTEM).internal().ephemeral() //
                    .withMessage(join("\n", //
                            "The user will ask whether the following annotation is correct.", //
                            "Give one response per annotation.", //
                            "Start each response with yes, no, or unsure, then very briefly explain.", //
                            "If expanding or reducing the span seems appropriate, mention that.", //
                            "Use markdown for formatting.", //
                            "", //
                            "```json", //
                            instance, //
                            "```")) //
                    .build();

            var inquiryTask = MTextMessage.builder() //
                    .withId(inquiryMsgId) //
                    .withActor(getSessionOwner().get().getUiName()) //
                    .withRole(USER) //
                    .withMessage(rewrittenQuestion.message()) //
                    .build();

            assistantService.processUserMessage(sessionOwner, getProject(), document, dataOwner,
                    inquiryTask, inquiryContext);
        }
    }

    private String annotationToJson(Annotation aAnnotation, Annotation aContext) throws IOException
    {
        var instance = new LinkedHashMap<String, Object>();

        instance.put("span", normalizeSpace(aAnnotation.getCoveredText()));

        var docText = aAnnotation.getCAS().getDocumentText();
        var context = docText.substring(aContext.getBegin(), aAnnotation.getBegin()) //
                + " <span> " //
                + aAnnotation.getCoveredText() //
                + " </span> " //
                + docText.substring(aAnnotation.getEnd(), aContext.getEnd());
        instance.put("context", normalizeSpace(context));

        var adapter = schemaService.findAdapter(getProject(), aAnnotation);
        var attributes = new LinkedHashMap<String, Object>();
        for (var feature : adapter.listFeatures()) {
            if (TYPE_NAME_BOOLEAN.equals(feature.getType())) {
                attributes.put(normalizeSpace(feature.getUiName()),
                        adapter.getFeatureValue(feature, aAnnotation));
                continue;
            }

            attributes.put(normalizeSpace(feature.getUiName()),
                    normalizeSpace(adapter.renderFeatureValue(aAnnotation, feature.getName())));
        }
        instance.put("annotation", attributes);

        return toPrettyJsonString(instance);
    }

    public static Builder<Builder<?>> builder()
    {
        return new Builder<>();
    }

    public static class Builder<T extends Builder<?>>
        extends Task.Builder<T>
    {
        private SourceDocument document;
        private String dataOwner;
        private VID annotation;

        protected Builder()
        {
        }

        @SuppressWarnings("unchecked")
        public T withAnnotation(VID aVid)
        {
            annotation = aVid;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withDocument(SourceDocument aDocument)
        {
            document = aDocument;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        public T withDataOwner(String aDataOwner)
        {
            dataOwner = aDataOwner;
            return (T) this;
        }

        public CheckAnnotationTask build()
        {
            Validate.notNull(project, "Parameter [project] must be specified");

            return new CheckAnnotationTask(this);
        }
    }
}
