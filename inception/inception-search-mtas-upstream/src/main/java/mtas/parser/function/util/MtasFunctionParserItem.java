package mtas.parser.function.util;

import mtas.parser.function.ParseException;

/**
 * The Class MtasFunctionParserItem.
 */
public class MtasFunctionParserItem
{

    /** The type. */
    private String type = null;

    /** The id. */
    private Integer id = null;

    /** The value long. */
    private Long valueLong = null;

    /** The value double. */
    private Double valueDouble = null;

    /** The degree. */
    private Integer degree = null;

    /** The parser. */
    private MtasFunctionParserFunction parser = null;

    /** The Constant TYPE_CONSTANT_LONG. */
    public static final String TYPE_CONSTANT_LONG = "constantLong";

    /** The Constant TYPE_CONSTANT_DOUBLE. */
    public static final String TYPE_CONSTANT_DOUBLE = "constantDouble";

    /** The Constant TYPE_PARSER_LONG. */
    public static final String TYPE_PARSER_LONG = "parserLong";

    /** The Constant TYPE_PARSER_DOUBLE. */
    public static final String TYPE_PARSER_DOUBLE = "parserDouble";

    /** The Constant TYPE_ARGUMENT. */
    public static final String TYPE_ARGUMENT_Q = "argumentQ";

    /** The Constant TYPE_ARGUMENT_D. */
    public static final String TYPE_ARGUMENT_D = "argumentD";

    /** The Constant TYPE_N. */
    public static final String TYPE_N = "n";

    /** The Constant TYPE_D. */
    public static final String TYPE_D = "d";

    /**
     * Instantiates a new mtas function parser item.
     *
     * @param t
     *            the t
     * @throws ParseException
     *             the parse exception
     */
    public MtasFunctionParserItem(String t) throws ParseException
    {
        if (t.equals(TYPE_N) || t.equals(TYPE_D)) {
            type = t;
            degree = 0;
        }
        else {
            throw new ParseException("unknown type " + t);
        }
    }

    /**
     * Instantiates a new mtas function parser item.
     *
     * @param t
     *            the t
     * @param i
     *            the i
     * @throws ParseException
     *             the parse exception
     */
    public MtasFunctionParserItem(String t, int i) throws ParseException
    {
        if (t.equals(TYPE_ARGUMENT_Q) || t.equals(TYPE_ARGUMENT_D)) {
            type = t;
            id = i;
            degree = 1;
        }
        else {
            throw new ParseException("unknown type " + t);
        }
    }

    /**
     * Instantiates a new mtas function parser item.
     *
     * @param t
     *            the t
     * @param l
     *            the l
     * @throws ParseException
     *             the parse exception
     */
    public MtasFunctionParserItem(String t, long l) throws ParseException
    {
        if (t.equals(TYPE_CONSTANT_LONG)) {
            type = t;
            valueLong = l;
            degree = 0;
        }
        else {
            throw new ParseException("unknown type " + t);
        }
    }

    /**
     * Instantiates a new mtas function parser item.
     *
     * @param t
     *            the t
     * @param d
     *            the d
     * @throws ParseException
     *             the parse exception
     */
    public MtasFunctionParserItem(String t, double d) throws ParseException
    {
        if (t.equals(TYPE_CONSTANT_DOUBLE)) {
            type = t;
            valueDouble = d;
            degree = 0;
        }
        else {
            throw new ParseException("unknown type " + t);
        }
    }

    /**
     * Instantiates a new mtas function parser item.
     *
     * @param t
     *            the t
     * @param p
     *            the p
     * @throws ParseException
     *             the parse exception
     */
    public MtasFunctionParserItem(String t, MtasFunctionParserFunction p) throws ParseException
    {
        if (t.equals(TYPE_PARSER_LONG)) {
            type = t;
            parser = p;
            degree = parser.degree;
        }
        else if (t.equals(TYPE_PARSER_DOUBLE)) {
            type = t;
            parser = p;
            degree = parser.degree;
        }
        else {
            throw new ParseException("unknown type " + t);
        }
    }

    /**
     * Gets the type.
     *
     * @return the type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public int getId()
    {
        return id.intValue();
    }

    /**
     * Gets the degree.
     *
     * @return the degree
     */
    public Integer getDegree()
    {
        return degree;
    }

    /**
     * Gets the value long.
     *
     * @return the value long
     */
    public long getValueLong()
    {
        return valueLong.longValue();
    }

    /**
     * Gets the value double.
     *
     * @return the value double
     */
    public double getValueDouble()
    {
        return valueDouble.doubleValue();
    }

    /**
     * Gets the parser.
     *
     * @return the parser
     */
    public MtasFunctionParserFunction getParser()
    {
        return parser;
    }

}
