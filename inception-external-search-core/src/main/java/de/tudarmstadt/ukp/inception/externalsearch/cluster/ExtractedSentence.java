package de.tudarmstadt.ukp.inception.externalsearch.cluster;

public class ExtractedSentence {
    private String sentenceText;
    private double score;
    private String sourceDoc;
    
    public ExtractedSentence(String aSentenceText, double aScore, String aSourceDoc)
    {
        sentenceText = aSentenceText;
        score = aScore;
        sourceDoc = aSourceDoc;
    }
    
    public String getSentenceText() {
        return sentenceText;
    }
    
    public void setSentenceText(String sentenceText) {
        this.sentenceText = sentenceText;
    }
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public String getSourceDoc() {
        return sourceDoc;
    }
    
    public void setSourceDoc(String sourceDoc) {
        this.sourceDoc = sourceDoc;
    }
}
