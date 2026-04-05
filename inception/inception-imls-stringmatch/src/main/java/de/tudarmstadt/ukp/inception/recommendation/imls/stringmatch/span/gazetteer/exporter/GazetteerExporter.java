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
package de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.exporter;

import static java.nio.file.Files.newInputStream;
import static java.util.Arrays.asList;

import java.io.IOException;
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
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.GazetteerService;
import de.tudarmstadt.ukp.inception.recommendation.imls.stringmatch.span.gazetteer.model.Gazetteer;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link StringMatchingRecommenderAutoConfiguration#gazetteerExporter}.
 * </p>
 */
public class GazetteerExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(GazetteerExporter.class);

    // Historic typo in the key name - fix later
    private static final String KEY = "gazeteers";

    // Historic typo in the folder name - fix later
    private static final String GAZETTEERS_FOLDER = "gazeteers";

    private final RecommendationService recommendationService;
    private final GazetteerService gazetteerService;

    @Autowired
    public GazetteerExporter(RecommendationService aRecommendationService,
            GazetteerService aGazetteerService)
    {
        recommendationService = aRecommendationService;
        gazetteerService = aGazetteerService;
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

        var exportedGazetteers = new ArrayList<ExportedGazetteer>();
        for (var recommender : recommendationService.listRecommenders(project)) {
            for (var gazetteer : gazetteerService.listGazetteers(recommender)) {
                var exportedGazetteer = new ExportedGazetteer();
                exportedGazetteer.setId(gazetteer.getId());
                exportedGazetteer.setName(gazetteer.getName());
                exportedGazetteer.setRecommender(recommender.getName());
                exportedGazetteers.add(exportedGazetteer);

                ProjectExporter.writeEntry(aStage,
                        GAZETTEERS_FOLDER + "/" + gazetteer.getId() + ".txt", os -> {
                            try (var is = newInputStream(
                                    gazetteerService.getGazetteerFile(gazetteer).toPath())) {
                                is.transferTo(os);
                            }
                        });
            }
        }

        aExProject.setProperty(KEY, exportedGazetteers);

        LOG.info("Exported [{}] gazetteers for project [{}]", exportedGazetteers.size(),
                project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var gazetteers = aExProject.getArrayProperty(KEY, ExportedGazetteer.class);

        for (var exportedGazetteer : gazetteers) {
            var recommender = recommendationService
                    .getRecommender(aProject, exportedGazetteer.getRecommender()).get();

            var gazetteer = new Gazetteer();
            gazetteer.setName(exportedGazetteer.getName());
            gazetteer.setRecommender(recommender);
            gazetteerService.createOrUpdateGazetteer(gazetteer);

            var entry = aZip.getEntry(GAZETTEERS_FOLDER + "/" + exportedGazetteer.getId() + ".txt");
            try (var is = aZip.getInputStream(entry)) {
                gazetteerService.importGazetteerFile(gazetteer, is);
            }
        }

        LOG.info("Imported [{}] gazetteers for project [{}]", gazetteers.length, aProject.getName());
    }
}
