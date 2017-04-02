package de.tudarmstadt.ukp.clarin.webanno.api.dao;

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
            catch (CASException e) {
                throw new IOException(e);
            }
            catch (ClassNotFoundException e) {
                throw new IOException(e);
            }
        }
}
