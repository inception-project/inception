/*
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.brat.adapter;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getFeature;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectByAddr;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.util.CasUtil.getType;
import static org.apache.uima.fit.util.CasUtil.selectCovered;
import static org.apache.uima.fit.util.JCasUtil.selectCovered;

import java.util.ArrayList;
import java.util.List;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.LinkWithRoleModel;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.TypeUtil;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Argument;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Entity;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Offsets;
import de.tudarmstadt.ukp.clarin.webanno.brat.render.model.Relation;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.LinkMode;
import de.tudarmstadt.ukp.clarin.webanno.model.MultiValueMode;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

/**
 * Render spans.
 */
public class BratSpanRenderer
    implements TypeRenderer
{
    private SpanAdapter typeAdapter;
    
    public BratSpanRenderer(SpanAdapter aTypeAdapter)
    {
        typeAdapter = aTypeAdapter;
    }
    
    /**
     * Add annotations from the CAS, which is controlled by the window size, to the brat response
     * {@link GetDocumentResponse}
     *
     * @param aJcas
     *            The JCAS object containing annotations
     * @param aResponse
     *            A brat response containing annotations in brat protocol
     * @param aBratAnnotatorModel
     *            Data model for brat annotations
     * @param aColoringStrategy
     *            the coloring strategy to render this layer
     */
    @Override
    public void render(JCas aJcas, List<AnnotationFeature> aFeatures,
            GetDocumentResponse aResponse, AnnotatorState aBratAnnotatorModel,
            ColoringStrategy aColoringStrategy)
    {
        Type type = getType(aJcas.getCas(), typeAdapter.getAnnotationTypeName());
        
        int windowBegin = aBratAnnotatorModel.getWindowBeginOffset();
        int windowEnd = aBratAnnotatorModel.getWindowEndOffset();

        List<Sentence> visibleSentences = selectCovered(aJcas, Sentence.class, windowBegin,
                windowEnd);
        
        for (AnnotationFS fs : selectCovered(aJcas.getCas(), type, windowBegin, windowEnd)) {
            String bratTypeName = TypeUtil.getUiTypeName(typeAdapter);
            String bratLabelText = TypeUtil.getUiLabelText(typeAdapter, fs, aFeatures);
            String color = aColoringStrategy.getColor(fs, bratLabelText);

            Sentence beginSent = null;
            Sentence endSent = null;
            
            // check if annotation extends beyond viewable window - if yes, then constrain it to 
            // the visible window
            for (Sentence sentence : visibleSentences) {
                if (beginSent == null) {
                    if (sentence.getBegin() <= fs.getBegin() && fs.getBegin() < sentence.getEnd()) {
                        beginSent = sentence;
                    }
                }
                
                if (endSent == null) {
                    if (sentence.getBegin() <= fs.getEnd() && fs.getEnd() <= sentence.getEnd()) {
                        endSent = sentence;
                    }
                }
                
                if (beginSent != null && endSent != null) {
                    break;
                }
            }
            
            if (beginSent == null || endSent == null) {
                throw new IllegalStateException(
                        "Unable to determine sentences in which the annotation starts/ends: " + fs);
            }

            List<Sentence> sentences = selectCovered(aJcas, Sentence.class, beginSent.getBegin(),
                    endSent.getEnd());
            List<Offsets> offsets = new ArrayList<Offsets>();
            if (sentences.size() > 1) {
                for (Sentence sentence : sentences) {
                    if (sentence.getBegin() <= fs.getBegin() && fs.getBegin() < sentence.getEnd()) {
                        offsets.add(new Offsets(fs.getBegin() - windowBegin, sentence
                                .getEnd() - windowBegin));
                    }
                    else if (sentence.getBegin() <= fs.getEnd() && fs.getEnd() <= sentence.getEnd()) {
                        offsets.add(new Offsets(sentence.getBegin() - windowBegin, fs
                                .getEnd() - windowBegin));
                    }
                    else {
                        offsets.add(new Offsets(sentence.getBegin() - windowBegin,
                                sentence.getEnd() - windowBegin));
                    }
                }
                aResponse.addEntity(new Entity(getAddr(fs), bratTypeName, offsets, bratLabelText,
                        color));
            }
            else {
                // FIXME It should be possible to remove this case and the if clause because
                // the case that a FS is inside a single sentence is just a special case
                aResponse.addEntity(new Entity(getAddr(fs), bratTypeName, new Offsets(fs.getBegin()
                        - windowBegin, fs.getEnd() - windowBegin), bratLabelText,
                        color));
            }

            // Render slots
            int fi = 0;
            for (AnnotationFeature feat : typeAdapter.listFeatures()) {
                if (MultiValueMode.ARRAY.equals(feat.getMultiValueMode())
                        && LinkMode.WITH_ROLE.equals(feat.getLinkMode())) {
                    List<LinkWithRoleModel> links = getFeature(fs, feat);
                    ArrayFS linksFS = (ArrayFS) fs.getFeatureValue(fs.getType()
                            .getFeatureByBaseName(feat.getName()));
                    for (int li = 0; li < links.size(); li++) {
                        LinkWithRoleModel link = links.get(li);
                        FeatureStructure targetFS = selectByAddr(fs.getCAS(), link.targetAddr);
                        FeatureStructure linkFS = linksFS.get(li);
                        // get the color of the link for suggestion annotations
                        color = aColoringStrategy.getColor(fs + "-" + targetFS + "-" + linkFS,
                                bratLabelText);
                        aResponse.addRelation(new Relation(new VID(getAddr(fs), fi, li),
                                bratTypeName, getArgument(fs, targetFS), link.role, color));
                    }
                }
                fi++;
            }
        }
    }
    
    /**
     * Argument lists for the arc annotation
     */
    private List<Argument> getArgument(FeatureStructure aGovernorFs, FeatureStructure aDependentFs)
    {
        return asList(new Argument("Arg1", getAddr(aGovernorFs)), new Argument("Arg2",
                getAddr(aDependentFs)));
    }
}
