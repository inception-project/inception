/*
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
 */
package de.tudarmstadt.ukp.clarin.webanno.brat.annotation;

import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.handler.TextRequestHandler;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.googlecode.wicket.jquery.ui.settings.JQueryUILibrarySettings;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAjaxResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratAnnotatorUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratConfigurationResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssUiReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratCssVisReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratDispatcherResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratUtilResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.BratVisualizerUiResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryJsonResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgDomResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQuerySvgResourceReference;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.WicketUtil;

/**
 * Base class for displaying a BRAT visualization. Override methods {@code #getCollectionData()} and
 * {@code #getDocumentData()} to provide the actual data.
 */
public abstract class BratVisualizer
    extends Panel
{
    private static final Logger LOG = LoggerFactory.getLogger(BratVisualizer.class);
    private static final long serialVersionUID = -1537506294440056609L;

    protected static final String EMPTY_DOC = "{text: ''}";

    protected WebMarkupContainer vis;

    protected AbstractAjaxBehavior collProvider;

    protected AbstractAjaxBehavior docProvider;

    private @SpringBean DocumentService repository;

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

        // CSS
        aResponse.render(CssHeaderItem.forReference(BratCssVisReference.get()));
        aResponse.render(CssHeaderItem.forReference(BratCssUiReference.get()));

        // Libraries
        aResponse.render(forReference(JQueryUILibrarySettings.get().getJavaScriptReference()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQuerySvgDomResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(JQueryJsonResourceReference.get()));

        // BRAT helpers
        aResponse.render(
                JavaScriptHeaderItem.forReference(BratConfigurationResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratUtilResourceReference.get()));

        // BRAT modules
        aResponse.render(JavaScriptHeaderItem.forReference(BratDispatcherResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAjaxResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratVisualizerResourceReference.get()));
        aResponse
                .render(JavaScriptHeaderItem.forReference(BratVisualizerUiResourceReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(BratAnnotatorUiResourceReference.get()));

        // BRAT call to load the BRAT JSON from our collProvider and docProvider.
        String[] script = { "Util.embedByURL(", "  '" + vis.getMarkupId() + "',",
                "  '" + collProvider.getCallbackUrl() + "', ",
                "  '" + docProvider.getCallbackUrl() + "', ", "  null);", };

        // This doesn't work with head.js because the onLoad event is fired before all the
        // JavaScript references are loaded.
        aResponse.render(OnLoadHeaderItem.forScript("\n" + StringUtils.join(script, "\n")));
    }

    public abstract String getDocumentData();

    protected String getCollectionData()
    {
        return "{}";
    }
    
    private String bratRenderCommand(String aJson)
    {
        String str = WicketUtil.wrapInTryCatch(
                "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('renderData', [" + 
                   aJson + "]);");
        return str;
    }

    public void render(AjaxRequestTarget aTarget)
    {
        LOG.debug("[{}][{}] render", getMarkupId(), vis.getMarkupId());
        
        // Controls whether rendering should happen within the AJAX request or after the AJAX
        // request. Doing it within the request has the benefit of the browser only having to
        // recalculate the layout once at the end of the AJAX request (at least theoretically)
        // while deferring the rendering causes the AJAX request to complete faster, but then
        // the browser needs to recalculate its layout twice - once of any Wicket components
        // being re-rendered and once for the brat view to re-render.
        final boolean deferredRendering = false;

        StringBuilder js = new StringBuilder();
        
        if (deferredRendering) {
            js.append("setTimeout(function() {");
        }
        
        js.append(bratRenderCommand(getDocumentData()));
        
        if (deferredRendering) {
            js.append("}, 0);");
        }
        
        aTarget.appendJavaScript(js);
    }
}
