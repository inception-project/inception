package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class Message
    extends JSONOutput
{
    private String text;

    public Message(String text)
    {
        super();
        this.text = text;
    }

    public Message()
    {

    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

}
