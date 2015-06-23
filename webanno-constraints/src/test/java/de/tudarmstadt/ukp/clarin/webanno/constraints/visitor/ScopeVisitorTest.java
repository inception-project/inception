package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Scope;

public class ScopeVisitorTest
{
    @Test
    public void test()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/6.rules"));
        Parse p = parser.Parse();

        List<Scope> scopes = new ArrayList<>();
        p.accept(new ScopeVisitor(), scopes);

        for (Scope scope : scopes) {
            System.out.printf("%s %n", scope);
        }
    }

}
