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
package de.tudarmstadt.ukp.inception.io.brat.dkprocore;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.util.CasCreationUtils.mergeTypeSystems;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.CasFactory;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.support.WebAnnoConst;

class BratReaderTest
{
    @Test
    void test() throws Exception
    {
        var mapping = """
                {
                  'textTypeMapppings': [
                    {
                      'from': '.*',
                      'to': 'custom.Span',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'relationTypeMapppings': [
                    {
                      'from': '.*',
                      'to': 'custom.Relation',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'spans': [
                    {
                      'type': 'custom.Span',
                      'subCatFeature': 'value',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'relations': [
                    {
                      'type': 'custom.Relation',
                      'arg1': 'Governor',
                      'arg2': 'Dependent',
                      'flags2': 'A',
                      'subCatFeature': 'value',
                      'defaultFeatureValues': { }
                    }
                  ],
                  'comments': [ ]
                }
                """;

        var tsd = UIMAFramework.getResourceSpecifierFactory().createTypeSystemDescription();

        var customSpanType = tsd.addType("custom.Span", "", CAS.TYPE_NAME_ANNOTATION);
        customSpanType.addFeature("value", "", CAS.TYPE_NAME_STRING);

        var customRelationType = tsd.addType("custom.Relation", "", CAS.TYPE_NAME_ANNOTATION);
        customRelationType.addFeature(WebAnnoConst.FEAT_REL_SOURCE, "", CAS.TYPE_NAME_ANNOTATION);
        customRelationType.addFeature(WebAnnoConst.FEAT_REL_TARGET, "", CAS.TYPE_NAME_ANNOTATION);
        customRelationType.addFeature("value", "", CAS.TYPE_NAME_STRING);

        var fullTsd = mergeTypeSystems(asList(tsd, createTypeSystemDescription()));

        var reader = createReader(BratReader.class, //
                BratReader.PARAM_SOURCE_LOCATION, "src/test/resources/corpora/TDT-b105-sample.zip", //
                BratReader.PARAM_LENIENT, true, //
                BratReader.PARAM_MAPPING, mapping);

        var cas = CasFactory.createCas(fullTsd);
        reader.getNext(cas);
    }
}
