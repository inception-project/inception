/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.search;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.createCas;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.getRealCas;
import static java.io.ObjectInputFilter.Config.createFilter;
import static java.lang.String.join;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.resource.ResourceInitializationException;

public class SearchCasUtils
{
    private final static ObjectInputFilter SERIALIZED_CAS_INPUT_FILTER = createFilter(join(";", //
            CASCompleteSerializer.class.getName(), //
            CASSerializer.class.getName(), //
            CASMgrSerializer.class.getName(), //
            String.class.getName(), //
            "!*"));

    public static byte[] casToByteArray(CAS aCas) throws IOException
    {
        // Index annotation document
        var realCas = (CASImpl) getRealCas(aCas);
        // UIMA-6162 Workaround: synchronize CAS during de/serialization
        synchronized (realCas.getBaseCAS()) {
            try (var bos = new ByteArrayOutputStream()) {
                try (var oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(serializeCASComplete(realCas));
                }
                return bos.toByteArray();
            }
        }
    }

    public static CAS byteArrayToCas(byte[] aByteArray) throws IOException
    {
        CAS cas;
        try {
            cas = createCas();
        }
        catch (ResourceInitializationException e) {
            throw new IOException(e);
        }

        var realCas = (CASImpl) getRealCas(cas);
        synchronized (realCas.getBaseCAS()) {
            try (var ois = new ObjectInputStream(new ByteArrayInputStream(aByteArray))) {
                ois.setObjectInputFilter(SERIALIZED_CAS_INPUT_FILTER);
                var casCompleteSerializer = (CASCompleteSerializer) ois.readObject();
                realCas.reinit(casCompleteSerializer);
            }
            catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }

        return cas;
    }
}
