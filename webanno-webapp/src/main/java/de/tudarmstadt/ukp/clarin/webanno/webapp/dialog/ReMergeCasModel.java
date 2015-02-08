/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
