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
package de.tudarmstadt.ukp.clarin.webanno.brat.message;

import java.util.ArrayList;
import java.util.List;

/**
 * Response for the {@code deleteArc} command.
 */
public class DeleteArcResponse
    extends AjaxResponse
{
    public static final String COMMAND = "deleteArc";

    private GetDocumentResponse annotations;

    public DeleteArcResponse()
    {
        super(COMMAND);
    }

    /**
     * [[ "T1"],["T2"]]
     */
    private List<String[]> edited = new ArrayList<>();

    public GetDocumentResponse getAnnotations()
    {
        return annotations;
    }

    public void setAnnotations(GetDocumentResponse aAnnotations)
    {
        annotations = aAnnotations;
    }

    public List<String[]> getEdited()
    {
        return edited;
    }

    public void setEdited(List<String[]> aEdited)
    {
        edited = aEdited;
    }

    public static boolean is(String aCommand)
    {
        return COMMAND.equals(aCommand);
    }
}
