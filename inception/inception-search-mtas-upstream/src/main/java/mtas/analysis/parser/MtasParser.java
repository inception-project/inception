package mtas.analysis.parser;

import java.io.Reader;

import mtas.analysis.token.MtasTokenCollection;
import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;
import mtas.analysis.util.MtasParserException;

/**
 * The Class MtasParser.
 */
abstract public class MtasParser
{

    /** The token collection. */
    protected MtasTokenCollection tokenCollection;

    /** The config. */
    protected MtasConfiguration config;

    /** The autorepair. */
    protected Boolean autorepair = false;

    /** The makeunique. */
    protected Boolean makeunique = false;

    /** The Constant TOKEN_OFFSET. */
    protected static final String TOKEN_OFFSET = "offset";

    /** The Constant TOKEN_REALOFFSET. */
    protected static final String TOKEN_REALOFFSET = "realoffset";

    /** The Constant TOKEN_PARENT. */
    protected static final String TOKEN_PARENT = "parent";

    /**
     * Instantiates a new mtas parser.
     */
    public MtasParser()
    {
    }

    /**
     * Instantiates a new mtas parser.
     *
     * @param config
     *            the config
     */
    public MtasParser(MtasConfiguration config)
    {
        this.config = config;
    }

    /**
     * Inits the parser.
     *
     * @throws MtasConfigException
     *             the mtas config exception
     */
    protected void initParser() throws MtasConfigException
    {
        if (config != null) {
            // find namespaceURI
            for (int i = 0; i < config.children.size(); i++) {
                MtasConfiguration current = config.children.get(i);
                if (current.name.equals("autorepair")) {
                    autorepair = current.attributes.get("value").equals("true");
                }
                if (current.name.equals("makeunique")) {
                    makeunique = current.attributes.get("value").equals("true");
                }
            }
        }
    }

    /**
     * Creates the token collection.
     *
     * @param reader
     *            the reader
     * @return the mtas token collection
     * @throws MtasParserException
     *             the mtas parser exception
     * @throws MtasConfigException
     *             the mtas config exception
     */
    public abstract MtasTokenCollection createTokenCollection(Reader reader)
        throws MtasParserException, MtasConfigException;

    /**
     * Prints the config.
     *
     * @return the string
     */
    public abstract String printConfig();

}
