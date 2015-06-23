package de.tudarmstadt.ukp.clarin.webanno.constraints.eval;

import static org.apache.uima.fit.util.JCasUtil.select;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.ConstraintsVerifier;
import de.tudarmstadt.ukp.clarin.webanno.constraints.evaluator.Verifiable;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.ConstraintsGrammar;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.Parse;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.ParsedConstraints;
import de.tudarmstadt.ukp.clarin.webanno.constraints.model.Rule;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ImportVisitor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.ParserVisitor;
import de.tudarmstadt.ukp.clarin.webanno.constraints.visitor.RuleVisitor;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;

public class ConstraintsVerifierTest
{
    @Test
    public void test()
        throws Exception
    {
        ConstraintsGrammar parser = new ConstraintsGrammar(new FileInputStream(
                "src/test/resources/rules/6.rules"));
        Parse p = parser.Parse();
        ParsedConstraints constraints = p.accept(new ParserVisitor());

        // Get imports
        Map<String, String> imports = new LinkedHashMap<>();
        imports = constraints.getImports();

        // Get rules
        // List<Rule> rules = new ArrayList<>();

        JCas jcas = JCasFactory.createJCas();
        jcas.setDocumentText("Just some text.");

        Lemma lemma1 = new Lemma(jcas, 0, 1);
        lemma1.setValue("good");
        lemma1.addToIndexes();

        Lemma lemma2 = new Lemma(jcas, 1, 2);
        lemma2.setValue("bad");
        lemma2.addToIndexes();

        Verifiable cVerifier = new ConstraintsVerifier();

        for (Lemma lemma : select(jcas, Lemma.class)) {
            if (lemma == lemma1) {
                assertEquals(true, cVerifier.verify(lemma, constraints));
            }
            if (lemma == lemma2) {
                assertEquals(false, cVerifier.verify(lemma, constraints));
            }
        }
    }

    // private boolean verify(FeatureStructure aFS, Map<String, String>
    // aImports, List<Rule> aRules)
    // {
    // boolean isOk = false;
    // Type type = aFS.getType();
    // for (Feature feature : type.getFeatures()) {
    // if (feature.getRange().isPrimitive()) {
    // String value = aFS.getFeatureValueAsString(feature);
    // // Check if all the feature values are ok according to the rules;
    // }
    // else {
    // // Here some recursion would be in order
    // }
    // }
    // return isOk;
    // }
}
