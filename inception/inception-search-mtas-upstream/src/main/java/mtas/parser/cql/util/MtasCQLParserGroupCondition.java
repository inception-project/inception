package mtas.parser.cql.util;

import java.util.Objects;

import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasCQLParserGroupCondition.
 */
public class MtasCQLParserGroupCondition {

  /** The condition. */
  private MtasSpanQuery condition;

  /** The field. */
  private String field;

  /**
   * Instantiates a new mtas CQL parser group condition.
   *
   * @param field the field
   * @param condition the condition
   */
  public MtasCQLParserGroupCondition(String field, MtasSpanQuery condition) {
    this.field = field;
    this.condition = condition;
  }

  /**
   * Field.
   *
   * @return the string
   */
  public String field() {
    return field;
  }

  /**
   * Gets the query.
   *
   * @return the query
   */
  public MtasSpanQuery getQuery() {
    return condition;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object object) {
    if (object != null && object instanceof MtasCQLParserGroupCondition) {
      MtasCQLParserGroupCondition groupCondition = (MtasCQLParserGroupCondition) object;
      return field.equals(groupCondition.field)
          && condition.equals(groupCondition.condition);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return Objects.hash(this.getClass().getSimpleName(), field, condition);   
  }
}
