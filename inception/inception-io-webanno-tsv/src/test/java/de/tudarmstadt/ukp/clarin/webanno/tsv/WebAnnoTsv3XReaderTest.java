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
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.TsvSchema.FEAT_REL_TARGET;
import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.util.FSUtil.getFeature;
import static org.assertj.core.api.Assertions.assertThat;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.CasCreationUtils;
import org.junit.jupiter.api.Test;

public class WebAnnoTsv3XReaderTest
{
    @Test
    void thatRelationHasCorrectOffsets() throws Exception
    {
        var reader = createReader( //
                WebannoTsv3XReader.class, //
                WebannoTsv3XReader.PARAM_SOURCE_LOCATION,
                "src/test/resources/tsv3-suite/testSingleTokenRelationWithFeatureValue/reference.tsv");

        var cas = createTestCas();
        reader.getNext(cas);

        assertThat(cas.<Annotation> select("webanno.custom.Relation").asList()).isNotEmpty()
                .allSatisfy(relation -> {
                    var target = getFeature(relation, FEAT_REL_TARGET, Annotation.class);
                    assertThat(target.getBegin()).isEqualTo(relation.getBegin());
                    assertThat(target.getEnd()).isEqualTo(relation.getEnd());
                });
    }

    private CAS createTestCas() throws ResourceInitializationException
    {
        var global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        var local = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(
                "src/test/resources/desc/type/webannoTestTypes.xml");
        var merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        return CasFactory.createCas(merged);
    }
}
