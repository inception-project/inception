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

import static de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.SidebarTabbedPanel.KEY_SIDEBAR_STATE;
import static java.util.Arrays.asList;

import java.io.IOException;
import java.util.List;

import de.tudarmstadt.ukp.clarin.webanno.api.project.ProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.QuickProjectInitializer;
import de.tudarmstadt.ukp.clarin.webanno.project.initializers.TokenLayerInitializer;
import de.tudarmstadt.ukp.clarin.webanno.ui.annotation.sidebar.AnnotationSidebarState;
import de.tudarmstadt.ukp.inception.preferences.PreferencesService;
import de.tudarmstadt.ukp.inception.project.initializers.doclabeling.config.InceptionDocumentLabelingProjectInitializersAutoConfiguration;
import de.tudarmstadt.ukp.inception.ui.core.docanno.sidebar.DocumentMetadataSidebarFactory;
import de.tudarmstadt.ukp.inception.workload.matrix.MatrixWorkloadExtension;
import de.tudarmstadt.ukp.inception.workload.matrix.trait.MatrixWorkloadTraits;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManager;

/**
 * <p>
 * This class is exposed as a Spring Component via
 * {@link InceptionDocumentLabelingProjectInitializersAutoConfiguration#basicDocumentLabelingProjectInitializer}.
 * </p>
 */
public class BasicDocumentLabelingProjectInitializer
    implements QuickProjectInitializer
{
    private final PreferencesService prefService;
    private final DocumentMetadataSidebarFactory docMetaSidebar;
    private final WorkloadManagementService workloadManagementService;
    private final MatrixWorkloadExtension matrixWorkloadExtension;

    public BasicDocumentLabelingProjectInitializer(PreferencesService aPreferencesService,
            DocumentMetadataSidebarFactory aDocMetaSidebar,
            WorkloadManagementService aWorkloadManagementService,
            MatrixWorkloadExtension aMatrixWorkloadExtension)
    {
        prefService = aPreferencesService;
        docMetaSidebar = aDocMetaSidebar;
        workloadManagementService = aWorkloadManagementService;
        matrixWorkloadExtension = aMatrixWorkloadExtension;
    }

    @Override
    public String getName()
    {
        return "Basic document labeling";
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
                BasicDocumentLabelLayerInitializer.class);
    }

    @Override
    public void configure(Project aProject) throws IOException
    {
        aProject.setDescription(String.join("\n",
                // Empty line to avoid the this text showing up in the short description of the
                // project overview
                "", //
                "This project has been pre-configured for document labeling tasks.", //
                "", //
                "When you open a document, the document-level annotation sidebar is directly", //
                "visible and offers a single-choice list to choose between some example labels.", //
                "You can change the labels **Tag sets** pane of the project settings.", //
                "Depending on how many tags you add or whether you allow annotators to add tags, ", //
                "the single-choice list might turn into a combo-box or an auto-complete field.", //
                "", //
                "The confirmation dialog for marking a document as finished has been disabled in", //
                "this projects and annotators may also re-open a document for annotation if they", //
                "want. You can change this setting in the settings on the **Montoring** page."));

        AnnotationSidebarState sidebarState = prefService
                .loadDefaultTraitsForProject(KEY_SIDEBAR_STATE, aProject);
        sidebarState.setExpanded(true);
        sidebarState.setSelectedTab(docMetaSidebar.getBeanName());
        prefService.saveDefaultTraitsForProject(KEY_SIDEBAR_STATE, aProject, sidebarState);

        WorkloadManager manager = workloadManagementService
                .loadOrCreateWorkloadManagerConfiguration(aProject);
        MatrixWorkloadTraits traits = matrixWorkloadExtension.readTraits(manager);
        traits.setReopenableByAnnotator(true);
        matrixWorkloadExtension.writeTraits(traits, aProject);
        workloadManagementService.saveConfiguration(manager);
    }
}
