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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

@Component(FactLinkingService.SERVICE_NAME)
public class FactLinkingServiceImpl implements FactLinkingService
{
    @Autowired private KnowledgeBaseService kbService;

    private final Logger LOG = LoggerFactory.getLogger(getClass());

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
    public KBHandle getKBHandleFromCasByAddr(JCas aJcas, int targetAddr, Project aProject)
    {
        KBHandle kbHandle = null;
        AnnotationFS selectedFS = WebAnnoCasUtil.selectByAddr(aJcas, targetAddr);
        String kbHandleIdentifier = WebAnnoCasUtil
            .getFeature(selectedFS, FactLinkingConstants.LINKED_LAYER_FEATURE);
        if (kbHandleIdentifier != null) {
            List<KBHandle> handles = getKBConceptsAndInstances(aProject);
            kbHandle = handles.stream().filter(x -> kbHandleIdentifier.equals(x.getIdentifier()))
                .findAny().orElse(null);
        }
        return kbHandle;
    }
}
