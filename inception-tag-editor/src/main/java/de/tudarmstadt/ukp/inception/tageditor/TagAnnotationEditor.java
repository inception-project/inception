/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.tageditor;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.uima.jcas.JCas;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.JCasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorBase;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.AnnotatorState;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.tageditor.brat.BratAnnotationDocument;
import de.tudarmstadt.ukp.inception.tageditor.brat.DKPro2Brat;
import de.tudarmstadt.ukp.inception.tageditor.tag.css.TagCssReference;
import de.tudarmstadt.ukp.inception.tageditor.tag.js.TagReference;

public class TagAnnotationEditor
    extends AnnotationEditorBase
{
    private static final long serialVersionUID = -3358207848681467993L;
    
    private static final Logger LOG = LoggerFactory.getLogger(TagAnnotationEditor.class);

    private @SpringBean DocumentService documentService;
    private @SpringBean AnnotationSchemaService annotationService;
    private @SpringBean AnnotationEditorExtensionRegistry extensionRegistry;

    private WebMarkupContainer vis;
    
    public TagAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, JCasProvider aJCasProvider)
    {
        super(aId, aModel, aActionHandler, aJCasProvider);

        vis = new WebMarkupContainer("vis");
        vis.setOutputMarkupId(true);
        add(vis);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);
        
        aResponse.render(CssHeaderItem.forReference(TagCssReference.get()));
        aResponse.render(JavaScriptHeaderItem.forReference(TagReference.get()));
        aResponse.render(OnDomReadyHeaderItem.forScript(initScript()));
        
        // If the page is reloaded in the browser and a document was already open, we need
        // to render it. We use the "later" commands here to avoid polluting the Javascript
        // header items with document data and because loading times are not that critical
        // on a reload.
        // We only do this if we are *not* in a partial page reload. The case of a partial
        // page reload is covered in onAfterRender()
        Optional<AjaxRequestTarget> target = RequestCycle.get().find(AjaxRequestTarget.class);
        if (!target.isPresent() && getModelObject().getProject() != null) {
            aResponse.render(OnLoadHeaderItem.forScript(renderAsyncScript()));
        }
    }

    public String initScript()
    {
        return String.join("\n", 
                "const e = Wicket.$('" + vis.getMarkupId() + "')",
                "const tag = TAG.tag({",
                "  container: e,",
                "  options: {}",
                "});",
                "e.tagEditor = tag;");
    }

    public String renderAsyncScript()
    {
        // This is not yet async - actually, this should not send the data long but rather
        // tell TAG to load the data from an AjaxBehavior which still needs to be implemented
        return "Wicket.$('" + vis.getMarkupId() + "').tagEditor.loadData('" + renderData()
                + "', 'brat')";
    }

    @Override
    public void render(AjaxRequestTarget aTarget)
    {
        aTarget.appendJavaScript("Wicket.$('" + vis.getMarkupId() + "').tagEditor.loadData('"
                + renderData() + "', 'brat')");
    }
    
    private String renderData()
    {
        try {
            DKPro2Brat converter = new DKPro2Brat();
            Set<String> excludes = new HashSet<>();
            excludes.add(Sentence.class.getName());
            excludes.add(Token.class.getName());
            converter.setExcludeTypes(excludes);
            
            BratAnnotationDocument doc = new BratAnnotationDocument();
            JCas jcas = getJCasProvider().get();
            converter.convert(jcas, doc);
            
            StringWriter buf = new StringWriter();
            buf.write(jcas.getDocumentText().replace('\n', ' '));
            buf.write('\n');
            doc.write(buf);
            return StringEscapeUtils.escapeEcmaScript(buf.toString());
        }
        catch (IOException e) {
            LOG.error("Unable to load data", e);
            return "";
        }
    }
}
