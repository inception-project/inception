package de.tudarmstadt.ukp.inception.recommendation.imls.external.util;

import org.apache.uima.cas.CAS;
import org.assertj.core.api.Assertions;

public class InceptionAssertions
    extends Assertions {

    public static CasAssert assertThat(CAS actual)
    {
        return new CasAssert(actual);
    }
}
