package mtas.parser.cql.util;

import java.util.ArrayList;
import java.util.List;

import mtas.parser.cql.ParseException;
import mtas.search.spans.MtasSpanRecurrenceQuery;
import mtas.search.spans.MtasSpanSequenceItem;
import mtas.search.spans.MtasSpanSequenceQuery;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasCQLParserBasicSentenceCondition.
 */
public class MtasCQLParserBasicSentenceCondition
{

    /** The part list. */
    private List<MtasCQLParserBasicSentencePartCondition> partList;

    /** The minimum occurence. */
    private int minimumOccurence;

    /** The maximum occurence. */
    private int maximumOccurence;

    /** The simplified. */
    private boolean simplified;

    /** The optional. */
    private boolean optional;

    /** The ignore clause. */
    private MtasSpanQuery ignoreClause;

    /** The maximum ignore length. */
    private Integer maximumIgnoreLength;

    /**
     * Instantiates a new mtas CQL parser basic sentence condition.
     *
     * @param ignore
     *            the ignore
     * @param maximumIgnoreLength
     *            the maximum ignore length
     */
    public MtasCQLParserBasicSentenceCondition(MtasSpanQuery ignore, Integer maximumIgnoreLength)
    {
        partList = new ArrayList<MtasCQLParserBasicSentencePartCondition>();
        minimumOccurence = 1;
        maximumOccurence = 1;
        optional = false;
        simplified = false;
        this.ignoreClause = ignore;
        this.maximumIgnoreLength = maximumIgnoreLength;
    }

    /**
     * Adds the word.
     *
     * @param w
     *            the w
     * @throws ParseException
     *             the parse exception
     */
    public void addWord(MtasCQLParserWordFullCondition w) throws ParseException
    {
        assert w.getCondition()
                .not() == false : "condition word should be positive in sentence definition";
        if (!simplified) {
            partList.add(w);
        }
        else {
            throw new ParseException("already simplified");
        }
    }

    /**
     * Adds the group.
     *
     * @param g
     *            the g
     * @throws ParseException
     *             the parse exception
     */
    public void addGroup(MtasCQLParserGroupFullCondition g) throws ParseException
    {
        if (!simplified) {
            partList.add(g);
        }
        else {
            throw new ParseException("already simplified");
        }
    }

    /**
     * Adds the basic sentence.
     *
     * @param s
     *            the s
     * @throws ParseException
     *             the parse exception
     */
    public void addBasicSentence(MtasCQLParserBasicSentenceCondition s) throws ParseException
    {
        if (!simplified) {
            List<MtasCQLParserBasicSentencePartCondition> newWordList = s.getPartList();
            partList.addAll(newWordList);
        }
        else {
            throw new ParseException("already simplified");
        }
    }

    /**
     * Gets the minimum occurence.
     *
     * @return the minimum occurence
     */
    public int getMinimumOccurence()
    {
        return minimumOccurence;
    }

    /**
     * Gets the maximum occurence.
     *
     * @return the maximum occurence
     */
    public int getMaximumOccurence()
    {
        return maximumOccurence;
    }

    /**
     * Sets the occurence.
     *
     * @param min
     *            the min
     * @param max
     *            the max
     * @throws ParseException
     *             the parse exception
     */
    public void setOccurence(int min, int max) throws ParseException
    {
        if (!simplified) {
            if ((min < 0) || (min > max) || (max < 1)) {
                throw new ParseException("Illegal number {" + min + "," + max + "}");
            }
            if (min == 0) {
                optional = true;
            }
            minimumOccurence = Math.max(1, min);
            maximumOccurence = max;
        }
        else {
            throw new ParseException("already simplified");
        }
    }

    /**
     * Checks if is optional.
     *
     * @return true, if is optional
     */
    public boolean isOptional()
    {
        return optional;
    }

    /**
     * Sets the optional.
     *
     * @param status
     *            the new optional
     * @throws ParseException
     *             the parse exception
     */
    public void setOptional(boolean status) throws ParseException
    {
        optional = status;
    }

    /**
     * Simplify.
     *
     * @throws ParseException
     *             the parse exception
     */
    public void simplify() throws ParseException
    {
        if (!simplified) {
            simplified = true;
            boolean optionalParts = true;
            List<MtasCQLParserBasicSentencePartCondition> newPartList;
            MtasCQLParserBasicSentencePartCondition lastPart = null;
            newPartList = new ArrayList<MtasCQLParserBasicSentencePartCondition>();
            // try and merge equal basicSentencePart (word/group) conditions
            for (MtasCQLParserBasicSentencePartCondition part : partList) {
                if ((lastPart == null) || !lastPart.equals(part)) {
                    lastPart = part;
                    newPartList.add(part);
                    if (!part.isOptional()) {
                        optionalParts = false;
                    }
                }
                else {
                    int newMinimumOccurence;
                    int newMaximumOccurence;
                    if (!lastPart.isOptional() && !part.isOptional()) {
                        newMinimumOccurence = lastPart.getMinimumOccurence()
                                + part.getMinimumOccurence();
                        newMaximumOccurence = lastPart.getMaximumOccurence()
                                + part.getMaximumOccurence();
                        lastPart.setOccurence(newMinimumOccurence, newMaximumOccurence);
                    }
                    else if (!lastPart.isOptional() && part.isOptional()) {
                        if (part.getMinimumOccurence() == 1) {
                            newMinimumOccurence = lastPart.getMinimumOccurence()
                                    + part.getMinimumOccurence() - 1;
                            newMaximumOccurence = lastPart.getMaximumOccurence()
                                    + part.getMaximumOccurence();
                            lastPart.setOccurence(newMinimumOccurence, newMaximumOccurence);
                        }
                        else {
                            lastPart = part;
                            newPartList.add(part);
                            if (!part.isOptional()) {
                                optionalParts = false;
                            }
                        }
                    }
                    else if (lastPart.isOptional() && !part.isOptional()) {
                        if (lastPart.getMinimumOccurence() == 1) {
                            newMinimumOccurence = lastPart.getMinimumOccurence()
                                    + part.getMinimumOccurence() - 1;
                            newMaximumOccurence = lastPart.getMaximumOccurence()
                                    + part.getMaximumOccurence();
                            lastPart.setOccurence(newMinimumOccurence, newMaximumOccurence);
                            lastPart.setOptional(false);
                            optionalParts = false;
                        }
                        else {
                            lastPart = part;
                            newPartList.add(part);
                            optionalParts = false;
                        }
                    }
                    else {
                        if ((lastPart.getMinimumOccurence() == 1)
                                && (part.getMinimumOccurence() == 1)) {
                            newMinimumOccurence = 1;
                            newMaximumOccurence = lastPart.getMaximumOccurence()
                                    + part.getMaximumOccurence();
                            lastPart.setOccurence(newMinimumOccurence, newMaximumOccurence);
                            lastPart.setOptional(true);
                        }
                        else {
                            lastPart = part;
                            newPartList.add(part);
                        }
                    }
                }
            }
            partList = newPartList;
            if (optionalParts) {
                optional = true;
            }
        }
    }

    /**
     * Gets the part list.
     *
     * @return the part list
     */
    private List<MtasCQLParserBasicSentencePartCondition> getPartList()
    {
        return partList;
    }

    /**
     * Gets the query.
     *
     * @return the query
     * @throws ParseException
     *             the parse exception
     */
    public MtasSpanQuery getQuery() throws ParseException
    {
        simplify();
        MtasSpanSequenceItem currentQuery = null;
        List<MtasSpanSequenceItem> currentQueryList = null;
        for (MtasCQLParserBasicSentencePartCondition part : partList) {
            // start list
            if (currentQuery != null) {
                currentQueryList = new ArrayList<MtasSpanSequenceItem>();
                currentQueryList.add(currentQuery);
                currentQuery = null;
            }
            if (part.getMaximumOccurence() > 1) {
                MtasSpanQuery q = new MtasSpanRecurrenceQuery(part.getQuery(),
                        part.getMinimumOccurence(), part.getMaximumOccurence(), ignoreClause,
                        maximumIgnoreLength);
                currentQuery = new MtasSpanSequenceItem(q, part.isOptional());
            }
            else {
                currentQuery = new MtasSpanSequenceItem(part.getQuery(), part.isOptional());
            }
            // add to list, if it exists
            if (currentQueryList != null) {
                currentQueryList.add(currentQuery);
                currentQuery = null;
            }
        }
        if (currentQueryList != null) {
            return new MtasSpanSequenceQuery(currentQueryList, ignoreClause, maximumIgnoreLength);
        }
        else if (currentQuery.isOptional()) {
            currentQueryList = new ArrayList<MtasSpanSequenceItem>();
            currentQueryList.add(currentQuery);
            currentQuery = null;
            return new MtasSpanSequenceQuery(currentQueryList, ignoreClause, maximumIgnoreLength);
        }
        else {
            return currentQuery.getQuery();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder text = new StringBuilder("BASIC SENTENCE");
        if (optional) {
            text.append(" OPTIONAL");
        }
        text.append("\n");
        if (simplified) {
            try {
                text.append("- Query: " + getQuery().toString(getQuery().getField()));
            }
            catch (ParseException e) {
                text.append("- Query: " + e.getMessage());
            }
        }
        else {
            for (MtasCQLParserBasicSentencePartCondition word : partList) {
                text.append(word.toString("  - ", "   "));
            }
        }
        return text.toString();
    }

}
