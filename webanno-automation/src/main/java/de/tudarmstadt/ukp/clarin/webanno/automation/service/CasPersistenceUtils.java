/*
 * Copyright 2012
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
package de.tudarmstadt.ukp.clarin.webanno.automation.service;

import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.jcas.JCas;

public final class CasPersistenceUtils
{

    private CasPersistenceUtils()
    {
        // No instances
    }

    public static void writeSerializedCas(JCas aJCas, File aFile)
        throws IOException
    {
        FileUtils.forceMkdir(aFile.getParentFile());

        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(aFile))) {
            CASCompleteSerializer serializer = serializeCASComplete(aJCas.getCasImpl());
            os.writeObject(serializer);
        }
    }

    public static void readSerializedCas(JCas aJCas, File aFile)
        throws IOException
    {
        try (ObjectInputStream is = new ObjectInputStream(new FileInputStream(aFile))) {
            CASCompleteSerializer serializer = (CASCompleteSerializer) is.readObject();
            deserializeCASComplete(serializer, aJCas.getCasImpl());
            // Initialize the JCas sub-system which is the most often used API in DKPro Core
            // components
            aJCas.getCas().getJCas();
        }
        catch (CASException | ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
