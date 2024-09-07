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
package de.tudarmstadt.ukp.inception.pdfeditor2.view.pdfjs;

import static de.tudarmstadt.ukp.inception.pdfeditor2.config.PdfAnnotationEditor2WebMvcConfiguration.BASE_URL;
import static org.apache.wicket.core.util.string.CssUtils.ATTR_LINK_HREF;
import static org.apache.wicket.core.util.string.CssUtils.ATTR_LINK_REL;
import static org.apache.wicket.core.util.string.CssUtils.ATTR_TYPE;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.IMarkupCacheKeyProvider;
import org.apache.wicket.markup.IMarkupResourceStreamProvider;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.StringHeaderItem;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.UrlRenderer;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.value.AttributeMap;
import org.wicketstuff.annotation.mount.MountPath;

import de.tudarmstadt.ukp.inception.pdfeditor2.config.PdfAnnotationEditor2WebMvcConfiguration;
import de.tudarmstadt.ukp.inception.pdfeditor2.resources.PdfJsViewerJavaScriptReference;
import de.tudarmstadt.ukp.inception.support.wicket.InputStreamResourceStream;
import jakarta.servlet.ServletContext;

@MountPath(PdfAnnotationEditor2WebMvcConfiguration.BASE_URL + "/viewer.html")
public class PdfJsViewerPage
    extends WebPage
    implements IMarkupResourceStreamProvider, IMarkupCacheKeyProvider
{
    private static final long serialVersionUID = -6785521330117759815L;

    private @SpringBean ServletContext servletContext;

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        renderLocaleReference(aResponse);

        aResponse.render(JavaScriptHeaderItem.forReference(PdfJsViewerJavaScriptReference.get()));
        var script = String.join("\n", //
                "window.addEventListener('DOMContentLoaded', function() {", //
                "  PDFViewerApplicationOptions.set('annotationMode', 0);", //
                "  PDFViewerApplicationOptions.set('defaultUrl', null);", //
                "  PDFViewerApplicationOptions.set('disablePreferences', true);", //
                "  PDFViewerApplicationOptions.set('workerSrc', 'pdf.worker.min.js');", //
                "  PDFViewerApplicationOptions.set('isEvalSupported', false);", //
                "  PDFViewerApplicationOptions.set('enableScripting', false);", //
                "  PDFViewerApplicationOptions.set('viewOnLoad', 1);", //
                // Because when when we jump to a location in a different document it sucks when
                // the sidebar automatically opens and causes a re-scaling which leads to a wrong
                // scroll position!
                "  PDFViewerApplicationOptions.set('sidebarViewOnLoad', 0);", //
                // Disable the PDF.js search and instead delegate to the browser search
                "  delete PDFViewerApplication.supportsIntegratedFind;", //
                "  PDFViewerApplication.supportsIntegratedFind = true;", //
                // Disable the PDF.js printing
                "  delete PDFViewerApplication.supportsPrinting;", //
                "  PDFViewerApplication.supportsPrinting = false;", //
                "});");
        aResponse.render(JavaScriptHeaderItem.forScript(script, "initialization"));
    }

    private void renderLocaleReference(IHeaderResponse aResponse)
    {
        UrlRenderer urlRenderer = RequestCycle.get().getUrlRenderer();
        String localeUrl = urlRenderer
                .renderContextRelativeUrl(BASE_URL + "/locale/locale.properties");

        AttributeMap attributes = new AttributeMap();
        attributes.putAttribute(ATTR_LINK_REL, "resource");
        attributes.putAttribute(ATTR_TYPE, "application/l10n");
        attributes.putAttribute(ATTR_LINK_HREF, localeUrl);
        aResponse.render(new StringHeaderItem("<link" + attributes.toCharSequence() + " />"));
    }

    @Override
    public String getCacheKey(MarkupContainer container, Class<?> containerClass)
    {
        return getClass().getName();
    }

    @Override
    public IResourceStream getMarkupResourceStream(MarkupContainer aContainer,
            Class<?> aContainerClass)
    {
        return new InputStreamResourceStream(getClass().getResourceAsStream(
                "/de/tudarmstadt/ukp/inception/pdfeditor2/resources/viewer.html"));
    }
}
