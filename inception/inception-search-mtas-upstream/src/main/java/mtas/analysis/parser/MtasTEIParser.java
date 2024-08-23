/**
 * 
 */
package mtas.analysis.parser;

import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;

/**
 * The Class MtasTEIParser.
 */
final public class MtasTEIParser
    extends MtasXMLParser
{

    /**
     * Instantiates a new mtas TEI parser.
     *
     * @param config
     *            the config
     */
    public MtasTEIParser(MtasConfiguration config)
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
        namespaceURI = "http://www.tei-c.org/ns/1.0";
        namespaceURI_id = "http://www.w3.org/XML/1998/namespace";
        rootTag = "TEI";
        contentTag = "text";
        allowNonContent = true;
        super.initParser();
    }

}
