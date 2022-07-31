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
package de.tudarmstadt.ukp.inception.schema.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.AutoCloseableNoException;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

class AnnotationSchemaServiceImplTest
{
    @Test
    void testCasUpgradePerformsGarbageCollection() throws Exception
    {
        CASImpl cas = (CASImpl) CasCreationUtils.createCas();
        try (AutoCloseableNoException a = cas.ll_enableV2IdRefs(true)) {
            Annotation ann = cas.createAnnotation(cas.getAnnotationType(), 0, 1);
            ann.addToIndexes();
            ann.removeFromIndexes();

            Set<FeatureStructure> allFSesBefore = new LinkedHashSet<>();
            cas.walkReachablePlusFSsSorted(allFSesBefore::add, null, null, null);

            assertThat(allFSesBefore) //
                    .as("The annotation that was added and then removed before serialization should be found") //
                    .containsExactly(cas.getSofa(), ann);

            AnnotationSchemaServiceImpl._upgradeCas(cas, cas,
                    UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription());

            Set<FeatureStructure> allFSesAfter = new LinkedHashSet<>();
            cas.walkReachablePlusFSsSorted(allFSesAfter::add, null, null, null);

            assertThat(allFSesAfter) //
                    .as("The annotation that was added and then removed before serialization should not be found") //
                    .containsExactly(cas.getSofa());
        }
    }
}
