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

import java.io.IOException;
import java.util.Optional;

import org.apache.wicket.model.IModel;

import com.networknt.schema.JsonSchema;

import de.tudarmstadt.ukp.clarin.webanno.api.annotation.paging.NoPagingStrategy;
import de.tudarmstadt.ukp.clarin.webanno.api.casstorage.CasProvider;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorBase;
import de.tudarmstadt.ukp.inception.editor.AnnotationEditorFactoryImplBase;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.externaleditor.config.ExternalEditorPluginDescripion;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceKey;
import de.tudarmstadt.ukp.inception.preferences.ClientSidePreferenceMapValue;
import de.tudarmstadt.ukp.inception.preferences.ClientSideUserPreferencesProvider;
import de.tudarmstadt.ukp.inception.preferences.PreferenceValue;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionIOUtils;

public class ExternalAnnotationEditorFactory
    extends AnnotationEditorFactoryImplBase
    implements ClientSideUserPreferencesProvider
{
    private final ExternalEditorPluginDescripion description;

    private WatchedResourceFile<PolicyCollection> policyResource;
    private WatchedResourceFile<JsonSchema> userPreferencesSchema;

    public ExternalAnnotationEditorFactory(ExternalEditorPluginDescripion aDescription)
    {
        description = aDescription;

        var policyFile = getDescription().getBasePath().resolve("policy.yaml");
        try {
            policyResource = new WatchedResourceFile<>(policyFile,
                    PolicyCollectionIOUtils::loadPolicies);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }

        var userPreferencesSchemaFile = getDescription().getBasePath()
                .resolve("userPreferences.schema.json");
        try {
            userPreferencesSchema = new WatchedResourceFile<>(userPreferencesSchemaFile,
                    JSONUtil::loadJsonSchema);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getDisplayName()
    {
        return description.getName();
    }

    public ExternalEditorPluginDescripion getDescription()
    {
        return description;
    }

    @Override
    public void initState(AnnotatorState aModelObject)
    {
        aModelObject.setPagingStrategy(new NoPagingStrategy());
    }

    @Override
    public AnnotationEditorBase create(String aId, IModel<AnnotatorState> aModel,
            AnnotationActionHandler aActionHandler, CasProvider aCasProvider)
    {
        return new ExternalAnnotationEditor(aId, aModel, aActionHandler, aCasProvider,
                getBeanName());
    }

    @Override
    public Optional<PolicyCollection> getPolicy() throws IOException
    {
        return policyResource.get();
    }

    @Override
    public Optional<JsonSchema> getUserPreferencesSchema() throws IOException
    {
        return userPreferencesSchema.get();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <T extends PreferenceValue> Optional<ClientSidePreferenceKey<T>> getUserPreferencesKey()
    {
        return (Optional) Optional.of(new ClientSidePreferenceKey<ClientSidePreferenceMapValue>(
                ClientSidePreferenceMapValue.class, "annotation/" + getBeanName()));
    }
}
