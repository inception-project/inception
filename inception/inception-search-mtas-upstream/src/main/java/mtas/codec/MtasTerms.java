package mtas.codec;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.IndexInput;

/**
 * The Class MtasTerms.
 */
public class MtasTerms
    extends Terms
{

    /** The index input list. */
    HashMap<String, IndexInput> indexInputList;

    /** The index input offset list. */
    HashMap<String, Long> indexInputOffsetList;

    /** The version. */
    int version;

    /** The delegate terms. */
    Terms delegateTerms;

    /**
     * Instantiates a new mtas terms.
     *
     * @param terms
     *            the terms
     * @param indexInputList
     *            the index input list
     * @param indexInputOffsetList
     *            the index input offset list
     * @param version
     *            the version
     */
    public MtasTerms(Terms terms, HashMap<String, IndexInput> indexInputList,
            HashMap<String, Long> indexInputOffsetList, int version)
    {
        delegateTerms = terms;
        this.indexInputList = indexInputList;
        this.indexInputOffsetList = indexInputOffsetList;
        this.version = version;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#iterator()
     */
    @Override
    public TermsEnum iterator() throws IOException
    {
        if (delegateTerms != null) {
            return delegateTerms.iterator();
        }
        else {
            return TermsEnum.EMPTY;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#size()
     */
    @Override
    public long size() throws IOException
    {
        if (delegateTerms != null) {
            return delegateTerms.size();
        }
        else {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#getSumTotalTermFreq()
     */
    @Override
    public long getSumTotalTermFreq() throws IOException
    {
        if (delegateTerms != null) {
            return delegateTerms.getSumTotalTermFreq();
        }
        else {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#getSumDocFreq()
     */
    @Override
    public long getSumDocFreq() throws IOException
    {
        if (delegateTerms != null) {
            return delegateTerms.getSumDocFreq();
        }
        else {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#getDocCount()
     */
    @Override
    public int getDocCount() throws IOException
    {
        if (delegateTerms != null) {
            return delegateTerms.getDocCount();
        }
        else {
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#hasFreqs()
     */
    @Override
    public boolean hasFreqs()
    {
        if (delegateTerms != null) {
            return delegateTerms.hasFreqs();
        }
        else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#hasOffsets()
     */
    @Override
    public boolean hasOffsets()
    {
        if (delegateTerms != null) {
            return delegateTerms.hasOffsets();
        }
        else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#hasPositions()
     */
    @Override
    public boolean hasPositions()
    {
        if (delegateTerms != null) {
            return delegateTerms.hasPositions();
        }
        else {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.index.Terms#hasPayloads()
     */
    @Override
    public boolean hasPayloads()
    {
        if (delegateTerms != null) {
            return delegateTerms.hasPayloads();
        }
        else {
            return false;
        }
    }

    /**
     * Gets the version.
     *
     * @return the version
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Gets the index input list.
     *
     * @return the index input list
     */
    public HashMap<String, IndexInput> getIndexInputList()
    {
        HashMap<String, IndexInput> clonedIndexInputList = new HashMap<String, IndexInput>();
        for (Entry<String, IndexInput> entry : indexInputList.entrySet()) {
            clonedIndexInputList.put(entry.getKey(), entry.getValue().clone());
        }
        return clonedIndexInputList;
    }

    /**
     * Gets the index input offset list.
     *
     * @return the index input offset list
     */
    public HashMap<String, Long> getIndexInputOffsetList()
    {
        return indexInputOffsetList;
    }

}
