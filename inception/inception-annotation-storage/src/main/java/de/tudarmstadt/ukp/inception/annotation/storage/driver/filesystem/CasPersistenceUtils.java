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
package de.tudarmstadt.ukp.inception.annotation.storage.driver.filesystem;

import static de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil.getRealCas;
import static java.io.ObjectInputFilter.Config.createFilter;
import static java.lang.String.join;
import static org.apache.uima.cas.SerialFormat.XMI;
import static org.apache.uima.cas.impl.Serialization.deserializeCASComplete;
import static org.apache.uima.cas.impl.Serialization.serializeCASComplete;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandles;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASCompleteSerializer;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.CASMgrSerializer;
import org.apache.uima.cas.impl.CASSerializer;
import org.apache.uima.util.CasIOUtils;
import org.apache.uima.util.TypeSystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.SnappyFramedInputStream;
import org.xerial.snappy.SnappyFramedOutputStream;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.inception.support.uima.WebAnnoCasUtil;

public final class CasPersistenceUtils
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final static ObjectInputFilter SERIALIZED_CAS_INPUT_FILTER = createFilter(join(";", //
            CASCompleteSerializer.class.getName(), //
            CASSerializer.class.getName(), //
            CASMgrSerializer.class.getName(), //
            String.class.getName(), //
            "!*"));

    private final static byte[] SNAPPY_MAGIC = new byte[] { (byte) 0xff, 0x06, 0x00, 0x00, 0x73,
            0x4e, 0x61, 0x50, 0x70, 0x59, };

    private CasPersistenceUtils()
    {
        // No instances
    }

    static void writeSerializedCas(CAS aCas, File aFile) throws IOException
    {
        FileUtils.forceMkdir(aFile.getParentFile());
        try (var os = new FileOutputStream(aFile)) {
            write(os, aCas);
        }
    }

    static void writeSerializedCasCompressed(CAS aCas, File aFile) throws IOException
    {
        FileUtils.forceMkdir(aFile.getParentFile());
        try (var os = new FileOutputStream(aFile)) {
            writeSnappyCompressed(os, aCas);
        }
    }

    public static void writeSerializedCasParanoid(CAS aCas, File aFile) throws IOException
    {
        FileUtils.forceMkdir(aFile.getParentFile());

        var realCas = getRealCas(aCas);
        // UIMA-6162 Workaround: synchronize CAS during de/serialization
        synchronized (((CASImpl) realCas).getBaseCAS()) {
            CASCompleteSerializer serializer = null;
            try {
                serializer = serializeCASComplete((CASImpl) getRealCas(aCas));

                // BEGIN SAFEGUARD --------------
                // Safeguard that we do NOT write a CAS which can afterwards not be read and thus
                // would render the document broken within the project
                // Reason we do this: https://issues.apache.org/jira/browse/UIMA-6162
                CAS dummy = getRealCas(WebAnnoCasUtil.createCas());
                deserializeCASComplete(serializer, (CASImpl) dummy);
                // END SAFEGUARD --------------
            }
            catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    preserveForDebugging(aFile, aCas, serializer);
                }
                throw new IOException(e);
            }

            try (var os = new FileOutputStream(aFile)) {
                write(os, serializer);
            }
        }
    }

    public static byte[] writeToByteArray(CAS aCas) throws IOException
    {
        try (var bos = new ByteArrayOutputStream()) {
            write(bos, aCas);
            return bos.toByteArray();
        }
    }

    public static byte[] writeToCompressedByteArray(CAS aCas) throws IOException
    {
        try (var bos = new ByteArrayOutputStream()) {
            writeSnappyCompressed(bos, aCas);
            return bos.toByteArray();
        }
    }

    private static void write(OutputStream aOut, CAS aCas) throws IOException, FileNotFoundException
    {
        var serializer = serializeCASComplete((CASImpl) getRealCas(aCas));
        write(aOut, serializer);
    }

    private static void write(OutputStream aOut, CASCompleteSerializer serializer)
        throws IOException
    {
        try (var os = new ObjectOutputStream(aOut)) {
            os.writeObject(serializer);
        }
    }

    static void writeSnappyCompressed(OutputStream aOut, CAS aCas)
        throws IOException, FileNotFoundException
    {
        var serializer = serializeCASComplete((CASImpl) getRealCas(aCas));
        try (var os = new ObjectOutputStream(new SnappyFramedOutputStream(aOut))) {
            os.writeObject(serializer);
        }
    }

    private static void preserveForDebugging(File aFile, CAS aCas,
            CASCompleteSerializer aSerializer)
    {
        long ts = System.currentTimeMillis();

        try (var xmiout = new FileOutputStream(
                new File(aFile.getPath() + ".borked-" + ts + ".xmi"))) {
            CasIOUtils.save(aCas, xmiout, XMI);
        }
        catch (Exception ex) {
            LOG.error("Debug XMI serialization failed: {}", ex.getMessage(), ex);
        }

        try (var tsout = new FileOutputStream(
                new File(aFile.getPath() + ".borked-" + ts + ".ts.xml"))) {
            TypeSystemUtil.typeSystem2TypeSystemDescription(aCas.getTypeSystem()).toXML(tsout);
        }
        catch (Exception ex) {
            LOG.error("Debug type system serialization failed: {}", ex.getMessage(), ex);
        }

        try (var os = new ObjectOutputStream(
                new FileOutputStream(new File(aFile.getPath() + ".borked-" + ts + ".ser")))) {
            os.writeObject(aSerializer);
        }
        catch (Exception ex) {
            LOG.error("Debug serialization failed: {}", ex.getMessage(), ex);
        }
    }

    public static void readSerializedCas(CAS aCas, File aFile) throws IOException
    {
        var realCas = getRealCas(aCas);
        // UIMA-6162 Workaround: synchronize CAS during de/serialization
        synchronized (((CASImpl) realCas).getBaseCAS()) {
            try (var bis = new BufferedInputStream(new FileInputStream(aFile))) {
                readSerializedCas(realCas, maybeUncompress(bis));
            }
        }
    }

    public static void readSerializedCas(CAS aCas, InputStream is) throws IOException
    {
        try (var ois = new ObjectInputStream(is)) {
            ois.setObjectInputFilter(SERIALIZED_CAS_INPUT_FILTER);
            var serializer = (CASCompleteSerializer) ois.readObject();
            deserializeCASComplete(serializer, (CASImpl) aCas);

            // Workaround for UIMA adding back deleted DocumentAnnotations
            // https://issues.apache.org/jira/browse/UIMA-6199
            // If there is a DocumentMetaData annotation, then we can drop any of the default UIMA
            // DocumentAnnotation instances (excluding the DocumentMetaData of course)
            if (!aCas.select(DocumentMetaData.class.getName()).isEmpty()) {
                aCas.select(CAS.TYPE_NAME_DOCUMENT_ANNOTATION).filter(
                        fs -> !DocumentMetaData.class.getName().equals(fs.getType().getName()))
                        .forEach(aCas::removeFsFromIndexes);
            }
        }
        catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    private static InputStream maybeUncompress(BufferedInputStream bis) throws IOException
    {
        byte[] buf = new byte[32];
        bis.mark(buf.length);
        int bytesRead = bis.read(buf);
        bis.reset();

        InputStream is = bis;
        if (isSnappyStream(buf, bytesRead)) {
            is = new SnappyFramedInputStream(bis);
        }
        return is;
    }

    private static boolean isSnappyStream(byte[] aBuffer, int aLen)
    {
        if (aLen < SNAPPY_MAGIC.length) {
            return false;
        }

        for (int i = 0; i < SNAPPY_MAGIC.length; i++) {
            if (aBuffer[i] != SNAPPY_MAGIC[i]) {
                return false;
            }
        }

        return true;
    }
}
