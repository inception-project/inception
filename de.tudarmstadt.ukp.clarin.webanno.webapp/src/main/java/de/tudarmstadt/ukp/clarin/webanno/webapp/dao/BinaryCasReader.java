/*******************************************************************************
 * Copyright 2013
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.dao;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.cas.impl.Serialization.deserializeCAS;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.TypeSystemImpl;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.core.api.resources.CompressionUtils;

public class BinaryCasReader
    extends ResourceCollectionReaderBase
{
    @Override
    public void getNext(CAS aCAS)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        InputStream is = null;
        try {
            is = CompressionUtils.getInputStream(res.getLocation(), res.getInputStream());
            BufferedInputStream bis = new BufferedInputStream(is);

            TypeSystemImpl ts = null;

            // Check if this is original UIMA CAS format or DKPro Core format
            bis.mark(10);
            DataInputStream dis = new DataInputStream(bis);
            byte[] dkproHeader = new byte[] { 'D', 'K', 'P', 'r', 'o', '1' };
            byte[] header = new byte[dkproHeader.length];
            dis.read(header);

            // If it is DKPro Core format, read the type system
            if (Arrays.equals(header, dkproHeader)) {
                ObjectInputStream ois = new ObjectInputStream(bis);
                CASMgrSerializer casMgrSerializer = (CASMgrSerializer) ois.readObject();
                ts = casMgrSerializer.getTypeSystem();
                ts.commit();
            }
            else {
                bis.reset();
            }

            if (ts == null) {
                // Check if this is a UIMA binary CAS stream
                byte[] uimaHeader = new byte[] { 'U', 'I', 'M', 'A' };

                byte[] header4 = new byte[uimaHeader.length];
                System.arraycopy(header, 0, header4, 0, header4.length);

                if (header4[0] != 'U') {
                    ArrayUtils.reverse(header4);
                }

                // If it is not a UIMA binary CAS stream, assume it is output from
                // SerializedCasWriter
                if (!Arrays.equals(header4, uimaHeader)) {
                    ObjectInputStream ois = new ObjectInputStream(bis);
                    CASCompleteSerializer serializer = (CASCompleteSerializer) ois.readObject();
                    deserializeCASComplete(serializer, (CASImpl) aCAS);
                }
                else {
                    // Since there was no type system, it must be type 0 or 4
                    deserializeCAS(aCAS, bis);
                }
            }
            else {
                // Only format 6 can have type system information
                deserializeCAS(aCAS, bis, ts, null);
            }
        }
        catch (ResourceInitializationException e) {
            throw new IOException(e);
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        finally {
            closeQuietly(is);
        }
    }
}
