package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ImportVisitor;

public class ImportVisitorTest
{
    @Test
    public void test()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/6.rules"));
        Parse p = parser.Parse();

        Map<String, String> imports = new LinkedHashMap<>();
        p.accept(new ImportVisitor(), imports);

        for (Entry<String, String> e : imports.entrySet()) {
            System.out.printf("[%s] is short for [%s]%n", e.getKey(), e.getValue());
        }

        assertEquals("de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
                imports.get("Lemma"));
    }
}
