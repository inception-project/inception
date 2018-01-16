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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

    protected abstract String getDocumentData();

    protected String getCollectionData()
    {
        return "{}";
    }
    
    private String bratRenderCommand(String aJson)
    {
        return "Wicket.$('" + vis.getMarkupId() + "').dispatcher.post('renderData', [" + aJson
                + "]);";
    }

    protected void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript(
                "setTimeout(function() { " + bratRenderCommand(getDocumentData()) + " }, 0);");
    }
    
    /**
     * Schedules a rendering call via at the end of the given AJAX cycle. This method can be
     * called multiple times, even for the same annotation editor, but only resulting in a single
     * rendering call.
     */
    public final void requestRender(AjaxRequestTarget aTarget)
    {
        aTarget.registerRespondListener(new RenderListener());
    }
    
    /**
     * This is a special AJAX target response listener which implements hashCode and equals.
     * It useds the markup ID of its host component to identify itself. This enables us to add
     * multiple instances of this listener to an AJAX response without *actually* adding
     * multiple instances since the AJAX response internally keeps track of the listeners
     * using a set.
     */
    private class RenderListener
        implements AjaxRequestTarget.ITargetRespondListener
    {
        private String markupId;

        public RenderListener()
        {
            markupId = BratVisualizer.this.getMarkupId();
        }

        @Override
        public void onTargetRespond(AjaxRequestTarget aTarget)
        {
            if (isNotBlank(getDocumentData())) {
                render(aTarget);
            }
        }

        private BratVisualizer getOuterType()
        {
            return BratVisualizer.this;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((markupId == null) ? 0 : markupId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            RenderListener other = (RenderListener) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (markupId == null) {
                if (other.markupId != null) {
                    return false;
                }
            }
            else if (!markupId.equals(other.markupId)) {
                return false;
            }
            return true;
        }
    }
}
