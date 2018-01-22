package de.tudarmstadt.ukp.inception.conceptlinking.model;

public class Label
{
    private String label;
    private String[] altlabel;
    private double freq;
    private String type;
        
    public Label(String label, String altlabel, String type, String freq)
    {
        super();
        this.label = label.trim().toLowerCase();
        this.altlabel = altlabel.trim().toLowerCase().split(", ");
        this.type = type;
        this.freq = Double.parseDouble(freq.trim().replace(",",""));
    }
    
    public String getLabel()
    {
        return label;
    }
    
    public void setLabel(String label)
    {
        this.label = label;
    }
    
    public String[] getAltlabel()
    {
        return altlabel;
    }

    public void setAltlabel(String[] altlabel)
    {
        this.altlabel = altlabel;
    }

    public double getFreq()
    {
        return freq;
    }
    
    public void setFreq(int freq)
    {
        this.freq = freq;
    }
    
    public String getType()
    {
        return type;
    }
    
    public void setType(String type)
    {
        this.type = type;
    }
    
}
