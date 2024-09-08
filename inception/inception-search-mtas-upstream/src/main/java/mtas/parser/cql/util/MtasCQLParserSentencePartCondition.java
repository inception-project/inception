package mtas.parser.cql.util;

import mtas.parser.cql.ParseException;
import mtas.search.spans.util.MtasSpanQuery;

/**
 * The Class MtasCQLParserSentencePartCondition.
 */
public class MtasCQLParserSentencePartCondition
{

    /** The first sentence. */
    private MtasCQLParserSentenceCondition firstSentence = null;

    /** The first basic sentence. */
    private MtasCQLParserBasicSentenceCondition firstBasicSentence = null;

    /** The first minimum occurence. */
    private int firstMinimumOccurence;

    /** The first maximum occurence. */
    private int firstMaximumOccurence;

    /** The first optional. */
    private boolean firstOptional;

    /** The second sentence part. */
    MtasCQLParserSentencePartCondition secondSentencePart = null;

    /** The or operator. */
    private boolean orOperator = false;

    /** The full condition. */
    private MtasCQLParserSentenceCondition fullCondition = null;

    /** The ignore clause. */
    private MtasSpanQuery ignoreClause;

    /** The maximum ignore length. */
    private Integer maximumIgnoreLength;

    /**
     * Instantiates a new mtas CQL parser sentence part condition.
     *
     * @param bs
     *            the bs
     * @param ignore
     *            the ignore
     * @param maximumIgnoreLength
     *            the maximum ignore length
     */
    public MtasCQLParserSentencePartCondition(MtasCQLParserBasicSentenceCondition bs,
            MtasSpanQuery ignore, Integer maximumIgnoreLength)
    {
        firstMinimumOccurence = 1;
        firstMaximumOccurence = 1;
        firstOptional = false;
        firstBasicSentence = bs;
        this.ignoreClause = ignore;
        this.maximumIgnoreLength = maximumIgnoreLength;
    }

    /**
     * Instantiates a new mtas CQL parser sentence part condition.
     *
     * @param s
     *            the s
     * @param ignore
     *            the ignore
     * @param maximumIgnoreLength
     *            the maximum ignore length
     */
    public MtasCQLParserSentencePartCondition(MtasCQLParserSentenceCondition s,
            MtasSpanQuery ignore, Integer maximumIgnoreLength)
    {
        firstMinimumOccurence = 1;
        firstMaximumOccurence = 1;
        firstOptional = false;
        firstSentence = s;
        this.ignoreClause = ignore;
        this.maximumIgnoreLength = maximumIgnoreLength;
    }

    /**
     * Sets the first occurence.
     *
     * @param min
     *            the min
     * @param max
     *            the max
     * @throws ParseException
     *             the parse exception
     */
    public void setFirstOccurence(int min, int max) throws ParseException
    {
        if (fullCondition == null) {
            if ((min < 0) || (min > max) || (max < 1)) {
                throw new ParseException("Illegal number {" + min + "," + max + "}");
            }
            if (min == 0) {
                firstOptional = true;
            }
            firstMinimumOccurence = Math.max(1, min);
            firstMaximumOccurence = max;
        }
        else {
            throw new ParseException("fullCondition already generated");
        }
    }

    /**
     * Sets the first optional.
     *
     * @param status
     *            the new first optional
     * @throws ParseException
     *             the parse exception
     */
    public void setFirstOptional(boolean status) throws ParseException
    {
        if (fullCondition == null) {
            firstOptional = status;
        }
        else {
            throw new ParseException("fullCondition already generated");
        }
    }

    /**
     * Sets the or.
     *
     * @param status
     *            the new or
     * @throws ParseException
     *             the parse exception
     */
    public void setOr(boolean status) throws ParseException
    {
        if (fullCondition == null) {
            orOperator = status;
        }
        else {
            throw new ParseException("fullCondition already generated");
        }
    }

    /**
     * Sets the second part.
     *
     * @param sp
     *            the new second part
     * @throws ParseException
     *             the parse exception
     */
    public void setSecondPart(MtasCQLParserSentencePartCondition sp) throws ParseException
    {
        if (fullCondition == null) {
            secondSentencePart = sp;
        }
        else {
            throw new ParseException("fullCondition already generated");
        }
    }

    /**
     * Creates the full sentence.
     *
     * @return the mtas CQL parser sentence condition
     * @throws ParseException
     *             the parse exception
     */
    public MtasCQLParserSentenceCondition createFullSentence() throws ParseException
    {
        if (fullCondition == null) {
            if (secondSentencePart == null) {
                if (firstBasicSentence != null) {
                    fullCondition = new MtasCQLParserSentenceCondition(firstBasicSentence,
                            ignoreClause, maximumIgnoreLength);

                }
                else {
                    fullCondition = firstSentence;
                }
                fullCondition.setOccurence(firstMinimumOccurence, firstMaximumOccurence);
                if (firstOptional) {
                    fullCondition.setOptional(firstOptional);
                }
                return fullCondition;
            }
            else {
                if (!orOperator) {
                    if (firstBasicSentence != null) {
                        firstBasicSentence.setOccurence(firstMinimumOccurence,
                                firstMaximumOccurence);
                        firstBasicSentence.setOptional(firstOptional);
                        fullCondition = new MtasCQLParserSentenceCondition(firstBasicSentence,
                                ignoreClause, maximumIgnoreLength);
                    }
                    else {
                        firstSentence.setOccurence(firstMinimumOccurence, firstMaximumOccurence);
                        firstSentence.setOptional(firstOptional);
                        fullCondition = new MtasCQLParserSentenceCondition(firstSentence,
                                ignoreClause, maximumIgnoreLength);
                    }
                    fullCondition.addSentenceToEndLatestSequence(
                            secondSentencePart.createFullSentence());
                }
                else {
                    MtasCQLParserSentenceCondition sentence = secondSentencePart
                            .createFullSentence();
                    if (firstBasicSentence != null) {
                        sentence.addSentenceAsFirstOption(new MtasCQLParserSentenceCondition(
                                firstBasicSentence, ignoreClause, maximumIgnoreLength));
                    }
                    else {
                        sentence.addSentenceAsFirstOption(firstSentence);
                    }
                    fullCondition = sentence;
                }
                return fullCondition;
            }
        }
        else {
            return fullCondition;
        }
    }

}
