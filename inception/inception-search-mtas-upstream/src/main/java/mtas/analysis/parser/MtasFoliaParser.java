/**
 * 
 */
package mtas.analysis.parser;

import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;

/**
 * The Class MtasFoliaParser.
 */
final public class MtasFoliaParser
    extends MtasXMLParser
{

    /**
     * Instantiates a new mtas folia parser.
     *
     * @param config
     *            the config
     */
    public MtasFoliaParser(MtasConfiguration config)
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
        namespaceURI = "http://ilk.uvt.nl/folia";
        namespaceURI_id = "http://www.w3.org/XML/1998/namespace";
        rootTag = "FoLiA";
        contentTag = "text";
        allowNonContent = true;
        super.initParser();
    }

}
