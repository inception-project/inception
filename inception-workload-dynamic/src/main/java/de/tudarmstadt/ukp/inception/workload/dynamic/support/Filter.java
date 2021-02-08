/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt 
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

//Helper class for the Filter
public class Filter
    implements Serializable
{

    private static final long serialVersionUID = 256259364194000084L;

    // Input fields appended value
    private String username;
    private String documentName;

    // Checkbox
    private boolean selected = false;

    // dates
    private Date from;
    private Date to;

    // State
    private final List<SourceDocumentState> aStates = new ArrayList<>();

    public Filter()
    {
        // Nothing to do
    }

    public String getUsername()
    {
        return username;
    }

    public String getDocumentName()
    {
        return documentName;
    }

    public boolean getSelected()
    {
        return selected;
    }

    public Date getFrom()
    {
        return from;
    }

    public Date getTo()
    {
        return to;
    }

    public void setUsername(String aUsername)
    {
        this.username = aUsername;
    }

    public void setDocumentName(String aDocumentName)
    {
        this.documentName = aDocumentName;
    }

    public void setSelected(boolean aSelected)
    {
        this.selected = aSelected;
    }

    public void setFrom(Date aFrom)
    {
        this.from = aFrom;
    }

    public void setTo(Date aTo)
    {
        this.to = aTo;
    }

    public List<SourceDocumentState> getStates()
    {
        return aStates;
    }

    public void setState(List<SourceDocumentState> states)
    {
        aStates.clear();
        if (states != null) {
            aStates.addAll(states);
        }
    }
}
