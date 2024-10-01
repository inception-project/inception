/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
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
package de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.exporter;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
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
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.chains.WeblichtChainService;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.config.WeblichtRecommenderAutoConfiguration;
import de.tudarmstadt.ukp.inception.recommendation.imls.weblicht.model.WeblichtChain;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link WeblichtRecommenderAutoConfiguration#chainExporter}.
 * </p>
 */
public class ChainExporter
    implements ProjectExporter
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String KEY = "weblicht-chains";

    private static final String CHAINS_FOLDER = "weblicht-chains";

    private final RecommendationService recommendationService;
    private final WeblichtChainService chainService;

    @Autowired
    public ChainExporter(RecommendationService aRecommendationService,
            WeblichtChainService aChainService)
    {
        recommendationService = aRecommendationService;
        chainService = aChainService;
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

        var exportedChains = new ArrayList<ExportedWeblichtChain>();
        for (var recommender : recommendationService.listRecommenders(project)) {
            var optChain = chainService.getChain(recommender);
            if (optChain.isPresent()) {
                var chain = optChain.get();
                var exportedChain = new ExportedWeblichtChain();
                exportedChain.setId(chain.getId());
                exportedChain.setName(chain.getName());
                exportedChain.setRecommender(recommender.getName());
                exportedChains.add(exportedChain);

                ProjectExporter.writeEntry(aStage, CHAINS_FOLDER + "/" + chain.getId() + ".xml",
                        os -> {
                            try (var is = Files
                                    .newInputStream(chainService.getChainFile(chain).toPath())) {
                                is.transferTo(os);
                            }
                        });
            }
        }

        aExProject.setProperty(KEY, exportedChains);

        LOG.info("Exported [{}] chains for project [{}]", exportedChains.size(), project.getName());
    }

    @Override
    public void importData(ProjectImportRequest aRequest, Project aProject,
            ExportedProject aExProject, ZipFile aZip)
        throws Exception
    {
        var exportedChains = aExProject.getArrayProperty(KEY, ExportedWeblichtChain.class);

        for (var exportedChain : exportedChains) {
            var recommender = recommendationService
                    .getRecommender(aProject, exportedChain.getRecommender()).get();

            var chain = new WeblichtChain();
            chain.setName(exportedChain.getName());
            chain.setRecommender(recommender);
            chainService.createOrUpdateChain(chain);

            var entry = ProjectExporter.getEntry(aZip,
                    CHAINS_FOLDER + "/" + exportedChain.getId() + ".xml");
            try (var is = aZip.getInputStream(entry)) {
                chainService.importChainFile(chain, is);
            }
        }

        LOG.info("Imported [{}] chains for project [{}]", exportedChains.length,
                aProject.getName());
    }
}
