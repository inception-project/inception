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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode.CHARACTERS;
import static de.tudarmstadt.ukp.clarin.webanno.model.OverlapMode.NO_OVERLAP;
import static de.tudarmstadt.ukp.clarin.webanno.model.ValidationMode.NEVER;
import static de.tudarmstadt.ukp.clarin.webanno.support.WebAnnoConst.SPAN_TYPE;
import static java.util.Collections.emptyList;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.config.ProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.inception.schema.AnnotationSchemaService;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link ProjectInitializersAutoConfiguration#tokenLayerInitializer}.
 * </p>
 */
public class TokenLayerInitializer
    implements LayerInitializer
{
    private final AnnotationSchemaService annotationSchemaService;

    @Autowired
    public TokenLayerInitializer(AnnotationSchemaService aAnnotationSchemaService)
    {
        annotationSchemaService = aAnnotationSchemaService;
    }

    @Override
    public String getName()
    {
        return "Tokens";
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return emptyList();
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return annotationSchemaService.existsLayer(Token.class.getName(), aProject);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        AnnotationLayer tokenLayer = new AnnotationLayer(Token.class.getName(), "Token", SPAN_TYPE,
                aProject, true, CHARACTERS, NO_OVERLAP);

        // Since the user cannot turn off validation for the token layer if there is any kind of
        // problem with the validation functionality we are conservative here and disable validation
        // from the start.
        tokenLayer.setValidationMode(NEVER);
        tokenLayer.setReadonly(true);
        tokenLayer.setCrossSentence(false);
        tokenLayer.setEnabled(false);

        annotationSchemaService.createOrUpdateLayer(tokenLayer);
    }
}
