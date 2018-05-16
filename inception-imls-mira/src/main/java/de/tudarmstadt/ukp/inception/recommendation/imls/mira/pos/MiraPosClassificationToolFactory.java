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
 */package de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos;

import org.apache.uima.cas.CAS;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationTool;
import de.tudarmstadt.ukp.inception.recommendation.api.ClassificationToolFactory;

@Component
public class MiraPosClassificationToolFactory
    implements ClassificationToolFactory<MiraConfigurationParameters, Void>
{
    // This is a string literal so we can rename/refactor the class without it changing its ID
    // and without the database starting to refer to non-existing recommendation tools.
    public static final String ID = 
            "de.tudarmstadt.ukp.inception.recommendation.imls.mira.pos.MiraPosClassificationTool";

    @Override
    public String getId()
    {
        return ID;
    }
    
    @Override
    public String getName()
    {
        return "Token Sequence Classifier (Mira)";
    }

    @Override
    public ClassificationTool<MiraConfigurationParameters> createTool(long aRecommenderId,
        String aFeature, AnnotationLayer aLayer, int aMaxPredictions)
    {
        return new MiraPosClassificationTool(aRecommenderId, aFeature, aLayer);
    }
    
    @Override
    public boolean accepts(AnnotationLayer aLayer, AnnotationFeature aFeature)
    {
        if (aLayer == null || aFeature == null) {
            return false;
        }
        
        return aLayer.isLockToTokenOffset() && "span".equals(aLayer.getType())
                && CAS.TYPE_NAME_STRING.equals(aFeature.getType());
    }
}
