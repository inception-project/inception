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
package de.tudarmstadt.ukp.inception.project.initializers.doclabeling;

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.TOKENS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.ANY_OVERLAP;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.LayerInitializer;
import de.tudarmstadt.ukp.inception.annotation.layer.document.api.DocumentMetadataLayerSupport;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.doclabeling.config.InceptionDocumentLabelingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionDocumentLabelingProjectInitializersAutoConfiguration#basicDocumentTagLayerInitializer}.
 * </p>
 */
@Order(40)
public class BasicDocumentLabelLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "BasicDocumentLabelLayerInitializer.svg");

    public static final String BASIC_DOCUMENT_LABEL_LAYER_NAME = "custom.DocumentLabel";
    public static final String BASIC_DOCUMENT_LABEL_LABEL_FEATURE_NAME = "label";

    private final AnnotationSchemaService annotationSchemaService;
    private final DocumentMetadataLayerSupport docLayerSupport;

    @Autowired
    public BasicDocumentLabelLayerInitializer(AnnotationSchemaService aAnnotationSchemaService,
            DocumentMetadataLayerSupport aDocLayerSupport)
    {
        annotationSchemaService = aAnnotationSchemaService;
        docLayerSupport = aDocLayerSupport;
    }

    @Override
    public String getName()
    {
        return "Generic document classification";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("document-labeling-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public boolean applyByDefault()
    {
        return false;
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(BASIC_DOCUMENT_LABEL_LAYER_NAME, aProject);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Tagsets
                BasicDocumentLabelTagSetInitializer.class);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();

        var docTagLayer = new AnnotationLayer(BASIC_DOCUMENT_LABEL_LAYER_NAME, "Document Label",
                DocumentMetadataLayerSupport.TYPE, project, false, TOKENS, ANY_OVERLAP);
        var traits = docLayerSupport.readTraits(docTagLayer);
        traits.setSingleton(true);
        docLayerSupport.writeTraits(docTagLayer, traits);
        annotationSchemaService.createOrUpdateLayer(docTagLayer);

        var docLabelTagSet = annotationSchemaService.getTagSet(
                BasicDocumentLabelTagSetInitializer.BASIC_DOCUMENT_LABEL_TAG_SET_NAME, project);

        annotationSchemaService.createFeature(
                new AnnotationFeature(project, docTagLayer, BASIC_DOCUMENT_LABEL_LABEL_FEATURE_NAME,
                        "Label", CAS.TYPE_NAME_STRING, "Document label", docLabelTagSet));
    }
}
