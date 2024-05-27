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
package de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.request.resource.ResourceReference;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.inception.project.api.ProjectInitializer;
import de.tudarmstadt.ukp.inception.project.initializers.sentencelabeling.config.InceptionSentenceLabelingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.support.wicket.resource.Strings;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionSentenceLabelingProjectInitializersAutoConfiguration#sentenceLabelingProjectInitializer}.
 * </p>
 */
@Order(2000)
public class SentenceLabelingProjectInitializer
    implements QuickProjectInitializer
{
    private static final PackageResourceReference THUMBNAIL = new PackageResourceReference(
            MethodHandles.lookup().lookupClass(), "SentenceLabelingProjectInitializer.svg");

    @Override
    public String getName()
    {
        return "Sentence classification";
    }

    @Override
    public Optional<String> getDescription()
    {
        return Optional.of(Strings.getString("sentence-labeling-project.description"));
    }

    @Override
    public Optional<ResourceReference> getThumbnail()
    {
        return Optional.of(THUMBNAIL);
    }

    @Override
    public boolean alreadyApplied(Project aProject)
    {
        return false;
    }

    @Override
    public List<Class<? extends ProjectInitializer>> getDependencies()
    {
        return asList(
                // Because all projects should have a Token layer
                TokenLayerInitializer.class, //
                SentenceLabelLayerInitializer.class, //
                SentenceLabelRecommenderInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        URL descriptionUrl = getClass().getResource("SentenceLabelingProjectInitializer.md");
        aProject.setDescription(
                // Empty line to avoid the this text showing up in the short description of the
                // project overview
                "\n" + IOUtils.toString(descriptionUrl, UTF_8));
    }
}
