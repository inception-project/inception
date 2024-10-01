package mtas.search.similarities;

import org.apache.lucene.search.similarities.Similarity.SimScorer;

/**
 * The Class MtasSimScorer.
 */
public class MtasSimScorer
    extends SimScorer
{

    public MtasSimScorer()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.search.similarities.Similarity.SimScorer#score(int, float)
     */
    @Override
    public float score(float freq, long norm)
    {
        return 0;
    }

}
