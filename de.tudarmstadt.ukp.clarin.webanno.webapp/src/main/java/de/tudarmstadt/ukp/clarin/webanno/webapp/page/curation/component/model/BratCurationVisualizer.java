/*******************************************************************************
 * Copyright 2012
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.webapp.page.curation.component.model;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetDocumentResponse;

/**
 * Wicket panel for visualizing an annotated sentence in brat. When a user
 * clicks on a span or an arc, the Method onSelectAnnotationForMerge() is
 * called. Override that method to receive the result in another wicket panel.
 * 
 * @author Andreas Straninger
 */
public class BratCurationVisualizer extends BratVisualizer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6653508018500736430L;
	private AbstractDefaultAjaxBehavior controller;

	public BratCurationVisualizer(String id, IModel<CurationUserSegmentForAnnotationDocument> aModel) {
		super(id, aModel);
		
		Label label = new Label("username", "Username: "+getModelObject().getUsername());
		add(label);
        controller = new AbstractDefaultAjaxBehavior() {

			@Override
			protected void respond(AjaxRequestTarget aTarget) {
                //aTarget.prependJavaScript("Wicket.$('"+vis.getMarkupId()+"').temp = ['test'];");
				onSelectAnnotationForMerge(aTarget);
			}
        	
        };
        add(controller);
	}

	public void setModel(IModel<CurationUserSegmentForAnnotationDocument> aModel)
	{
		setDefaultModel(aModel);
	}

	public void setModelObject(CurationUserSegmentForAnnotationDocument aModel)
	{
		setDefaultModelObject(aModel);
	}

	@SuppressWarnings("unchecked")
	public IModel<CurationUserSegmentForAnnotationDocument> getModel()
	{
		return (IModel<CurationUserSegmentForAnnotationDocument>) getDefaultModel();
	}

	public CurationUserSegmentForAnnotationDocument getModelObject()
	{
		return (CurationUserSegmentForAnnotationDocument) getDefaultModelObject();
	}

	@Override
	public void renderHead(IHeaderResponse aResponse)
	{
		// BRAT call to load the BRAT JSON from our collProvider and docProvider.
		String[] script = new String[] {
				"Util.embedByURL(",
				"  '"+vis.getMarkupId()+"',",
				"  '"+collProvider.getCallbackUrl()+"', ",
				"  '"+docProvider.getCallbackUrl()+"', ",
				"  function(dispatcher) {",
                "dispatcher.post('clearSVG', []);",
				"    dispatcher.ajaxUrl = '" + controller.getCallbackUrl() + "'; ",
                "    dispatcher.wicketId = '" + vis.getMarkupId() + "'; ",
                "    var ajax = new Ajax(dispatcher);",
                "    var ajax_"+vis.getMarkupId()+" = ajax;",
				"    var curation_mod = new CurationMod(dispatcher, '"+vis.getMarkupId()+"');",
				"  }",
				");",
		};

		// This doesn't work with head.js because the onLoad event is fired before all the
		// JavaScript references are loaded.
		aResponse.renderOnLoadJavaScript("\n"+StringUtils.join(script, "\n"));
	}
	@Override
	protected String getDocumentData() {
		return getModelObject().getDocumentResponse();
	}
	
	@Override
	protected String getCollectionData() {
		return getModelObject().getCollectionData();
	}
	
	protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget) {
		// Overriden in Curation Panel
	}

}
