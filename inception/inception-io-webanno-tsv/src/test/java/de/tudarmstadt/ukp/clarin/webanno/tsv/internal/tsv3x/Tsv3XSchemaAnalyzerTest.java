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
package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x;

import org.apache.uima.fit.factory.JCasFactory;
import org.junit.jupiter.api.Test;

public class Tsv3XSchemaAnalyzerTest
{
    @Test
    public void testAnalyze() throws Exception
    {
        var jcas = JCasFactory.createJCas();

        var schema = Tsv3XCasSchemaAnalyzer.analyze(jcas.getTypeSystem());

        for (var col : schema.getColumns()) {
            // System.out.println(col);
        }
    }
}
