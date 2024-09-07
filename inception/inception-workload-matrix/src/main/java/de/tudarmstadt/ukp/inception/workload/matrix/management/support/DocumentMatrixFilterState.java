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
package de.tudarmstadt.ukp.inception.workload.matrix.management.support;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocumentState;

public class DocumentMatrixFilterState
    implements Serializable
{
    private static final long serialVersionUID = -8778279692135238878L;

    private String documentName;
    private boolean matchDocumentNameAsRegex;
    private String userName;
    private boolean matchUserNameAsRegex;
    private final List<SourceDocumentState> states = new ArrayList<>();

    public String getDocumentName()
    {
        return documentName;
    }

    public void setDocumentName(String aDocumentName)
    {
        documentName = aDocumentName;
    }

    public boolean isMatchDocumentNameAsRegex()
    {
        return matchDocumentNameAsRegex;
    }

    public void setMatchDocumentNameAsRegex(boolean aMatchDocumentNameAsRegex)
    {
        matchDocumentNameAsRegex = aMatchDocumentNameAsRegex;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String aUserName)
    {
        userName = aUserName;
    }

    public boolean isMatchUserNameAsRegex()
    {
        return matchUserNameAsRegex;
    }

    public void setMatchUserNameAsRegex(boolean aMatchUserNameAsRegex)
    {
        matchUserNameAsRegex = aMatchUserNameAsRegex;
    }

    public void reset()
    {
        documentName = null;
        userName = null;
    }

    public List<SourceDocumentState> getStates()
    {
        return states;
    }

    public void setState(List<SourceDocumentState> states)
    {
        states.clear();
        if (states != null) {
            states.addAll(states);
        }
    }
}
