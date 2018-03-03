/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.kb.feature;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.setFeature;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import javax.annotation.Resource;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupport;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureType;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.editor.FeatureEditor;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.FeatureState;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.kb.graph.KBProperty;


@Component
public class PropertyFeatureSupport implements FeatureSupport {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Resource private KnowledgeBaseService kbService;
    private static final String PREDICATE_KEY = "Property: Predicate";

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer) {
        List<FeatureType> types = new LinkedList<>();
        types.add(new FeatureType(PREDICATE_KEY, PREDICATE_KEY));

        return new ArrayList<>(types);
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature) {
        switch (aFeature.getMultiValueMode()) {
        case NONE:
            switch (aFeature.getType()) {
            case PREDICATE_KEY:
                return true;
            default:
                return false;
            }
        case ARRAY: // fallthrough
        default:
            return false;
        }
    }

    @Override
    public String renderFeatureValue(AnnotationFeature aFeature, AnnotationFS aFS,
                                     Feature aLabelFeature) {
        try {
            String value = aFS.getFeatureValueAsString(aLabelFeature);
            String renderValue = null;
            if (value != null) {
                // FIXME Since this might be called very often during rendering, it *might* be
                // worth to set up an LRU cache instead of relying on the performance of the
                // underlying KB store.
                renderValue = kbService.getKnowledgeBases(aFeature.getProject())
                    .stream()
                    .map(k -> kbService.readProperty(k, value))
                    .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                    .map(KBProperty::getUiLabel)
                    .findAny()
                    .orElseThrow(NoSuchElementException::new);
            }
            return renderValue;
        } catch (Exception e) {
            log.error("Unable to render feature value", e);
            return "ERROR";
        }
    }

    @Override
    public void setFeatureValue(JCas aJcas, AnnotationFeature aFeature, int aAddress, Object
        aValue) {
        KBHandle kbProp = (KBHandle) aValue;
        FeatureStructure fs = selectByAddr(aJcas, FeatureStructure.class, aAddress);
        setFeature(fs, aFeature, kbProp != null ? kbProp.getIdentifier() : null);
    }

    @Override
    public KBHandle getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFs) {
        String value = (String) FeatureSupport.super.getFeatureValue(aFeature, aFs);
        if (value != null) {
            KBProperty prop = kbService.getKnowledgeBases(aFeature.getProject())
                .stream()
                .map(k -> kbService.readProperty(k, value))
                .flatMap(o -> o.map(Stream::of).orElseGet(Stream::empty))
                .findAny()
                .orElseThrow(() -> new NoSuchElementException());
            return new KBHandle(prop.getIdentifier(), prop.getName());
        } else {
            return null;
        }
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
                                      AnnotationActionHandler aHandler, final
                                      IModel<AnnotatorState> aStateModel,
                                      final IModel<FeatureState> aFeatureStateModel) {
        FeatureState featureState = aFeatureStateModel.getObject();
        final FeatureEditor editor;

        switch (featureState.feature.getMultiValueMode()) {
        case NONE:
            if (featureState.feature.getType().startsWith("Property")) {
                editor = new PropertyFeatureEditor(aId, aOwner, aFeatureStateModel);
            } else {
                throw unsupportedMultiValueModeException(featureState);
            }
            break;
        case ARRAY: // fallthrough
        default:
            throw unsupportedMultiValueModeException(featureState);
        }

        return editor;
    }

}

