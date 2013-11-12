package de.tudarmstadt.ukp.clarin.webanno.util;

import static org.uimafit.util.JCasUtil.select;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.factory.JCasFactory;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.SerializedCasReader;
import de.tudarmstadt.ukp.dkpro.core.io.bincas.SerializedCasWriter;

public class correctDependencyParser
{

    public static void main(String[] args) throws UIMAException, IOException
    {

        String file = "marc.ser";


        CAS cas = JCasFactory.createJCas().getCas();
        CollectionReader reader = CollectionReaderFactory.createCollectionReader(
                SerializedCasReader.class, SerializedCasReader.PARAM_PATH,
                "/home/likewise-open/UKP/yimam/CLARIN/bugeddata/ser/testing/2988/annotation/",
                SerializedCasReader.PARAM_PATTERNS, new String[] { "[+]" + file });
        reader.getNext(cas);

        List<Dependency> dependencies = new ArrayList<Dependency>();
        for (Dependency d : select(cas.getJCas(), Dependency.class)) {
            dependencies.add(d);
        }
        // update
        for(Dependency d: dependencies){
            Token dep = d.getDependent();
            Token gov = d.getGovernor();
            d.setDependent(gov);
            d.setGovernor(dep);
            d.addToIndexes();
        }

        File targetPath = new File("/home/likewise-open/UKP/yimam/CLARIN/bugeddata/ser/testing/2988/annotation2/");
        AnalysisEngine writer = AnalysisEngineFactory.createPrimitive(
                SerializedCasWriter.class, SerializedCasWriter.PARAM_PATH, targetPath,
                SerializedCasWriter.PARAM_USE_DOCUMENT_ID, true);
        writer.process(cas);

    }
}
