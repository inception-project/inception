package de.tudarmstadt.ukp.inception.recommendation.imls.external.util;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.fit.util.CasUtil;
import org.apache.uima.fit.util.FSUtil;
import org.assertj.core.api.AbstractAssert;

import de.tudarmstadt.ukp.inception.recommendation.api.type.PredictedSpan;

public class CasAssert
    extends AbstractAssert<CasAssert, CAS>
{
    private static String TYPE_NE = "de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity";

    public CasAssert(CAS cas)
    {
        super(cas, CasAssert.class);
    }

    public static CasAssert assertThat(CAS actual) 
    {
        return new CasAssert(actual);
    }

    public CasAssert containsNamedEntity(String text, String value)
    {
        isNotNull();

        Type type = CasUtil.getType(actual, TYPE_NE);
        for (AnnotationFS annotation : CasUtil.select(actual, type)) {
            if (annotation.getCoveredText().equals(text) &&
                FSUtil.getFeature(annotation, "value", String.class).equals(value)) {
                return this;
            }
        }

        failWithMessage("No named entity with text <%s> and value <%s> found", text, value);

        return this;
    }

    public CasAssert containsPrediction(String text, String label)
    {
        isNotNull();

        Type type = CasUtil.getType(actual, PredictedSpan.class);
        for (AnnotationFS annotation : CasUtil.select(actual, type)) {
            if (annotation.getCoveredText().equals(text) &&
                FSUtil.getFeature(annotation, "label", String.class).equals(label)) {
                return this;
            }
        }

        failWithMessage("No named entity with text <%s> and label <%s> found", text, label);

        return this;
    }

}
