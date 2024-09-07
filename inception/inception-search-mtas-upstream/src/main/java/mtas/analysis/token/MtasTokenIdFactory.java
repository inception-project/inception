package mtas.analysis.token;

/**
 * A factory for creating MtasTokenId objects.
 */
public final class MtasTokenIdFactory
{

    /** The token id. */
    Integer tokenId;

    /**
     * Instantiates a new mtas token id factory.
     */
    public MtasTokenIdFactory()
    {
        tokenId = 0;
    }

    /**
     * Creates a new MtasTokenId object.
     *
     * @return the integer
     */
    public Integer createTokenId()
    {
        return tokenId++;
    }

}
