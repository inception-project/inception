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
package de.tudarmstadt.ukp.inception.io.xml;

import static de.tudarmstadt.ukp.inception.io.xml.CustomXmlFormatLoader.CUSTOM_XML_FORMAT_PREFIX;
import static de.tudarmstadt.ukp.inception.io.xml.CustomXmlFormatLoader.PLUGINS_XML_FORMAT_BASE_NAME;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.Application;
import org.apache.wicket.request.resource.ResourceReference;
import org.apache.wicket.request.resource.ResourceReferenceRegistry;
import org.apache.wicket.resource.FileSystemResourceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.api.format.UimaReaderWriterFormatSupport_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlDocumentReader;
import de.tudarmstadt.ukp.inception.io.xml.dkprocore.XmlNodeUtils;
import de.tudarmstadt.ukp.inception.support.io.WatchedResourceFile;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollection;
import de.tudarmstadt.ukp.inception.support.xml.sanitizer.PolicyCollectionIOUtils;

public class CustomXmlFormatFactory
    extends UimaReaderWriterFormatSupport_ImplBase
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CustomXmlFormatPluginDescripion description;
    private final Application wicketApplication;
    private final List<ResourceReference> stylesheetReferences;
    private final List<ResourceReference> scriptReferences;

    private WatchedResourceFile<PolicyCollection> policyResource;

    public CustomXmlFormatFactory(CustomXmlFormatPluginDescripion aDescription,
            Application aWicketApplication)
    {
        description = aDescription;
        wicketApplication = aWicketApplication;

        var policyFile = description.getBasePath().resolve("policy.yaml");
        try {
            policyResource = new WatchedResourceFile<>(policyFile,
                    PolicyCollectionIOUtils::loadPolicies);
        }
        catch (IOException e) {
            throw new IllegalStateException(e);
        }

        var basePath = description.getBasePath().normalize();
        stylesheetReferences = buildReferences(basePath, description.getStylesheets(),
                "Stylesheet");
        scriptReferences = buildReferences(basePath, description.getScripts(), "Script");
    }

    private List<ResourceReference> buildReferences(Path aBasePath, List<String> aResources,
            String aKind)
    {
        var references = new ArrayList<ResourceReference>();
        for (var resource : aResources) {
            var resourcePath = aBasePath.resolve(resource).normalize();
            if (!resourcePath.startsWith(aBasePath)) {
                LOG.warn("{} in custom XML format [{}] has illegal path [{}]", aKind,
                        description.getId(), resource);
                continue;
            }
            var ref = new FileSystemResourceReference(
                    PLUGINS_XML_FORMAT_BASE_NAME + description.getId() + "/" + resource,
                    resourcePath);
            references.add(ref);
        }
        return references;
    }

    private ResourceReferenceRegistry resolveRegistry()
    {
        // Prefer the application that was handed to us, but fall back to the thread-bound
        // application if that one is not (yet) initialized.
        if (wicketApplication != null) {
            var registry = wicketApplication.getResourceReferenceRegistry();
            if (registry != null) {
                return registry;
            }
        }

        if (Application.exists()) {
            return Application.get().getResourceReferenceRegistry();
        }

        return null;
    }

    /**
     * Register the references with the Wicket application's resource reference registry. The
     * registry is only available once the Wicket application has been initialized. During early
     * bean instantiation (e.g. in integration tests) it may not yet be available, so we skip
     * registration and rely on this method being called again on first access via
     * {@link #getCssStylesheets()} / {@link #getJavaScripts()}, by which point the registry is
     * available. Registration is idempotent, so repeating it per access is safe.
     */
    private void ensureRegistered(List<ResourceReference> aReferences)
    {
        var registry = resolveRegistry();
        if (registry == null) {
            return;
        }

        for (var ref : aReferences) {
            registry.registerResourceReference(ref);
        }
    }

    @Override
    public String getId()
    {
        return CUSTOM_XML_FORMAT_PREFIX + description.getId();
    }

    @Override
    public String getName()
    {
        return description.getName();
    }

    @Override
    public List<ResourceReference> getCssStylesheets()
    {
        ensureRegistered(stylesheetReferences);
        return stylesheetReferences;
    }

    @Override
    public List<ResourceReference> getJavaScripts()
    {
        ensureRegistered(scriptReferences);
        return scriptReferences;
    }

    @Override
    public Optional<String> getDocumentStructureFactory()
    {
        return Optional.ofNullable(description.getDocumentStructureFactory());
    }

    @Override
    public Optional<PolicyCollection> getPolicy() throws IOException
    {
        return policyResource.get();
    }

    @Override
    public Set<String> getSectionElements()
    {
        return description.getSectionElements();
    }

    @Override
    public CollectionReaderDescription getReaderDescription(Project aProject,
            TypeSystemDescription aTSD)
        throws ResourceInitializationException
    {
        return createReaderDescription( //
                XmlDocumentReader.class, aTSD, //
                XmlDocumentReader.PARAM_BLOCK_ELEMENTS, description.getBlockElements(), //
                XmlDocumentReader.PARAM_SPLIT_SENTENCES_IN_BLOCK_ELEMENTS,
                description.isSplitSentencesInBlockElements());
    }

    @Override
    public void prepareAnnotationCas(CAS aInitialCas, SourceDocument aDocument)
    {
        XmlNodeUtils.removeXmlDocumentStructure(aInitialCas);
    }

    @Override
    public boolean isReadable()
    {
        return true;
    }
}
