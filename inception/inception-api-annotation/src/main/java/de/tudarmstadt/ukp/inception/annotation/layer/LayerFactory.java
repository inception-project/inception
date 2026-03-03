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
package de.tudarmstadt.ukp.inception.annotation.layer;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.SINGLE_TOKEN;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode.NEVER;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer.Builder;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerTypes;

public class LayerFactory
{
    public static Builder namedEntityLayer(Project aProject)
    {
        return AnnotationLayer.builder() //
                .withProject(aProject) //
                .forJCasClass(NamedEntity.class) //
                .withUiName("Named entity") //
                .withType(LayerTypes.SPAN_LAYER_TYPE) //
                .withBuiltIn(true) //
                .withAnchoringMode(TOKENS) //
                .withOverlapMode(NO_OVERLAP) //
                .withCrossSentence(false);
    }

    public static Builder partOfSpeechLayer(Project aProject, AnnotationFeature tokenPosFeature)
    {
        return AnnotationLayer.builder() //
                .withProject(aProject) //
                .forJCasClass(POS.class) //
                .withUiName("Part of speech") //
                .withType(LayerTypes.SPAN_LAYER_TYPE) //
                .withBuiltIn(true) //
                .withAnchoringMode(SINGLE_TOKEN) //
                .withOverlapMode(NO_OVERLAP) //
                .withAttachType(tokenPosFeature.getLayer()) //
                .withAttachFeature(tokenPosFeature) //
                .withCrossSentence(false);

    }

    public static Builder tokenLayer(Project aProject)
    {
        return AnnotationLayer.builder() //
                .withProject(aProject) //
                .forJCasClass(Token.class) //
                .withType(LayerTypes.SPAN_LAYER_TYPE) //
                .withBuiltIn(true) //
                .withAnchoringMode(CHARACTERS) //
                .withOverlapMode(NO_OVERLAP) //
                .withValidationMode(NEVER) //
                .withReadonly(true) //
                .withCrossSentence(false) //
                .withEnabled(false);
    }

    public static Builder sentenceLayer(Project aProject)
    {
        return AnnotationLayer.builder() //
                .withProject(aProject) //
                .forJCasClass(Sentence.class) //
                .withType(LayerTypes.SPAN_LAYER_TYPE) //
                .withBuiltIn(true) //
                .withAnchoringMode(TOKENS) //
                .withOverlapMode(NO_OVERLAP) //
                // Since the user cannot turn off validation for the sentence layer if there is any
                // kind of problem with the validation functionality we are conservative here and
                // disable validation from the start.
                .withValidationMode(NEVER) //
                .withReadonly(true) //
                .withCrossSentence(true) //
                .withEnabled(false);
    }
}
