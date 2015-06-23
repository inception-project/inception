package de.tudarmstadt.ukp.clarin.webanno.constraints.eval;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
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

public class ConstraintsGeneratorTest
{
    @Test
    public void testSimpleFeature()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/9.rules"));
        Parse p = parser.Parse();

        ParsedConstraints constraints = p.accept(new ParserVisitor());

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("is");

        Lemma lemma = new Lemma(jcas, 0, 2);
        lemma.setValue("be");
        lemma.addToIndexes();

        Evaluator constraintsEvaluator = new ValuesGenerator();

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(lemma,
                "value", constraints);

        List<PossibleValue> expectedOutput = new LinkedList<PossibleValue>();
        expectedOutput.add(new PossibleValue("be", true));

        assertEquals(expectedOutput, possibleValues);
    }

    @Test
    public void testSimplePath()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/10.rules"));
        Parse p = parser.Parse();

        ParsedConstraints constraints = p.accept(new ParserVisitor());

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("The sun.");

        // Add token annotations
        Token t_the = new Token(jcas, 0, 3);
        t_the.addToIndexes();
        Token t_sun = new Token(jcas, 0, 3);
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

        List<PossibleValue> possibleValues = constraintsEvaluator.generatePossibleValues(
                dep_the_sun, "DependencyType", constraints);

        List<PossibleValue> expectedOutput = new LinkedList<PossibleValue>();
        expectedOutput.add(new PossibleValue("det", false));

        assertEquals(expectedOutput, possibleValues);
    }
}
