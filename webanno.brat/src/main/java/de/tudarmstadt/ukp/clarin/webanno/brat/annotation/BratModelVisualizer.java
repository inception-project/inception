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
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.codehaus.jackson.JsonGenerator;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;

/**
 * Displays a BRAT visualization and fills it with data from an BRAT object model.
 *
 * @author Richard Eckart de Castilho
 */
public class BratModelVisualizer
	extends BratVisualizer
{
	private static final long serialVersionUID = -5898873898138122798L;

	private boolean dirty = true;

	private String docData = EMPTY_DOC;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

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
	protected String getDocumentData()
	{
		if (!dirty) {
			return docData;
		}

		// Get BRAT object model
        GetDocumentResponse response = getModelObject();

        // Serialize BRAT object model to JSON
		try {
			StringWriter out = new StringWriter();
			JsonGenerator jsonGenerator = jsonConverter.getObjectMapper().getJsonFactory()
					.createJsonGenerator(out);
	        jsonGenerator.writeObject(response);
			docData = out.toString();
		}
		catch (IOException e) {
			error(ExceptionUtils.getRootCauseMessage(e));
		}

		return docData;
	}
}
