package de.tudarmstadt.ukp.inception.curation;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;

public class CurationVID
    extends VID
{
    private static final long serialVersionUID = -4052847275637346338L;
    
    private final String username;
    
    public CurationVID(String aExtId, String aUsername, VID aVID)
    {
        super(aExtId, aVID.getLayerId(), aVID.getId(), aVID.getSubId(), aVID.getAttribute(),
                aVID.getSlot());
        username = aUsername;
    }
    
    public String getUsername()
    {
        return username;
    }
    
    @Override
    public int hashCode()
    {
        return super.hashCode() * 31 + username.hashCode();
    }

    @Override
    public boolean equals(Object aObj)
    {
        return super.equals(aObj) && ((CurationVID) aObj).getUsername().equals(username);
    }
  
}
