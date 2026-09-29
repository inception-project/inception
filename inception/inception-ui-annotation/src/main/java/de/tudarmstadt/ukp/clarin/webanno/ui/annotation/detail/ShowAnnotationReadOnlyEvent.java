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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static java.util.Collections.emptyList;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;

import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.inception.rendering.vmodel.VID;

/**
 * Asks the {@link AnnotationDetailEditorPanel} to show an annotation read-only without changing the
 * active editor or its selection, e.g. an annotation from another user's CAS. Send it to the page;
 * the sender does not need to know the panel.
 */
public class ShowAnnotationReadOnlyEvent
{
    private final AjaxRequestTarget requestHandler;
    private final SourceDocument document;
    private final User dataOwner;
    private final VID vid;
    private final List<User> otherDataOwners;

    /**
     * @param aRequestHandler
     *            the AJAX target
     * @param aDocument
     *            the document the annotation belongs to
     * @param aDataOwner
     *            the user whose annotations contain the annotation
     * @param aVid
     *            the annotation, as resolved in the data owner's CAS
     */
    public ShowAnnotationReadOnlyEvent(AjaxRequestTarget aRequestHandler, SourceDocument aDocument,
            User aDataOwner, VID aVid)
    {
        this(aRequestHandler, aDocument, aDataOwner, aVid, emptyList());
    }

    /**
     * @param aRequestHandler
     *            the AJAX target
     * @param aDocument
     *            the document the annotation belongs to
     * @param aDataOwner
     *            the user whose annotations contain the annotation
     * @param aVid
     *            the annotation, as resolved in the data owner's CAS
     * @param aOtherDataOwners
     *            other users who have the same annotation, e.g. the annotators a curation
     *            suggestion stands for - shown alongside the data owner
     */
    public ShowAnnotationReadOnlyEvent(AjaxRequestTarget aRequestHandler, SourceDocument aDocument,
            User aDataOwner, VID aVid, List<User> aOtherDataOwners)
    {
        requestHandler = aRequestHandler;
        document = aDocument;
        dataOwner = aDataOwner;
        vid = aVid;
        otherDataOwners = aOtherDataOwners;
    }

    public AjaxRequestTarget getRequestHandler()
    {
        return requestHandler;
    }

    public SourceDocument getDocument()
    {
        return document;
    }

    public User getDataOwner()
    {
        return dataOwner;
    }

    public VID getVid()
    {
        return vid;
    }

    public List<User> getOtherDataOwners()
    {
        return otherDataOwners;
    }
}
