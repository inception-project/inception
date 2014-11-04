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

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.api.RepositoryService;

/**
 * Base class for displaying a BRAT visualization. Override methods {@code #getCollectionData()}
 * and {@code #getDocumentData()} to provide the actual data.
 *
 * @author Richard Eckart de Castilho
 */
public abstract class BratVisualizer
	extends Panel
{
	private static final long serialVersionUID = -1537506294440056609L;

	protected static final String EMPTY_DOC = "{text: ''}";

	protected WebMarkupContainer vis;

	protected AbstractAjaxBehavior collProvider;

	protected AbstractAjaxBehavior docProvider;

    @SpringBean(name = "jsonConverter")
    private MappingJacksonHttpMessageConverter jsonConverter;

    @SpringBean(name = "documentRepository")
    private RepositoryService repository;

	public BratVisualizer(String id, IModel<?> aModel)
	{
		super(id, aModel);

		vis = new WebMarkupContainer("vis");
		vis.setOutputMarkupId(true);

		// Provides collection-level information like type definitions, styles, etc.
		collProvider = new AbstractAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onRequest()
			{
				getRequestCycle().scheduleRequestHandlerAfterCurrent(
						new TextRequestHandler("application/json", "UTF-8", getCollectionData()));
			}
		};

		// Provides the actual document contents
		docProvider = new AbstractAjaxBehavior()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void onRequest()
			{
				getRequestCycle().scheduleRequestHandlerAfterCurrent(
						new TextRequestHandler("application/json", "UTF-8", getDocumentData()));
			}
		};

		add(vis);
		add(collProvider, docProvider);
	}

	@Override
	public void renderHead(IHeaderResponse aResponse)
	{
		super.renderHead(aResponse);

		// BRAT call to load the BRAT JSON from our collProvider and docProvider.
		String[] script = new String[] {
				"Util.embedByURL(",
				"  '"+vis.getMarkupId()+"',",
				"  '"+collProvider.getCallbackUrl()+"', ",
				"  '"+docProvider.getCallbackUrl()+"', ",
				"  null);",
		};

		// This doesn't work with head.js because the onLoad event is fired before all the
		// JavaScript references are loaded.
		aResponse.renderOnLoadJavaScript("\n"+StringUtils.join(script, "\n"));
	}

	protected abstract String getDocumentData();

	protected String getCollectionData()
	{
		return "{}";
	}
}
