/*
 * Copyright 2015
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
package de.tudarmstadt.ukp.clarin.webanno.constraints;

import java.util.Arrays;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Test;

public class CustomTypesTest
{
    @Test
    public void testProfType()
        throws Exception
    {
        TypeSystemDescription tsd = TypeSystemDescriptionFactory
                .createTypeSystemDescription("desc.types.TestTypeSystemDescriptor");

        CAS cas = CasCreationUtils.createCas(tsd, null, null);
        cas.setDocumentText("I listen to lectures by Prof. Gurevych sometimes.");

        TypeSystem ts = cas.getTypeSystem();
        Type profType = ts.getType("de.tud.Prof");
        Feature profNameFeature = profType.getFeatureByBaseName("fullName");
        Feature profBossFeature = profType.getFeatureByBaseName("boss");

        AnnotationFS proemel = cas.createAnnotation(profType, 0, 0);
        proemel.setStringValue(profNameFeature, "Hans Juergen Proeml");
        cas.addFsToIndexes(proemel);

        AnnotationFS gurevych = cas.createAnnotation(profType, 24, 38);
        gurevych.setStringValue(profNameFeature, "Iryna Gurevych");
        gurevych.setFeatureValue(profBossFeature, proemel);
        cas.addFsToIndexes(gurevych);

        for (String feature : Arrays.asList("fullName", "boss")) {
            Feature someFeature = gurevych.getType().getFeatureByBaseName(feature);
            if (someFeature.getRange().isPrimitive()) {
                String value = gurevych.getFeatureValueAsString(someFeature);
                System.out.println(value);
            }
            else {
                FeatureStructure value = gurevych.getFeatureValue(someFeature);
                System.out.printf("%s (%s)%n", value.getFeatureValueAsString(profNameFeature),
                        value.getType());
            }
        }
    }
}
