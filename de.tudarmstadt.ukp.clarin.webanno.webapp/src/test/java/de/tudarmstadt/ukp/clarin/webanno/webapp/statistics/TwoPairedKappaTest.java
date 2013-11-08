package de.tudarmstadt.ukp.clarin.webanno.webapp.statistics;

import static org.junit.Assert.assertEquals;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.uimafit.factory.JCasFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.User;
import de.tudarmstadt.ukp.clarin.webanno.tcf.TcfReader;
import de.tudarmstadt.ukp.clarin.webanno.tsv.WebannoTsvReader;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 * Unit Test for Kappa Agreement. The example reads two TSV files with POS and DEP annotations for
 * two users and check the disagreement
 * @author yimam
 *
 */
public class TwoPairedKappaTest
{

    @Test
    public void test()
        throws Exception
    {
        User user1 = new User();
        user1.setUsername("user1");

        User user2 = new User();
        user2.setUsername("user2");

        SourceDocument document = new SourceDocument();
        document.setName("kappatest.tsv");

        CAS cas1 = JCasFactory.createJCas().getCas();

        CollectionReader reader1 = createCollectionReader(WebannoTsvReader.class,
                WebannoTsvReader.PARAM_PATH, new File("src/test/resources/").getAbsolutePath(),
                TcfReader.PARAM_PATTERNS, new String[] { "[+]kappatest.tsv" });

        reader1.getNext(cas1);

        CAS cas2 = JCasFactory.createJCas().getCas();

        CollectionReader reader2 = createCollectionReader(WebannoTsvReader.class,
                WebannoTsvReader.PARAM_PATH, new File("src/test/resources/").getAbsolutePath(),
                TcfReader.PARAM_PATTERNS, new String[] { "[+]kappatest2.tsv" });

        reader2.getNext(cas2);
        Map<User, JCas> JCases = new HashMap<User, JCas>();
        JCases.put(user1, cas1.getJCas());
        JCases.put(user2, cas2.getJCas());

        double[][] results = new double[2][2];
        TwoPairedKappa kappa = new TwoPairedKappa();
        Map<String, Map<String, String>> allUserAnnotations = new TreeMap<String, Map<String, String>>();

        //Tes POS agreement
        kappa.getStudy(POS.class.getName(), "PosValue", user1, user2, allUserAnnotations, document,
                JCases);
        results = kappa.getAgreement(allUserAnnotations);
        assertEquals(results[0][1], 0.8999999999999999,0.0005);

        // Test Dependency Agreement
        kappa = new TwoPairedKappa();
        kappa.getStudy(Dependency.class.getName(), "DependencyType", user1, user2, allUserAnnotations, document,
                JCases);
        results = kappa.getAgreement(allUserAnnotations);
        assertEquals(results[0][1], 0.902127659574468,0.0001);
    }
}
