package mtas.search.spans;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import mtas.codec.util.CodecInfo;
import mtas.search.similarities.MtasSimScorer;
import mtas.search.spans.util.MtasSpanQuery;
import mtas.search.spans.util.MtasSpanWeight;
import mtas.search.spans.util.MtasSpans;

import org.apache.lucene.codecs.FieldsProducer;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermStates;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;

/**
 * The Class MtasSpanPositionQuery.
 */
public class MtasSpanPositionQuery extends MtasSpanQuery {

  /** The field. */
  private String field;

  /** The start. */
  private int start;

  /** The end. */
  private int end;

  /**
   * Instantiates a new mtas span position query.
   *
   * @param field the field
   * @param position the position
   */
  public MtasSpanPositionQuery(String field, int position) {
    this(field, position, position);
  }

  /**
   * Instantiates a new mtas span position query.
   *
   * @param field the field
   * @param start the start
   * @param end the end
   */
  public MtasSpanPositionQuery(String field, int start, int end) {
    super(1, 1);
    this.field = field;
    this.start = start;
    this.end = end;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.SpanQuery#getField()
   */
  @Override
  public String getField() {
    return field;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
   * search.IndexSearcher, boolean)
   */
  @Override
  public MtasSpanWeight createWeight(IndexSearcher searcher,
      ScoreMode scoreMode, float boost) throws IOException {
    return new SpanPositionWeight(searcher, null, boost);
  }

  /**
   * The Class SpanPositionWeight.
   */
  protected class SpanPositionWeight extends MtasSpanWeight {

    /** The Constant METHOD_GET_DELEGATE. */
    private static final String METHOD_GET_DELEGATE = "getDelegate";

    /** The Constant METHOD_GET_POSTINGS_READER. */
    private static final String METHOD_GET_POSTINGS_READER = "getPostingsReader";

    /**
     * Instantiates a new span position weight.
     *
     * @param searcher the searcher
     * @param termContexts the term contexts
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public SpanPositionWeight(IndexSearcher searcher,
        Map<Term, TermStates> termContexts, float boost) throws IOException {
      super(MtasSpanPositionQuery.this, searcher, termContexts, boost);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.lucene.search.spans.SpanWeight#extractTermContexts(java.util.
     * Map)
     */
    @Override
    public void extractTermStates(Map<Term, TermStates> contexts) {
      // don't do anything
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.lucene.search.spans.SpanWeight#getSpans(org.apache.lucene.
     * index.LeafReaderContext,
     * org.apache.lucene.search.spans.SpanWeight.Postings)
     */
    @Override
    public MtasSpans getSpans(LeafReaderContext context,
        Postings requiredPostings) throws IOException {
      try {
        // get leafreader
        LeafReader r = context.reader();
        // get delegate
        Boolean hasMethod = true;
        while (hasMethod) {
          hasMethod = false;
          Method[] methods = r.getClass().getMethods();
          for (Method m : methods) {
            if (m.getName().equals(METHOD_GET_DELEGATE)) {
              hasMethod = true;
              r = (LeafReader) m.invoke(r, (Object[]) null);
              break;
            }
          }
        }
        // get fieldsproducer
        Method fpm = r.getClass().getMethod(METHOD_GET_POSTINGS_READER,
            (Class<?>[]) null);
        FieldsProducer fp = (FieldsProducer) fpm.invoke(r, (Object[]) null);
        // get MtasFieldsProducer using terms
        Terms t = fp.terms(field);
        if (t == null) {
          return new MtasSpanMatchNoneSpans(MtasSpanPositionQuery.this);
        } else {
          CodecInfo mtasCodecInfo = CodecInfo.getCodecInfoFromTerms(t);
          return new MtasSpanPositionSpans(MtasSpanPositionQuery.this,
              mtasCodecInfo, field, start, end);
        }
      } catch (InvocationTargetException | IllegalAccessException
          | NoSuchMethodException e) {
        throw new IOException("Can't get reader", e);
      }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.lucene.search.spans.SpanWeight#getSimScorer(org.apache.lucene.
     * index.LeafReaderContext)
     */
    @Override
    public LeafSimScorer getSimScorer(LeafReaderContext context) throws IOException {
      return new LeafSimScorer(new MtasSimScorer(), context.reader(), field, true);
    }

//    @Override
//    public boolean isCacheable(LeafReaderContext arg0) {
//      return true;
//    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.lucene.search.spans.SpanTermQuery#toString(java.lang.String)
   */
  @Override
  public String toString(String field) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(this.getClass().getSimpleName() + "([" + start
        + (start != end ? "," + end : "") + "])");
    return buffer.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
        return true;
    }
    if (obj == null) {
        return false;
    }
    if (getClass() != obj.getClass()) {
        return false;
    }
    final MtasSpanPositionQuery that = (MtasSpanPositionQuery) obj;
    return field.equals(that.field) && start == that.start && end == that.end;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), field, start, end);   
  }
  
  @Override
  public boolean isMatchAllPositionsQuery() {
    return false;
  }

@Override
public void visit(QueryVisitor aVisitor)
{
    // don't do anything
}

}
