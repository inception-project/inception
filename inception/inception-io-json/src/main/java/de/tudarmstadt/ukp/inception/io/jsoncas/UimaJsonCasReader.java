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
package de.tudarmstadt.ukp.inception.io.jsoncas;

import java.io.IOException;

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.json.jsoncas2.JsonCas2Deserializer;
import org.apache.uima.json.jsoncas2.mode.FeatureStructuresMode;
import org.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;

/**
 * UIMA JSON CAS format reader.
 */
public class UimaJsonCasReader
    extends JCasResourceCollectionReader_ImplBase
{
    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(aJCas, res);

        var jds = new JsonCas2Deserializer();
        jds.setFsMode(FeatureStructuresMode.AS_ARRAY);

        try (var is = res.getInputStream()) {
            jds.deserialize(is, aJCas.getCas());
        }
    }
}
