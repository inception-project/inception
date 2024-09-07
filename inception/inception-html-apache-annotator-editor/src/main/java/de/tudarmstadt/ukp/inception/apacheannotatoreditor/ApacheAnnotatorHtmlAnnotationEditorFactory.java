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
package de.tudarmstadt.ukp.inception.apacheannotatoreditor;

import java.io.IOException;
import java.util.Optional;

import org.apache.wicket.model.IModel;

import com.networknt.schema.JsonSchema;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.apacheannotatoreditor.config.ApacheAnnotatorHtmlAnnotationEditorSupportAutoConfiguration;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactoryImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.io.html.HtmlArchiveFormatSupport;
import de.tudarmstadt.ukp.inception.io.html.HtmlFormatSupport;
import de.tudarmstadt.ukp.inception.io.html.MHtmlFormatSupport;
import de.tudarmstadt.ukp.inception.io.tei.TeiXmlDocumentFormatSupport;
import de.tudarmstadt.ukp.inception.io.xml.CustomXmlFormatLoader;
import de.tudarmstadt.ukp.inception.io.xml.XmlFormatSupport;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceMapValue;
import de.tudarmstadt.ukp.inception.preferences.ClientSideUserPreferencesProvider;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * Support for HTML-oriented editor component.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ApacheAnnotatorHtmlAnnotationEditorSupportAutoConfiguration#apacheAnnotatorHtmlAnnotationEditorFactory()}.
 * </p>
 */
public class ApacheAnnotatorHtmlAnnotationEditorFactory
    extends AnnotationEditorFactoryImplBase
    implements ClientSideUserPreferencesProvider
{
    private static final ClientSidePreferenceKey<ClientSidePreferenceMapValue> KEY_APACHE_ANNOTATOR_EDITOR_PREFS = //
            new ClientSidePreferenceKey<>(ClientSidePreferenceMapValue.class,
                    "annotation/apache-annotator-editor");

    private WatchedResourceFile<JsonSchema> userPreferencesSchema;

    public ApacheAnnotatorHtmlAnnotationEditorFactory()
    {
        var userPreferencesSchemaFile = getClass().getResource(
                "ApacheAnnotatorHtmlAnnotationEditorFactoryUserPreferences.schema.json");
        userPreferencesSchema = new WatchedResourceFile<>(userPreferencesSchemaFile,
                JSONUtil::loadJsonSchema);
    }

    @Override
    public String getDisplayName()
    {
        return Strings.getString("apacheannotator-editor.name");
    }

    @Override
    public int accepts(Project aProject, String aFormat)
    {
        if (aFormat.startsWith(CustomXmlFormatLoader.CUSTOM_XML_FORMAT_PREFIX)) {
            return PREFERRED;
        }

        return switch (aFormat) {
        case HtmlFormatSupport.ID, //
                HtmlArchiveFormatSupport.ID, //
                MHtmlFormatSupport.ID, //
                XmlFormatSupport.ID, //
                TeiXmlDocumentFormatSupport.ID:
            yield PREFERRED;
        default:
            yield DEFAULT;
        };
    }

    @Override
    public AnnotationEditorBase create(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        return new ApacheAnnotatorHtmlAnnotationEditor(aId, aModel, aActionHandler, aCasProvider,
                getBeanName());
    }

    @Override
    public void initState(AnnotatorState aModelObject)
    {
        aModelObject.setPagingStrategy(new NoPagingStrategy());
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public Optional<ClientSidePreferenceKey<ClientSidePreferenceMapValue>> getUserPreferencesKey()
    {
        return Optional.of(KEY_APACHE_ANNOTATOR_EDITOR_PREFS);
    }

    @Override
    public Optional<JsonSchema> getUserPreferencesSchema() throws IOException
    {
        return userPreferencesSchema.get();
    }
}
