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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.exporter;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.api.export.FullProjectExportRequest;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExportTaskMonitor;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectExporter;
import de.tudarmstadt.ukp.clarin.webanno.api.export.ProjectImportRequest;
import de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.recommendation.api.RecommendationService;
import de.tudarmstadt.ukp.inception.recommendation.exporter.RecommenderExporter;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.config.StringMatchingRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.GazeteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazeteer.model.Gazeteer;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link StringMatchingRecommenderAutoConfiguration#gazeteerExporter}.
 * </p>
 */
public class GazeteerExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(GazeteerExporter.class);

    private static final String KEY = "gazeteers";

    private static final String GAZETEERS_FOLDER = "gazeteers";

    private final RecommendationService recommendationService;
    private final GazeteerService gazeteerService;

    @Autowired
    public GazeteerExporter(RecommendationService aRecommendationService,
            GazeteerService aGazeteerService)
    {
        recommendationService = aRecommendationService;
        gazeteerService = aGazeteerService;
    }

    @Override
    public List<Class<? extends ProjectExporter>> getImportDependencies()
    {
        return asList(RecommenderExporter.class);
    }

    @Override
    public void exportData(FullProjectExportRequest aRequest, ProjectExportTaskMonitor aMonitor,
            ExportedProject aExProject, ZipOutputStream aStage)
        throws IOException
    {
        var project = aRequest.getProject();

        var exportedGazeteers = new ArrayList<ExportedGazeteer>();
        for (var recommender : recommendationService.listRecommenders(project)) {
            for (var gazeteer : gazeteerService.listGazeteers(recommender)) {
                var exportedGazeteer = new ExportedGazeteer();
                exportedGazeteer.setId(gazeteer.getId());
                exportedGazeteer.setName(gazeteer.getName());
                exportedGazeteer.setRecommender(recommender.getName());
                exportedGazeteers.add(exportedGazeteer);

                ProjectExporter.writeEntry(aStage,
                        GAZETEERS_FOLDER + "/" + gazeteer.getId() + ".txt", os -> {
                            try (var is = Files.newInputStream(
                                    gazeteerService.getGazeteerFile(gazeteer).toPath())) {
                                is.transferTo(os);
                            }
                        });
            }
        }

        aExProject.setProperty(KEY, exportedGazeteers);

        LOG.info("Exported [{}] gazeteers for project [{}]", exportedGazeteers.size(),
                project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var gazeteers = aExProject.getArrayProperty(KEY, ExportedGazeteer.class);

        for (var exportedGazeteer : gazeteers) {
            var recommender = recommendationService
                    .getRecommender(aProject, exportedGazeteer.getRecommender()).get();

            var gazeteer = new Gazeteer();
            gazeteer.setName(exportedGazeteer.getName());
            gazeteer.setRecommender(recommender);
            gazeteerService.createOrUpdateGazeteer(gazeteer);

            var entry = aZip.getEntry(GAZETEERS_FOLDER + "/" + exportedGazeteer.getId() + ".txt");
            try (var is = aZip.getInputStream(entry)) {
                gazeteerService.importGazeteerFile(gazeteer, is);
            }
        }

        LOG.info("Imported [{}] gazeteeres for project [{}]", gazeteers.length, aProject.getName());
    }
}
