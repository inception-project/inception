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
package de.tudarmstadt.ukp.clarin.webanno.brat.curation.component.model;

import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import com.googlecode.wicket.jquery.ui.resource.JQueryUIResourceReference;

import de.tudarmstadt.ukp.clarin.webanno.brat.annotation.BratVisualizer;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAjaxResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotationLogResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotatorUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratConfigurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCurationUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratDispatcherResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratSpinnerResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUrlMonitorResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUtilResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryJsonResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgDomResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.WebfontResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.model.Mode;

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
		String username;
		if(getModelObject().getBratAnnotatorModel().getMode().equals(Mode.AUTOMATION)
		        ||getModelObject().getBratAnnotatorModel().getMode().equals(Mode.CORRECTION)){
		    username = "Suggestion";
		}
        else {
            username = getModelObject().getUsername();
        }
		Label label = new Label("username", username);
		add(label);
        controller = new AbstractDefaultAjaxBehavior() {
            private static final long serialVersionUID = 1133593826878553307L;

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
        // Libraries
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryUIResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgDomResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryJsonResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(WebfontResourceReference.get()));

        // BRAT helpers
        aResponse.render(JavaScriptHeaderItem.forReference(BratConfigurationResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUtilResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotationLogResourceReference.get()));
        
        // BRAT modules
        aResponse.render(JavaScriptHeaderItem.forReference(BratDispatcherResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUrlMonitorResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAjaxResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotatorUiResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratSpinnerResourceReference.get()));	    
        aResponse.render(JavaScriptHeaderItem.forReference(BratCurationUiResourceReference.get()));        
	    
        // BRAT call to load the BRAT JSON from our collProvider and docProvider.
        StringBuilder script = new StringBuilder();
        script.append("Util.embedByURL(");
        script.append("  '"+vis.getMarkupId()+"',");
        script.append("  '"+collProvider.getCallbackUrl()+"', ");
        script.append("  '"+docProvider.getCallbackUrl()+"', ");
        script.append("  function(dispatcher) {");
        script.append("    dispatcher.wicketId = '" + vis.getMarkupId() + "'; ");
        script.append("    dispatcher.ajaxUrl = '" + controller.getCallbackUrl() + "'; ");
        script.append("    var ajax = new Ajax(dispatcher);");
//        script.append("    var ajax_"+vis.getMarkupId()+" = ajax;");
        script.append("    var curation_mod = new CurationMod(dispatcher, '"+vis.getMarkupId()+"');");
        script.append("    dispatcher.post('clearSVG', []);");
        script.append("  });");
        aResponse.render(OnLoadHeaderItem.forScript("\n" + script.toString()));
	}
	
	@Override
	protected String getDocumentData() {
		return getModelObject().getDocumentResponse() == null?"{}":getModelObject().getDocumentResponse();
	}

	@Override
	protected String getCollectionData() {
		return getModelObject().getCollectionData();
	}

	protected void onSelectAnnotationForMerge(AjaxRequestTarget aTarget) {
		// Overriden in Curation Panel
	}

}
