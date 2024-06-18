package mtas.parser.cql.util;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import mtas.analysis.token.MtasToken;
import mtas.parser.cql.ParseException;
import mtas.search.spans.MtasSpanMatchNoneQuery;
import mtas.search.spans.MtasSpanOperatorQuery;
import mtas.search.spans.MtasSpanOrQuery;
import mtas.search.spans.MtasSpanPrefixQuery;
import mtas.search.spans.MtasSpanRegexpQuery;
import mtas.search.spans.MtasSpanTermQuery;
import mtas.search.spans.MtasSpanWildcardQuery;
import mtas.search.spans.util.MtasSpanQuery;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanWeight;

/**
 * The Class MtasCQLParserWordQuery.
 */
public class MtasCQLParserWordQuery extends MtasSpanQuery {

  /** The query. */
  MtasSpanQuery query;

  /** The term. */
  Term term;

  /** The Constant MTAS_CQL_TERM_QUERY. */
  public static final String MTAS_CQL_TERM_QUERY = "term";

  /** The Constant MTAS_CQL_REGEXP_QUERY. */
  public static final String MTAS_CQL_REGEXP_QUERY = "regexp";

  /** The Constant MTAS_CQL_WILDCARD_QUERY. */
  public static final String MTAS_CQL_WILDCARD_QUERY = "wildcard";

  /** The Constant MTAS_CQL_VARIABLE_QUERY. */
  public static final String MTAS_CQL_VARIABLE_REGEXP_QUERY = "variable_regexp";
  public static final String MTAS_CQL_VARIABLE_TERM_QUERY = "variable_term";

  /**
   * Instantiates a new mtas CQL parser word query.
   *
   * @param field the field
   * @param prefix the prefix
   * @param variables the variables
   */
  public MtasCQLParserWordQuery(String field, String prefix,
      Map<String, String[]> variables) {
    super(1, 1);
    term = new Term(field, prefix + MtasToken.DELIMITER);
    query = new MtasSpanPrefixQuery(term, true);
  }

  /**
   * Instantiates a new mtas CQL parser word query.
   *
   * @param field the field
   * @param prefix the prefix
   * @param value the value
   * @param variables the variables
   * @param usedVariables the used variables
   * @throws ParseException the parse exception
   */
  public MtasCQLParserWordQuery(String field, String prefix, String value,
      Map<String, String[]> variables, Set<String> usedVariables)
      throws ParseException {
    this(field, prefix, value, MTAS_CQL_REGEXP_QUERY, variables, usedVariables);
  }

  /**
   * Instantiates a new mtas CQL parser word query.
   *
   * @param field the field
   * @param prefix the prefix
   * @param value the value
   * @param type the type
   * @param variables the variables
   * @param usedVariables the used variables
   * @throws ParseException the parse exception
   */
  public MtasCQLParserWordQuery(String field, String prefix, String value,
      String type, Map<String, String[]> variables, Set<String> usedVariables)
      throws ParseException {
    super(1, 1);
    String termBase = prefix + MtasToken.DELIMITER + value;
    if (type.equals(MTAS_CQL_REGEXP_QUERY)) {
      term = new Term(field, termBase + "\u0000*");      
      query = new MtasSpanRegexpQuery(term, true);
    } else if (type.equals(MTAS_CQL_WILDCARD_QUERY)) {
      term = new Term(field, termBase);
      query = new MtasSpanWildcardQuery(term, true);
    } else if (type.equals(MTAS_CQL_TERM_QUERY)) {
      term = new Term(field,
              "\"" + termBase.replace("\"", "\"\\\"\"") + "\"\u0000*");
      query = new MtasSpanOperatorQuery(field, prefix, MtasSpanOperatorQuery.MTAS_OPERATOR_DEQUAL, value, true);
    } else if (type.equals(MTAS_CQL_VARIABLE_REGEXP_QUERY) || type.equals(MTAS_CQL_VARIABLE_TERM_QUERY)) {
      if (value != null && variables != null && variables.containsKey(value)
          && variables.get(value) != null) {
        if (usedVariables.contains(value)) {
          throw new ParseException(
              "variable $" + value + " should be used exactly one time");
        } else {
          usedVariables.add(value);
        }
        String[] list = variables.get(value);
        MtasSpanQuery[] queries = new MtasSpanQuery[list.length];
        term = new Term(field, prefix + MtasToken.DELIMITER);
        for (int i = 0; i < list.length; i++) {
          termBase = prefix + MtasToken.DELIMITER + list[i];
          if(type.equals(MTAS_CQL_VARIABLE_TERM_QUERY)) {
            term = new Term(field, "\"" + termBase + "\"\u0000*");
          } else {
        	term = new Term(field, termBase + "\u0000*");  
          }
          queries[i] = new MtasSpanRegexpQuery(term, true);
        }
        if (queries.length == 0) {
          query = new MtasSpanMatchNoneQuery(field);
        } else if (queries.length > 1) {
          query = new MtasSpanOrQuery(queries);
        } else {
          query = queries[0];
        }
      } else {
        throw new ParseException("variable $" + value + " not defined");
      }
    } else {
      term = new Term(field, prefix + MtasToken.DELIMITER + value);
      query = new MtasSpanTermQuery(term, true);
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
   * @see
   * org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
   * search.IndexSearcher, boolean)
   */
  @Override
  public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost)
      throws IOException {
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
    final MtasCQLParserWordQuery that = (MtasCQLParserWordQuery) obj;
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
