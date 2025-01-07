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
package de.tudarmstadt.ukp.clarin.webanno.project.initializers;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.uima.cas.CAS;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.annotation.layer.LayerFactory;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializationRequest;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#partOfSpeechLayerInitializer}.
 * </p>
 */
public class PartOfSpeechLayerInitializer
    implements LayerInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "PartOfSpeechLayerInitializer.svg");

    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public PartOfSpeechLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Part-of-speech tagging";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("pos-layer.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because locks to token boundaries
                TokenLayerInitializer.class,
                // Tagsets
                PartOfSpeechTagSetInitializer.class);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(POS.class.getName(), aProject);
    }

    @Override
    public void configure(ProjectInitializationRequest aRequest) throws IOException
    {
        var project = aRequest.getProject();
        var posTagSet = annotationSchemaService
                .getTagSet(PartOfSpeechTagSetInitializer.TAG_SET_NAME, project);

        var tokenLayer = annotationSchemaService.findLayer(project, Token.class.getName());

        var tokenPosFeature = new AnnotationFeature(project, tokenLayer, "pos", "pos",
                POS.class.getName());
        annotationSchemaService.createFeature(tokenPosFeature);

        var posLayer = LayerFactory.partOfSpeechLayer(project, tokenPosFeature).build();

        annotationSchemaService.createOrUpdateLayer(posLayer);

        var xpos = new AnnotationFeature(project, posLayer, "PosValue", "XPOS",
                CAS.TYPE_NAME_STRING, "XPOS", null);
        xpos.setDescription("Language-specific part-of-speech tag");
        annotationSchemaService.createFeature(xpos);

        var upos = new AnnotationFeature(project, posLayer, "coarseValue", "UPOS",
                CAS.TYPE_NAME_STRING, "UPOS", posTagSet);
        upos.setDescription("Universal part-of-speech tag");
        annotationSchemaService.createFeature(upos);

    }
}
