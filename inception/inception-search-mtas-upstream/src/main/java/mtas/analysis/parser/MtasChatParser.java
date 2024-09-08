package mtas.analysis.parser;

import mtas.analysis.util.MtasConfigException;
import mtas.analysis.util.MtasConfiguration;

/**
 * The Class MtasChatParser.
 */
final public class MtasChatParser
    extends MtasXMLParser
{

    /**
     * Instantiates a new mtas chat parser.
     *
     * @param config
     *            the config
     */
    public MtasChatParser(MtasConfiguration config)
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
        namespaceURI = "http://www.talkbank.org/ns/talkbank";
        namespaceURI_id = null;
        rootTag = "CHAT";
        super.initParser();
    }

}
