package mtas.codec;

import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.CompoundFormat;
import org.apache.lucene.codecs.DocValuesFormat;
import org.apache.lucene.codecs.FieldInfosFormat;
import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.codecs.LiveDocsFormat;
import org.apache.lucene.codecs.NormsFormat;
import org.apache.lucene.codecs.PointsFormat;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.SegmentInfoFormat;
import org.apache.lucene.codecs.StoredFieldsFormat;
import org.apache.lucene.codecs.TermVectorsFormat;
import org.apache.lucene.codecs.perfield.PerFieldPostingsFormat;

/**
 * The Class MtasCodec.
 */
public class MtasCodec
    extends Codec
{

    /** The Constant MTAS_CODEC_NAME. */
    public static final String MTAS_CODEC_NAME = "MtasCodec";

    /** The delegate. */
    Codec delegate;

    /**
     * Instantiates a new mtas codec.
     */
    public MtasCodec()
    {
        super(MTAS_CODEC_NAME);
        delegate = null;
    }

    /**
     * Instantiates a new mtas codec.
     *
     * @param name
     *            the name
     * @param delegate
     *            the delegate
     */
    protected MtasCodec(String name, Codec delegate)
    {
        super(name);
        this.delegate = delegate;
    }

    /**
     * Inits the delegate.
     */
    private void initDelegate()
    {
        if (delegate == null) {
            delegate = Codec.getDefault();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#postingsFormat()
     */
    @Override
    public PostingsFormat postingsFormat()
    {
        initDelegate();
        if (delegate.postingsFormat() instanceof PerFieldPostingsFormat) {
            Codec defaultCodec = Codec.getDefault();
            PostingsFormat defaultPostingsFormat = defaultCodec.postingsFormat();
            if (defaultPostingsFormat instanceof PerFieldPostingsFormat) {
                defaultPostingsFormat = ((PerFieldPostingsFormat) defaultPostingsFormat)
                        .getPostingsFormatForField("");
                if ((defaultPostingsFormat == null)
                        || (defaultPostingsFormat instanceof PerFieldPostingsFormat)) {
                    // fallback option
                    return new MtasCodecPostingsFormat(PostingsFormat.forName("Lucene84"));
                }
                else {
                    return new MtasCodecPostingsFormat(defaultPostingsFormat);
                }
            }
            else {
                return new MtasCodecPostingsFormat(defaultPostingsFormat);
            }
        }
        else {
            return new MtasCodecPostingsFormat(delegate.postingsFormat());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#docValuesFormat()
     */
    @Override
    public DocValuesFormat docValuesFormat()
    {
        initDelegate();
        return delegate.docValuesFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#storedFieldsFormat()
     */
    @Override
    public StoredFieldsFormat storedFieldsFormat()
    {
        initDelegate();
        return delegate.storedFieldsFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#termVectorsFormat()
     */
    @Override
    public TermVectorsFormat termVectorsFormat()
    {
        initDelegate();
        return delegate.termVectorsFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#fieldInfosFormat()
     */
    @Override
    public FieldInfosFormat fieldInfosFormat()
    {
        initDelegate();
        return delegate.fieldInfosFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#segmentInfoFormat()
     */
    @Override
    public SegmentInfoFormat segmentInfoFormat()
    {
        initDelegate();
        return delegate.segmentInfoFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#normsFormat()
     */
    @Override
    public NormsFormat normsFormat()
    {
        initDelegate();
        return delegate.normsFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#liveDocsFormat()
     */
    @Override
    public LiveDocsFormat liveDocsFormat()
    {
        initDelegate();
        return delegate.liveDocsFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#compoundFormat()
     */
    @Override
    public CompoundFormat compoundFormat()
    {
        initDelegate();
        return delegate.compoundFormat();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.codecs.Codec#pointsFormat()
     */
    @Override
    public PointsFormat pointsFormat()
    {
        initDelegate();
        return delegate.pointsFormat();
    }

    @Override
    public KnnVectorsFormat knnVectorsFormat()
    {
        return delegate.knnVectorsFormat();
    }

}
