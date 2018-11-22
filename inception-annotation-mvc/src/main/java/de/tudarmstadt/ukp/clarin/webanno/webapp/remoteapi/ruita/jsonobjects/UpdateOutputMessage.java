package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

public class UpdateOutputMessage<T>
    extends Message
{
    T updatedValue;

    public UpdateOutputMessage(String text, T updatedValue)
    {
        super(text);
        this.updatedValue = updatedValue;
    }

    public T getUpdatedValue()
    {
        return updatedValue;
    }

    public void setUpdatedValue(T updatedValue)
    {
        this.updatedValue = updatedValue;
    }

}
