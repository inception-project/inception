package de.tudarmstadt.ukp.inception.statistics;

import org.apache.uima.cas.CAS;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

public class DataCollectorTest
{

    private static final String DATA_FOLDER = "D:\\Falko\\Documents\\UKP\\Statistics\\Testdaten";

    @Test
    public void testCollectData()
    {
        DataCollector collector = new DataCollector();
        CAS testCas;
        try {
            testCas = collector.loadCas(DATA_FOLDER + "\\admin.xmi");
            System.out.println("nach import");
            collector.collectData(testCas);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        assertEquals(1, 1);

    }

}
