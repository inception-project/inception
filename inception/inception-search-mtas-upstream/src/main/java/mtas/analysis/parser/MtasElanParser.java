/**
 * 
 */
package mtas.analysis.parser;

import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;

/**
 * The Class MtasElanParser.
 */
final public class MtasElanParser
    extends MtasXMLParser
{

    /**
     * Instantiates a new mtas elan parser.
     *
     * @param config
     *            the config
     */
    public MtasElanParser(MtasConfiguration config)
    {
        super(config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.analysis.parser.MtasXMLParser#initParser()
     */
    @Override
    protected void initParser() throws MtasConfigException
    {
        namespaceURI = null;
        namespaceURI_id = null;
        rootTag = "ELAN";
        contentTag = null;
        allowNonContent = true;
        super.initParser();
    }

}
