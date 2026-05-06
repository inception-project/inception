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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.actionbar.undo.actions;

import java.io.Serializable;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.annotation.events.AnnotationEvent;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

public abstract class AnnotationAction_ImplBase
    implements Serializable
{
    private static final long serialVersionUID = -7723798951981402947L;

    private final long requestId;
    private final SourceDocument document;
    private final String user;
    private final VID vid;
    private final AnnotationLayer layer;

    public AnnotationAction_ImplBase(long aRequestId, AnnotationEvent aEvent, VID aVid)
    {
        requestId = aRequestId;
        vid = aVid;
        document = aEvent.getDocument();
        user = aEvent.getDocumentOwner();
        layer = aEvent.getLayer();
    }

    public AnnotationLayer getLayer()
    {
        return layer;
    }

    public VID getVid()
    {
        return vid;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public String getUser()
    {
        return user;
    }

    public long getRequestId()
    {
        return requestId;
    }
}
