/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.ui.kb.feature;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Resource;

import org.apache.uima.cas.CASException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBStatement;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component(FactLinkingService.SERVICE_NAME)
public class FactLinkingServiceImpl implements FactLinkingService
{
    @Resource private KnowledgeBaseService kbService;
    @Resource private AnnotationSchemaService annotationService;

    private static final String FACT_LAYER = "webanno.custom.Fact";
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public KnowledgeBase getKBByKBHandle(KBHandle kbHandle, Project aProject)
    {
        for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
            if (kbService.listProperties(kb, false).contains(kbHandle)) {
                return kb;
            }
            if (kbService.listConcepts(kb, false).contains(kbHandle)) {
                return kb;
            }
            for (KBHandle concept : kbService.listConcepts(kb, false)) {
                if (kbService.listInstances(kb, concept.getIdentifier(), false)
                    .contains(kbHandle)) {
                    return kb;
                }
            }

        }
        return null;
    }

    @Override
    public List<KBHandle> getKBConceptsAndInstances(Project aProject)
    {
        List<KBHandle> handles = new ArrayList<>();
        for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
            handles.addAll(kbService.listConcepts(kb, false));
            for (KBHandle concept : kbService.listConcepts(kb, false)) {
                handles.addAll(kbService.listInstances(kb, concept.getIdentifier(), false));
            }
        }
        return handles;
    }

    @Override
    public List<KBHandle> getAllPredicatesFromKB(Project aProject)
    {
        List<KBHandle> handles = new ArrayList<>();
        for (KnowledgeBase kb : kbService.getKnowledgeBases(aProject)) {
            handles.addAll(kbService.listProperties(kb, false));
        }
        return handles;
    }

    @Override
    public KBHandle getPredicateKBHandle(AnnotatorState aState)
    {
        AnnotationLayer factLayer = annotationService.getLayer(
            FACT_LAYER, aState.getProject());
        AnnotationFeature predicateFeature = annotationService.getFeature(
            "KBPredicate", factLayer);
        KBHandle predicateHandle = (KBHandle) aState.getFeatureState
            (predicateFeature).value;
        return predicateHandle;
    }

    @Override
    public KBHandle getLinkedSubjectObjectKBHandle(String featureName, AnnotationActionHandler
        actionHandler, AnnotatorState aState)
    {
        AnnotationLayer factLayer = annotationService.getLayer(FACT_LAYER, aState.getProject());
        KBHandle kbHandle = null;
        AnnotationFeature annotationFeature = annotationService.getFeature(featureName, factLayer);
        List<LinkWithRoleModel> featureValue = (List<LinkWithRoleModel>) aState.getFeatureState
            (annotationFeature).value;
        int targetAddress = featureValue.get(0).targetAddr;
        if (targetAddress != -1) {
            JCas jCas = null;
            try {
                jCas = actionHandler.getEditorCas().getCas().getJCas();
            }
            catch (CASException | IOException e) {
                log.error("Error: " + e.getMessage(), e);
            }
            kbHandle = getKBHandleFromCasByAddr(jCas, targetAddress, aState.getProject());
        }
        return kbHandle;
    }

    @Override
    public KBHandle getKBHandleFromCasByAddr(JCas aJcas, int targetAddr, Project aProject)
    {
        KBHandle kbHandle = null;
        AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(aJcas, targetAddr);
        String kbHandleIdentifier = WebAnnoCasUtil.getFeature(selectedFS, "KBItems");
        if (kbHandleIdentifier != null) {
            List<KBHandle> handles = getKBConceptsAndInstances(aProject);
            kbHandle = handles.stream()
                .filter(x -> kbHandleIdentifier.equals(x.getIdentifier())).findAny()
                .orElse(null);
        }
        return kbHandle;
    }

    @Override
    public boolean checkSameKnowledgeBase(KBHandle handleA, KBHandle handleB, Project aProject)
    {
        KnowledgeBase kbA = getKBByKBHandle(handleA, aProject);
        KnowledgeBase kbB = getKBByKBHandle(handleB, aProject);
        return kbA.equals(kbB);
    }

    @Override
    public void updateStatement(KBHandle subject, KBHandle predicate, String object,
                                       KBStatement oldStatement, Project aProject)
    {
        KnowledgeBase kb = getKBByKBHandle(subject, aProject);

        // Update old statement by deleting it and creating a new one
        if (oldStatement != null) {
            kbService.deleteStatement(kb, oldStatement);
        }

        KBStatement statement = new KBStatement();
        statement.setInstance(subject);
        statement.setProperty(predicate);
        statement.setValue(object);
        kbService.upsertStatement(kb, statement);
    }

    @Override
    public KBStatement getOldStatement(KBHandle subject, KBHandle predicate, String oldValue,
        Project aProject)
    {
        KnowledgeBase kb = getKBByKBHandle(subject, aProject);
        KBStatement oldStatement = kbService.getExistingStatement(kb, subject, predicate, oldValue);
        return oldStatement;
    }
}
