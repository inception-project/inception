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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.IOException;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;
import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

/**
 * Displays a BRAT visualization and fills it with data from an BRAT object model.
 */
public class BratModelVisualizer
    extends BratVisualizer
{
    private static final long serialVersionUID = -5898873898138122798L;

    private boolean dirty = true;

    private String docData = EMPTY_DOC;

    public BratModelVisualizer(String id, IModel<GetDocumentResponse> aModel)
    {
        super(id, aModel);
    }

    public void setModel(IModel<GetDocumentResponse> aModel)
    {
        setDefaultModel(aModel);
    }

    public void setModelObject(GetDocumentResponse aModel)
    {
        setDefaultModelObject(aModel);
    }

    @SuppressWarnings("unchecked")
    public IModel<GetDocumentResponse> getModel()
    {
        return (IModel<GetDocumentResponse>) getDefaultModel();
    }

    public GetDocumentResponse getModelObject()
    {
        return (GetDocumentResponse) getDefaultModelObject();
    }

    @Override
    protected void onModelChanged()
    {
        super.onModelChanged();

        dirty = true;
    }

    @Override
    public String getDocumentData()
    {
        if (!dirty) {
            return docData;
        }

        // Get BRAT object model
        GetDocumentResponse response = getModelObject();

        // Serialize BRAT object model to JSON
        try {
            docData = JSONUtil.toInterpretableJsonString(response);
        }
        catch (IOException e) {
            error(ExceptionUtils.getRootCauseMessage(e));
        }

        return docData;
    }
}
