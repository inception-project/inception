package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Restriction;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.RestrictionVisitor;

public class RestrictionVisitorTest
{
    @Test
    public void test()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/6.rules"));
        Parse p = parser.Parse();

        List<Restriction> restrictions = new ArrayList<>();
        p.accept(new RestrictionVisitor(), restrictions);

        for (Restriction res : restrictions) {
            System.out.printf("%s %n", res);
        }
    }
}
