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
package de.tudarmstadt.ukp.inception.htmleditor;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.apache.uima.cas.CAS;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.dkpro.core.api.xml.type.XmlDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.exception.AnnotationException;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;

public abstract class HtmlAnnotationEditorImplBase
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -4705174621824546501L;

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;
    private @SpringBean ColoringService coloringService;

    protected final Label vis;

    public HtmlAnnotationEditorImplBase(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        super(aId, aModel, aActionHandler, aCasProvider);

        vis = new Label("vis", LoadableDetachableModel.of(this::renderHtmlDocument));
        vis.setOutputMarkupId(true);
        vis.setEscapeModelStrings(false);
        // vis.add(new StyleAttributeModifier() {
        // private static final long serialVersionUID = -4326783281323226869L;
        //
        // @Override
        // protected Map<String, String> update(Map<String, String> aStyles)
        // {
        // switch (aModel.getObject().getScriptDirection()) {
        // case RTL:
        // aStyles.put("direction", "rtl");
        // break;
        // default:
        // aStyles.put("direction", "ltr");
        // break;
        // }
        // return aStyles;
        // }
        // });
        add(vis);
    }

    private String renderHtmlDocument()
    {
        CAS cas;
        try {
            cas = getCasProvider().get();
        }
        catch (IOException e) {
            LOG.error("Unable to load data", e);
            getSession().error("Unable to load data: " + getRootCauseMessage(e));
            return "";
        }

        try {
            if (cas.select(XmlDocument.class).isEmpty()) {
                return new LegacyHtmlDocumentRenderer().renderHtmlDocumentStructure(cas);
            }

            return new HtmlDocumentRenderer().render(cas);
        }
        catch (Exception e) {
            LOG.error("Unable to render data", e);
            getSession().error("Unable to render data: " + getRootCauseMessage(e));
            return "";
        }
    }

    protected void handleError(String aMessage, Exception e)
    {
        RequestCycle requestCycle = RequestCycle.get();
        requestCycle.find(AjaxRequestTarget.class)
                .ifPresent(target -> target.addChildren(getPage(), IFeedback.class));

        if (e instanceof AnnotationException) {
            // These are common exceptions happening as part of the user interaction. We do
            // not really need to log their stack trace to the log.
            error(aMessage + ": " + e.getMessage());
            // If debug is enabled, we'll also write the error to the log just in case.
            if (LOG.isDebugEnabled()) {
                LOG.error("{}: {}", aMessage, e.getMessage(), e);
            }
            return;
        }

        LOG.error("{}", aMessage, e);
        error(aMessage);
    }
}
