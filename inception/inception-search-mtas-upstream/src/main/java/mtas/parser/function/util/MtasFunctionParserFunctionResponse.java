package mtas.parser.function.util;

/**
 * The Class MtasFunctionParserFunctionResponse.
 */
abstract public class MtasFunctionParserFunctionResponse
{

    /** The defined. */
    boolean defined;

    /**
     * Instantiates a new mtas function parser function response.
     *
     * @param s
     *            the s
     */
    protected MtasFunctionParserFunctionResponse(boolean s)
    {
        defined = s;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    abstract public boolean equals(Object obj);

}
