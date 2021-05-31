package de.tudarmstadt.ukp.inception.experimental.api;

public class Annotation
{
    private int id;
    private String word;
    private int begin;
    private int end;
    private String type;

    public Annotation(int aId, String aWord, int aBegin, int aEnd, String aType)
    {
        id = aId;
        word = aWord;
        begin = aBegin;
        end = aEnd;
        type = aType;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int aId)
    {
        id = aId;
    }

    public String getWord()
    {
        return word;
    }

    public void setWord(String aWord)
    {
        word = aWord;
    }

    public int getBegin()
    {
        return begin;
    }

    public void setBegin(int aBegin)
    {
        begin = aBegin;
    }

    public int getEnd()
    {
        return end;
    }

    public void setEnd(int aEnd)
    {
        end = aEnd;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String aType)
    {
        type = aType;
    }
}
