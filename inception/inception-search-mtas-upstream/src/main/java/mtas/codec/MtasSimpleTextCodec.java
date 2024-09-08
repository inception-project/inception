package mtas.codec;

import org.apache.lucene.codecs.simpletext.SimpleTextCodec;

/**
 * The Class MtasSimpleTextCodec.
 */
public class MtasSimpleTextCodec
    extends MtasCodec
{

    /**
     * Instantiates a new mtas simple text codec.
     */
    public MtasSimpleTextCodec()
    {
        super("MtasSimpleTextCodec", new SimpleTextCodec());
    }
}
