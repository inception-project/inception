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
package de.tudarmstadt.ukp.clarin.webanno.diag.checks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.diag.CasDoctor;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.LogMessage;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class AllAnnotationsIndexedCheckTest
{
    @Test
    public void testFail()
        throws Exception
    {
        TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
                .createTypeSystemDescription();
        
        String refTypeName = "RefType";
        
        TypeDescription refTypeDesc = tsd.addType(refTypeName, null, CAS.TYPE_NAME_ANNOTATION);
        refTypeDesc.addFeature("ref", null, CAS.TYPE_NAME_ANNOTATION);
        
        CAS cas = CasCreationUtils.createCas(tsd, null, null);
        
        Type refType = cas.getTypeSystem().getType(refTypeName);
        
        // A regular index annotation
        AnnotationFS anno1 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
        cas.addFsToIndexes(anno1);

        // A non-index annotation but reachable through an indexe one (below)
        AnnotationFS anno2 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);

        // An indexed annotation that references the non-indexed annotation above
        AnnotationFS anno3 = cas.createAnnotation(refType, 0, 1);
        anno3.setFeatureValue(refType.getFeatureByBaseName("ref"), anno2);
        cas.addFsToIndexes(anno3);
        
        List<LogMessage> messages = new ArrayList<>();
        CasDoctor cd = new CasDoctor(AllFeatureStructuresIndexedCheck.class);
        // A project is not required for this check
        boolean result = cd.analyze(null, cas, messages);
        
        messages.forEach(System.out::println);
        
        assertFalse(result);
    }

    @Test
    public void testOK()
        throws Exception
    {
        TypeSystemDescription tsd = UIMAFramework.getResourceSpecifierFactory()
                .createTypeSystemDescription();
        
        String refTypeName = "RefType";
        
        TypeDescription refTypeDesc = tsd.addType(refTypeName, null, CAS.TYPE_NAME_ANNOTATION);
        refTypeDesc.addFeature("ref", null, CAS.TYPE_NAME_ANNOTATION);
        
        CAS cas = CasCreationUtils.createCas(tsd, null, null);
        
        Type refType = cas.getTypeSystem().getType(refTypeName);
        
        // A regular index annotation
        AnnotationFS anno1 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
        cas.addFsToIndexes(anno1);

        // An indexed annotation but reachable through an indexe one (below)
        AnnotationFS anno2 = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
        cas.addFsToIndexes(anno2);

        // An indexed annotation that references the non-indexed annotation above
        AnnotationFS anno3 = cas.createAnnotation(refType, 0, 1);
        anno3.setFeatureValue(refType.getFeatureByBaseName("ref"), anno2);
        cas.addFsToIndexes(anno3);
        
        List<LogMessage> messages = new ArrayList<>();
        CasDoctor cd = new CasDoctor(AllFeatureStructuresIndexedCheck.class);
        // A project is not required for this check
        boolean result = cd.analyze(null, cas, messages);
        
        messages.forEach(System.out::println);
        
        assertTrue(result);
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
