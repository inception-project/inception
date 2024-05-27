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
package de.tudarmstadt.ukp.inception.diam.sidebar;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.apache.wicket.model.IModel;
import org.springframework.core.annotation.Order;

import com.networknt.schema.JsonSchema;

import de.agilecoders.wicket.core.markup.html.bootstrap.image.Icon;
import de.agilecoders.wicket.extensions.markup.html.bootstrap.icon.FontAwesome5IconType;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.AnnotationPage;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarFactory_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebar_ImplBase;
import de.tudarmstadt.ukp.inception.diam.sidebar.config.AnnotationBrowserSidebarAutoConfiguration;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferencesKey;
import de.tudarmstadt.ukp.inception.preferences.ClientSideUserPreferencesProvider;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link AnnotationBrowserSidebarAutoConfiguration#annotationBrowserSidebarFactory}.
 * </p>
 */
@Order(1000)
public class DiamSidebarFactory
    extends AnnotationSidebarFactory_ImplBase
    implements ClientSideUserPreferencesProvider
{
    private WatchedResourceFile<JsonSchema> userPreferencesSchema;

    public DiamSidebarFactory()
    {
        var userPreferencesSchemaFile = getClass()
                .getResource("DiamSidebarFactoryUserPreferences.schema.json");
        userPreferencesSchema = new WatchedResourceFile<>(userPreferencesSchemaFile,
                JSONUtil::loadJsonSchema);
    }

    @Override
    public String getDisplayName()
    {
        return "Annotations";
    }

    @Override
    public String getDescription()
    {
        return "List of annnotations.";
    }

    @Override
    public org.apache.wicket.Component createIcon(String aId, IModel<AnnotatorState> aState)
    {
        return new Icon(aId, FontAwesome5IconType.list_s);
    }

    @Override
    public AnnotationSidebar_ImplBase create(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider,
            AnnotationPage aAnnotationPage)
    {
        return new DiamSidebar(aId, aModel, aActionHandler, aCasProvider, aAnnotationPage,
                getUserPreferencesKey().get().getClientSideKey());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Optional<ClientSidePreferencesKey<Map>> getUserPreferencesKey()
    {
        return Optional.of(
                new ClientSidePreferencesKey<>(Map.class, "annotation/annotation-browser-sidebar"));
    }

    @Override
    public Optional<JsonSchema> getUserPreferencesSchema() throws IOException
    {
        return userPreferencesSchema.get();
    }
}
