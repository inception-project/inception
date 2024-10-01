package mtas.parser.cql.util;

import java.util.HashMap;
import java.util.HashSet;

import mtas.parser.cql.ParseException;

/**
 * The Class MtasCQLParserDefaultPrefixCondition.
 */
public class MtasCQLParserDefaultPrefixCondition
    extends MtasCQLParserWordCondition
{

    /**
     * Instantiates a new mtas CQL parser default prefix condition.
     *
     * @param field
     *            the field
     * @param prefix
     *            the prefix
     * @param value
     *            the value
     * @param variables
     *            the variables
     * @param usedVariables
     *            the used variables
     * @throws ParseException
     *             the parse exception
     */
    public MtasCQLParserDefaultPrefixCondition(String field, String prefix, String value,
            HashMap<String, String[]> variables, HashSet<String> usedVariables)
        throws ParseException
    {
        super(field, TYPE_AND);
        if (prefix == null) {
            throw new ParseException("no default prefix defined");
        }
        else {
            addPositiveQuery(
                    new MtasCQLParserWordQuery(field, prefix, value, variables, usedVariables));
        }
    }

}
