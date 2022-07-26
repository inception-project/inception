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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.component;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModel;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;

/**
 * Show closed/not closed images on annoataion/correction pages
 */
@Deprecated
public class FinishImage
    extends WebMarkupContainer
{

    private static final long serialVersionUID = -4931039843586219625L;

    private @SpringBean DocumentService documentService;
    private @SpringBean UserDao userRepository;

    public void setModel(IModel<AnnotatorState> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(AnnotatorState aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<AnnotatorState> getModel()
    {
        return (IModel<AnnotatorState>) getDefaultModel();
    }

    public AnnotatorState getModelObject()
    {
        return (AnnotatorState) getDefaultModelObject();
    }

    public FinishImage(String id, final IModel<AnnotatorState> aModel)
    {
        super(id, aModel);

        add(new AttributeModifier("src", LambdaModel.of(() -> {
            if (aModel.getObject().getProject() != null
                    && aModel.getObject().getDocument() != null) {
                if (isFinished(aModel, aModel.getObject().getUser(), documentService)) {
                    return "images/accept.png";
                }
                else {
                    return "images/inprogress.png";
                }
            }
            else {
                return "images/inprogress.png";
            }
        })));
    }

    public static boolean isFinished(final IModel<AnnotatorState> aModel, User user,
            DocumentService aRepository)
    {
        return aRepository.existsAnnotationDocument(aModel.getObject().getDocument(), user)
                && aRepository.getAnnotationDocument(aModel.getObject().getDocument(), user)
                        .getState().equals(AnnotationDocumentState.FINISHED);
    }
}
