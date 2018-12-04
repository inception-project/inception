/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.recommendation.exporter;

import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.MAX_RECOMMENDATIONS_CAP;
import static de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService.MAX_RECOMMENDATIONS_DEFAULT;
import static java.util.Arrays.asList;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.export.exporters.LayerExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.api.model.Recommender;

@Component
public class RecommenderExporter implements ProjectExporter {

    private static final String KEY = "recommenders";
    private static final Logger LOG = LoggerFactory.getLogger(RecommenderExporter.class);

    private final AnnotationSchemaService annotationService;
    private final RecommendationService recommendationService;

    @Autowired
    public RecommenderExporter(AnnotationSchemaService aAnnotationService,
                               RecommendationService aRecommendationService)
    {
        annotationService = aAnnotationService;
        recommendationService = aRecommendationService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(LayerExporter.class);
    }

    @Override
    public List<Class<? extends ProjectExporter>> getExportDependencies()
    {
        return asList(LayerExporter.class);
    }

    @Override
    public void exportData(ProjectExportRequest aRequest, ExportedProject aExProject, File aFile)
    {
        Project project = aRequest.getProject();

        List<ExportedRecommender> exportedRecommenders = new ArrayList<>();
        for (Recommender recommender : recommendationService.listRecommenders(project)) {
            ExportedRecommender exportedRecommender = new ExportedRecommender();
            exportedRecommender.setAlwaysSelected(recommender.isAlwaysSelected());
            exportedRecommender.setFeature(recommender.getFeature());
            exportedRecommender.setEnabled(recommender.isEnabled());
            exportedRecommender.setLayerName(recommender.getLayer().getName());
            exportedRecommender.setName(recommender.getName());
            exportedRecommender.setThreshold(recommender.getThreshold());
            exportedRecommender.setTool(recommender.getTool());
            exportedRecommender.setSkipEvaluation(recommender.isSkipEvaluation());
            exportedRecommender.setMaxRecommendations(recommender.getMaxRecommendations());
            exportedRecommender.setStatesForTraining(recommender.getStatesIgnoredForTraining());
            exportedRecommender.setTraits(recommender.getTraits());
            exportedRecommenders.add(exportedRecommender);
        }

        aExProject.setProperty(KEY, exportedRecommenders);
        int n = exportedRecommenders.size();
        LOG.info("Exported [{}] recommenders for project [{}]", n, project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
                           ExportedProject aExProject, ZipFile aZip) {
        ExportedRecommender[] recommenders = aExProject
                .getArrayProperty(KEY, ExportedRecommender.class);

        for (ExportedRecommender exportedRecommender : recommenders) {
            Recommender recommender = new Recommender();
            recommender.setAlwaysSelected(exportedRecommender.isAlwaysSelected());
            recommender.setFeature(exportedRecommender.getFeature());
            recommender.setEnabled(exportedRecommender.isEnabled());
            recommender.setName(exportedRecommender.getName());
            recommender.setThreshold(exportedRecommender.getThreshold());
            recommender.setTool(exportedRecommender.getTool());
            recommender.setSkipEvaluation(exportedRecommender.isSkipEvaluation());
            recommender.setMaxRecommendations(exportedRecommender.getMaxRecommendations());
            recommender.setStatesIgnoredForTraining(
                    exportedRecommender.getStatesIgnoredForTraining());
            recommender.setTraits(exportedRecommender.getTraits());

            // The value for max recommendations must be between 1 and 100
            if (recommender.getMaxRecommendations() < 1) {
                // We had a bug for some time where during export this value was not included.
                // For such cases, we import using the default value.
                recommender.setMaxRecommendations(MAX_RECOMMENDATIONS_DEFAULT);
            }
            if (recommender.getMaxRecommendations() > MAX_RECOMMENDATIONS_CAP) {
                // If the instance from where this was exported allowed more max recommendations
                // than our instance, we just reset it to our max cap.
                recommender.setMaxRecommendations(MAX_RECOMMENDATIONS_CAP);
            }

            String layerName = exportedRecommender.getLayerName();
            AnnotationLayer layer = annotationService.getLayer(layerName, aProject);
            recommender.setLayer(layer);

            recommender.setProject(aProject);
            recommendationService.createOrUpdateRecommender(recommender);
        }

        int n = recommenders.length;
        LOG.info("Imported [{}] recommenders for project [{}]", n, aProject.getName());
    }
}
