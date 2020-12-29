/*
 * Copyright 2019
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
 */
package de.tudarmstadt.ukp.inception.search;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static org.apache.uima.cas.SerialFormat.SERIALIZED_TSI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.util.CasIOUtils;

public class SearchCasUtils
{
    public static byte[] casToByteArray(CAS aCas) throws IOException
    {
        // Index annotation document
        CAS realCas = getRealCas(aCas);
        // UIMA-6162 Workaround: synchronize CAS during de/serialization
        synchronized (((CASImpl) realCas).getBaseCAS()) {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                // XmiCasSerializer.serialize(aCas, null, bos, true, null);
                CasIOUtils.save(realCas, bos, SERIALIZED_TSI);
                return bos.toByteArray();
            }
        }
    }
}
