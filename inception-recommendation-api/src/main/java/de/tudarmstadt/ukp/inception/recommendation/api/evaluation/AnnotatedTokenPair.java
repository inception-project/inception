package de.tudarmstadt.ukp.inception.recommendation.api.evaluation;

public class AnnotatedTokenPair
{
    private int begin;
    private int end;
    private String goldLabel;
    private String predictedLabel;

    public AnnotatedTokenPair(int aBegin, int aEnd, String aGoldLabel, String aPredictedLabel)
    {
        super();
        begin = aBegin;
        end = aEnd;
        goldLabel = aGoldLabel;
        predictedLabel = aPredictedLabel;
    }

    public int getBegin()
    {
        return begin;
    }

    public int getEnd()
    {
        return end;
    }

    public String getGoldLabel()
    {
        return goldLabel;
    }

    public String getPredictedLabel()
    {
        return predictedLabel;
    }

}
