package de.tudarmstadt.ukp.clarin.webanno.constraints;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Evaluator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.PossibleValue;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ValuesGenerator;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ParserVisitor;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

public class SymbolicRulesTest
{
    @Test
    public void testSimpleSymbolicRules()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/symbolic1.rules"));
        Parse p = parser.Parse();

        ParsedConstraints constraints = p.accept(new ParserVisitor());

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("this is a complex document");

        Lemma lemma = new Lemma(jcas, 0, 4);
        lemma.setValue("be");
        lemma.addToIndexes();

        POS pos = new POS(jcas, 0, 4);
        pos.setPosValue("pronoun");
        pos.addToIndexes();

        // Add token annotations
        Token t_the = new Token(jcas, 0, 4);
        t_the.addToIndexes();
        Token t_sun = new Token(jcas, 0, 4);
        t_sun.addToIndexes();

        // Add POS annotations and link them to the tokens
        POS p_the = new POS(jcas, t_the.getBegin(), t_the.getEnd());
        p_the.setPosValue("DET");
        p_the.addToIndexes();
        t_the.setPos(p_the);
        POS p_sun = new POS(jcas, t_sun.getBegin(), t_sun.getEnd());
        p_sun.setPosValue("NN");
        p_sun.addToIndexes();
        t_sun.setPos(p_sun);

        // Add dependency annotations
        Dependency dep_the_sun = new Dependency(jcas);
        dep_the_sun.setGovernor(t_sun);
        dep_the_sun.setDependent(t_the);
        dep_the_sun.setDependencyType("det");
        dep_the_sun.setBegin(dep_the_sun.getGovernor().getBegin());
        dep_the_sun.setEnd(dep_the_sun.getGovernor().getEnd());
        dep_the_sun.addToIndexes();

        Evaluator constraintsEvaluator = new ValuesGenerator();

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(lemma,
                "value", constraints);

        List<PossibleValue> expectedOutput = new ArrayList<PossibleValue>();
        expectedOutput.add(new PossibleValue("good", true));

        assertEquals(expectedOutput, possibleValues);

    }

}
