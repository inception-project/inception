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
package de.tudarmstadt.ukp.inception.search.model;

import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;

public class BulkIndexingContext
    implements AutoCloseable
{
    private final static ThreadLocal<BulkIndexingContext> INSTANCE = new ThreadLocal<>();

    private final boolean fullReindex;
    private final AnnotationSearchState prefs;
    private final Project project;
    private final List<AnnotationLayer> layers;
    private final List<AnnotationFeature> features;

    public BulkIndexingContext(Project aProject, List<AnnotationLayer> aLayers,
            List<AnnotationFeature> aFeatures, boolean aFullReindex, AnnotationSearchState aPrefs)
    {
        project = aProject;
        layers = aLayers;
        features = aFeatures;
        fullReindex = aFullReindex;
        prefs = aPrefs;
    }

    public Project getProject()
    {
        return project;
    }

    public List<AnnotationLayer> getLayers()
    {
        return layers;
    }

    public List<AnnotationFeature> getFeatures()
    {
        return features;
    }

    public boolean isFullReindex()
    {
        return fullReindex;
    }

    public AnnotationSearchState getIndexingSettings()
    {
        return prefs;
    }

    @Override
    public void close()
    {
        clear();
    }

    public static BulkIndexingContext init(Project aProject, AnnotationSchemaService aSchemaService,
            boolean aFullReindex, AnnotationSearchState aPrefs)
    {
        var features = aSchemaService.listSupportedFeatures(aProject).stream() //
                .filter(f -> f.isEnabled() && f.getLayer().isEnabled()) //
                .toList();

        var layers = aSchemaService.listSupportedLayers(aProject).stream() //
                .filter(AnnotationLayer::isEnabled) //
                .toList();

        var indexingContext = new BulkIndexingContext(aProject, layers, features, aFullReindex,
                aPrefs);
        INSTANCE.set(indexingContext);
        return indexingContext;
    }

    public static Optional<BulkIndexingContext> get()
    {
        return Optional.ofNullable(INSTANCE.get());
    }

    public static void clear()
    {
        INSTANCE.set(null);
    }

    public static boolean isFullReindexInProgress()
    {
        return get().map(BulkIndexingContext::isFullReindex).orElse(false);
    }
}
