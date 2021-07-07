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

package de.tudarmstadt.ukp.inception.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;

public class DataCollector
{

    public void collectData(CAS aCas)
    {
        System.out.println("abc");
        TypeSystem system = aCas.getTypeSystem();
        Iterator<Feature> features = system.getFeatures();
        while (features.hasNext()) {
            System.out.println(features.next().getName());
            System.out.println("x");
        }
    }

    public CAS loadCas(String aFileName) throws Exception
    {
        System.out.println("hierx");
        try (FileInputStream fis = new FileInputStream(getResource(aFileName))) {
            JCas jcas = JCasFactory.createJCas();
            CasIOUtils.load(fis, jcas.getCas());
            return jcas.getCas();
        }
    }

    public static File getResource(String aFileName)
    {
        return Paths.get("src", "test", "resources", aFileName).toFile();

    }
}
