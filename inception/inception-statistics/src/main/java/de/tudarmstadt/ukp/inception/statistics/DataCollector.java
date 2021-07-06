package de.tudarmstadt.ukp.inception.statistics;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;

import org.apache.uima.cas.CAS;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasIOUtils;

public class DataCollector
{

    public void collectData(CAS aCas)
    {
        System.out.println(aCas.getDocumentText());
    }

    public CAS loadCas(String aPathToXmi) throws Exception
    {
        try (FileInputStream fis = new FileInputStream(getResource(aPathToXmi))) {
            JCas jcas = JCasFactory.createJCas();
            CasIOUtils.load(fis, jcas.getCas());
            return jcas.getCas();
        }
    }

    public static File getResource(String aResourceName)
    {
        return Paths.get("src", "test", "resources", aResourceName).toFile();
    }
}
