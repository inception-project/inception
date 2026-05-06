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
package de.tudarmstadt.ukp.inception.externaleditor;

import static de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorLoader.PLUGINS_EDITOR_BASE_URL;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.editor.view.DocumentViewExtensionPoint;
import de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorPluginDescripion;
import de.tudarmstadt.ukp.inception.externaleditor.model.AnnotationEditorProperties;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import jakarta.servlet.ServletContext;

public class ExternalAnnotationEditor
    extends ExternalAnnotationEditorBase
{
    private static final String PLUGIN_SCHEME = "plugin:";

    private static final long serialVersionUID = -3358207848681467993L;

    private @SpringBean DocumentViewExtensionPoint documentViewExtensionPoint;
    private @SpringBean DocumentService documentService;
    private @SpringBean ServletContext context;

    public ExternalAnnotationEditor(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            String aEditorFactoryId)
    {
        super(aId, aModel, aActionHandler, aCasProvider, aEditorFactoryId);
    }

    @Override
    protected Component makeView()
    {
        if (getDescription().getView().startsWith(PLUGIN_SCHEME)) {
            String resPath = substringAfter(getDescription().getView(), PLUGIN_SCHEME);
            return new ExternalAnnotationEditorStaticIFrameView(CID_VIS,
                    getUrlForPluginAsset(resPath));
        }

        var state = getModelObject();

        return documentViewExtensionPoint.getExtension(getDescription().getView()) //
                .map(ext -> ext.createView(CID_VIS, Model.of(state.getDocument()),
                        getFactory().getBeanName())) //
                .orElseGet(() -> new Label(CID_VIS,
                        "Unsupported view: [" + getDescription().getView() + "]"));
    }

    @Override
    protected ExternalAnnotationEditorFactory getFactory()
    {
        return (ExternalAnnotationEditorFactory) super.getFactory();
    }

    private ExternalEditorPluginDescripion getDescription()
    {
        return getFactory().getDescription();
    }

    private String getUrlForPluginAsset(String aAssetPath)
    {
        return getUrlForPluginAsset(context, getDescription(), aAssetPath);
    }

    public static String getUrlForPluginAsset(ServletContext aContext,
            ExternalEditorPluginDescripion aPlugin, String aAssetPath)
    {
        return aContext.getContextPath() + PLUGINS_EDITOR_BASE_URL + aPlugin.getId() + "/"
                + aAssetPath;
    }

    @Override
    protected AnnotationEditorProperties getProperties()
    {
        var pluginDesc = getDescription();

        var props = super.getProperties();
        props.setEditorFactory(pluginDesc.getFactory());
        props.setStylesheetSources(pluginDesc.getStylesheets().stream() //
                .map(this::getUrlForPluginAsset) //
                .collect(toList()));
        props.setScriptSources(pluginDesc.getScripts().stream() //
                .map(this::getUrlForPluginAsset) //
                .collect(toList()));
        props.setSectionElements(pluginDesc.getSectionElements());

        return props;
    }
}
