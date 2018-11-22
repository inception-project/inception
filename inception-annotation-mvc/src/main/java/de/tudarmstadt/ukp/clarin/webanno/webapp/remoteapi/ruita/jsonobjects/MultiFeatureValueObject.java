package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.ruita.jsonobjects;

import java.util.ArrayList;
import java.util.HashMap;

public class MultiFeatureValueObject
    extends FeatureValueObject
{
    ArrayList<HashMap<String, String>> role_label_info;

    public ArrayList<HashMap<String, String>> getRole_label_info()
    {
        return role_label_info;
    }

    public void setRole_label_info(ArrayList<HashMap<String, String>> role_label_tupels)
    {
        this.role_label_info = role_label_tupels;
    }

}
