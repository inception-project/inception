package mtas.search.spans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import mtas.analysis.token.MtasToken;
import mtas.search.spans.util.MtasSpanQuery;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.AutomatonQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreMode;
import org.apache.lucene.queries.spans.SpanMultiTermQueryWrapper;
import org.apache.lucene.queries.spans.SpanOrQuery;
import org.apache.lucene.queries.spans.SpanQuery;
import org.apache.lucene.queries.spans.SpanTermQuery;
import org.apache.lucene.queries.spans.SpanWeight;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.automaton.Automata;
import org.apache.lucene.util.automaton.Automaton;
import org.apache.lucene.util.automaton.Operations;

/**
 * The Class MtasSpanComparatorQuery.
 */
public class MtasSpanOperatorQuery extends MtasSpanQuery {

	/** The field. */
	private String field;

	/** The comparator. */
	private String operator;

	/** The prefix. */
	private String prefix;

	/** The ivalue. */
	private int ivalue;

	/** The svalue. */
	private String svalue;

	/** The single position. */
	private boolean singlePosition;

	/** The query. */
	private SpanMultiTermQueryWrapper<AutomatonQuery> query;

	/** The Constant MTAS_OPERATOR_EQUAL. */
	public static final String MTAS_OPERATOR_EQUAL = "=";

	/** The Constant MTAS_OPERATOR_DEQUAL. */
	public static final String MTAS_OPERATOR_DEQUAL = "==";

	/** The Constant MTAS_OPERATOR_LESS_THAN. */
	public static final String MTAS_OPERATOR_LESS_THAN = "<";

	/** The Constant MTAS_OPERATOR_LESS_THAN. */
	public static final String MTAS_OPERATOR_LESS_THAN_OR_EQUAL = "<=";

	/** The Constant MTAS_OPERATOR_MORE_THAN. */
	public static final String MTAS_OPERATOR_MORE_THAN = ">";

	/** The Constant MTAS_OPERATOR_MORE_THAN_OR_EQUAL. */
	public static final String MTAS_OPERATOR_MORE_THAN_OR_EQUAL = ">=";

	/**
	 * Instantiates a new mtas span comparator query.
	 *
	 * @param field    the field
	 * @param prefix   the prefix
	 * @param operator the operator
	 * @param value    the value
	 * @param singlePosition the single position
	 */
	public MtasSpanOperatorQuery(String field, String prefix, String operator, int value, boolean singlePosition) {
		super(singlePosition ? 1 : null, singlePosition ? 1 : null);
		this.field = field;
		this.prefix = prefix;
		this.operator = operator;
		this.singlePosition = singlePosition;
		this.ivalue = value;
		this.svalue = Integer.toString(value);
		Term term = new Term(field, prefix + MtasToken.DELIMITER + Integer.toString(value));
		Automaton a = toAutomaton(operator, prefix, value);
		AutomatonQuery auq = new AutomatonQuery(term, a);
		// RegexpQuery req = new RegexpQuery(term);
		query = new SpanMultiTermQueryWrapper<>(auq);
	}

	/**
	 * Instantiates a new mtas span operator query.
	 *
	 * @param field the field
	 * @param prefix the prefix
	 * @param operator the operator
	 * @param value the value
	 * @param singlePosition the single position
	 */
	public MtasSpanOperatorQuery(String field, String prefix, String operator, String value, boolean singlePosition) {
		super(singlePosition ? 1 : null, singlePosition ? 1 : null);
		this.field = field;
		this.prefix = prefix;
		this.operator = operator;
		this.singlePosition = singlePosition;
		this.svalue = value;
		this.ivalue = 0;
		Term term = new Term(field, prefix + MtasToken.DELIMITER + svalue);
		Automaton a = toAutomaton(operator, prefix, value);
		AutomatonQuery auq = new AutomatonQuery(term, a);
		query = new SpanMultiTermQueryWrapper<>(auq);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mtas.search.spans.util.MtasSpanQuery#rewrite(org.apache.lucene.index.
	 * IndexReader)
	 */
	@Override
	public MtasSpanQuery rewrite(IndexReader reader) throws IOException {
		Query q = query.rewrite(reader);
		if (q instanceof SpanOrQuery) {
			SpanQuery[] clauses = ((SpanOrQuery) q).getClauses();
			MtasSpanQuery[] newClauses = new MtasSpanQuery[clauses.length];
			for (int i = 0; i < clauses.length; i++) {
				if (clauses[i] instanceof SpanTermQuery) {
					newClauses[i] = new MtasSpanTermQuery((SpanTermQuery) clauses[i], true).rewrite(reader);
				} else {
					throw new IOException("no SpanTermQuery after rewrite");
				}
			}
			return new MtasSpanOrQuery(newClauses).rewrite(reader);
		} else {
			throw new IOException("no SpanOrQuery after rewrite");
		}
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see mtas.search.spans.util.MtasSpanQuery#equals(java.lang.Object)
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
		MtasSpanOperatorQuery other = (MtasSpanOperatorQuery) obj;
		return singlePosition==other.singlePosition && field.equals(other.field) && operator.equals(other.operator) && prefix.equals(other.prefix)
				&& (ivalue == other.ivalue) && (svalue.equals(other.svalue));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see mtas.search.spans.util.MtasSpanQuery#hashCode()
	 */
	@Override
	public int hashCode() {
		return Objects.hash(this.getClass().getSimpleName(), field, prefix, operator, ivalue, svalue);
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
	 * @see org.apache.lucene.search.spans.SpanQuery#createWeight(org.apache.lucene.
	 * search.IndexSearcher, org.apache.lucene.search.ScoreMode, float)
	 */
	@Override
	public SpanWeight createWeight(IndexSearcher searcher, ScoreMode scoreMode, float boost) throws IOException {
		return ((SpanQuery) searcher.rewrite(query)).createWeight(searcher, scoreMode, boost);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.search.Query#toString(java.lang.String)
	 */
	@Override
	public String toString(String field) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(this.getClass().getSimpleName() + "([" + this.query.getField() + ":" + prefix + operator + svalue + "])");
		return buffer.toString();
	}

	/**
	 * To automaton.
	 *
	 * @param operator the operator
	 * @param prefix   the prefix
	 * @param value    the value
	 * @return the automaton
	 */
	private static Automaton toAutomaton(String operator, String prefix, int value) {
		List<Automaton> automata = new ArrayList<>();
		automata.add(Automata.makeBinary(new BytesRef(prefix + MtasToken.DELIMITER)));
		if (operator.equals(MTAS_OPERATOR_EQUAL)) {
			automata.add(Automata.makeString(Integer.toString(value)));
		} else if (operator.equals(MTAS_OPERATOR_LESS_THAN)) {
			automata.add(Automata.makeDecimalInterval(0, value - 1, 0));
		} else if (operator.equals(MTAS_OPERATOR_LESS_THAN_OR_EQUAL)) {
			automata.add(Automata.makeDecimalInterval(0, value, 0));
		} else if (operator.equals(MTAS_OPERATOR_MORE_THAN)) {
			int min = value + 1;
			int max = ((int) Math.pow(10, Integer.toString(value + 1).length())) - 1;
			automata.add(Automata.makeDecimalInterval(min, max, 0));
			automata.add(Operations.repeat(Automata.makeDecimalInterval(0, 9, 0)));
		} else if (operator.equals(MTAS_OPERATOR_MORE_THAN_OR_EQUAL)) {
			int min = value;
			int max = ((int) Math.pow(10, Integer.toString(value).length())) - 1;
			automata.add(Automata.makeDecimalInterval(min, max, 0));
			automata.add(Operations.repeat(Automata.makeDecimalInterval(0, 9, 0)));
		}
		automata.add(Operations.repeat(Automata.makeBinary(new BytesRef("\u0000"))));
		return Operations.concatenate(automata);
	}

	/**
	 * To automaton.
	 *
	 * @param operator the operator
	 * @param prefix the prefix
	 * @param value the value
	 * @return the automaton
	 */
	private static Automaton toAutomaton(String operator, String prefix, String value) {
		List<Automaton> automata = new ArrayList<>();
		automata.add(Automata.makeBinary(new BytesRef(prefix + MtasToken.DELIMITER)));
		if (operator.equals(MTAS_OPERATOR_DEQUAL)) {
			automata.add(Automata.makeString(value));
		}
		automata.add(Operations.repeat(Automata.makeBinary(new BytesRef("\u0000"))));
		return Operations.concatenate(automata);
	}

    @Override
    public void visit(QueryVisitor aVisitor)
    {
        query.visit(aVisitor);
    }
}
