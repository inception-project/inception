/**
 *
 */
package de.tudarmstadt.ukp.clarin.webanno.webapp.dialog;

import java.io.Serializable;

/**
 * A model to remerge CAS object for curation annotation document
 * @author Seid Muhie Yimam
 *
 */
public class ReMergeCasModel implements Serializable
{
    private static final long serialVersionUID = -755734573655020271L;
private boolean reMerege;

public boolean isReMerege()
{
    return reMerege;
}

public void setReMerege(boolean reMerege)
{
    this.reMerege = reMerege;
}

}
