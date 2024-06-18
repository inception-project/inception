package mtas.parser.simple.util;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mtas.analysis.token.MtasToken;
import mtas.parser.simple.ParseException;
import mtas.search.spans.MtasSpanPrefixQuery;
import mtas.search.spans.MtasSpanRegexpQuery;
import mtas.search.spans.util.MtasSpanQuery;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanWeight;

/**
 * The Class MtasSimpleParserWordQuery.
 */
public class MtasSimpleParserWordQuery extends MtasSpanQuery {

  /** The query. */
  MtasSpanQuery query;

  /** The term. */
  Term term;

  /** The pattern word. */
  final Pattern patternWord = Pattern.compile("^([^:]+):(.*)$");
  

  /**
   * Instantiates a new mtas simple parser word query.
   *
   * @param field
   *          the field
   * @param prefix
   *          the prefix
   */
  public MtasSimpleParserWordQuery(String field, String prefix) {
    super(1, 1);
    term = new Term(field, prefix + MtasToken.DELIMITER);
    query = new MtasSpanPrefixQuery(term, true);
  }

  /**
   * Instantiates a new mtas simple parser word query.
   *
   * @param field
   *          the field
   * @param prefix
   *          the prefix
   * @param value
   *          the value
   * @throws ParseException
   *           the parse exception
   */
  public MtasSimpleParserWordQuery(String field, String prefix, String value) throws ParseException {
    super(1, 1);
    Matcher m = patternWord.matcher(value);
    if (m.find()) {
      String termBase = m.group(1) + MtasToken.DELIMITER + m.group(2);
      term = new Term(field, termBase + "\u0000*");
      query = new MtasSpanRegexpQuery(term, true);
    } else {
      String termBase = prefix + MtasToken.DELIMITER + value;
      term = new Term(field, termBase + "\u0000*");
      query = new MtasSpanRegexpQuery(term, true);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.SpanQuery#getField()
   */
  @Override
  public String getField() {
    return term.field();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.apache.lucene.search.Query#rewrite(org.apache.lucene.index.IndexReader)
   */
  @Override
  public MtasSpanQuery rewrite(IndexReader reader) throws IOException {
    return query.rewrite(reader);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
   * search.IndexSearcher, boolean)
   */
  @Override
  public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
    return query.createWeight(searcher, scoreMode, boost);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#toString(java.lang.String)
   */
  @Override
  public String toString(String field) {
    return query.toString(term.field());
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
    final MtasSimpleParserWordQuery that = (MtasSimpleParserWordQuery) obj;
    return query.equals(that.query);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.apache.lucene.search.Query#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), term, query);   
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpanQuery#disableTwoPhaseIterator()
   */
  @Override
  public void disableTwoPhaseIterator() {
    super.disableTwoPhaseIterator();
    query.disableTwoPhaseIterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see mtas.search.spans.util.MtasSpanQuery#isMatchAllPositionsQuery()
   */
  @Override
  public boolean isMatchAllPositionsQuery() {
    return false;
  }

  @Override
  public void visit(QueryVisitor aVisitor)
  {
      query.visit(aVisitor);    
  }

}
