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
package de.tudarmstadt.ukp.inception.testscenarios.workload;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.inception.documents.api.DocumentService;
import de.tudarmstadt.ukp.inception.project.api.ProjectService;
import de.tudarmstadt.ukp.inception.testscenarios.accounts.ScenarioAccountService;
import de.tudarmstadt.ukp.inception.testscenarios.config.InceptionTestScenariosAutoConfiguration;
import de.tudarmstadt.ukp.inception.workload.model.WorkloadManagementService;

/**
 * The workload scenario under the <b>dynamic</b> workload manager.
 * <p>
 * <b>This class is exposed as a Spring Component via
 * {@link InceptionTestScenariosAutoConfiguration#dynamicWorkloadScenarioInitializer}.</b>
 */
@Order(9000)
public class DynamicWorkloadScenarioInitializer
    extends WorkloadScenarioInitializer_ImplBase
{
    public DynamicWorkloadScenarioInitializer(DocumentService aDocumentService,
            ProjectService aProjectService, UserDao aUserService,
            ScenarioAccountService aScenarioAccountService,
            WorkloadManagementService aWorkloadManagementService)
    {
        super(aDocumentService, aProjectService, aUserService, aScenarioAccountService,
                aWorkloadManagementService);
    }

    @Override
    public String getName()
    {
        return "TEST: dynamic workload, annotation in progress";
    }

    @Override
    protected String getWorkloadManager()
    {
        // The workload manager extension id; the constant is not visible from here
        return "dynamic";
    }
}
