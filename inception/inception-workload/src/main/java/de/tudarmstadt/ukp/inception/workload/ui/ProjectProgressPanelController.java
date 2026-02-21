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
package de.tudarmstadt.ukp.inception.workload.ui;

import static de.tudarmstadt.ukp.inception.security.config.InceptionSecurityWebUIApiAutoConfiguration.BASE_API_URL;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.inception.log.api.EventRepository.DocumentStateSnapshot;

public interface ProjectProgressPanelController
{
    String BASE_URL = BASE_API_URL + "/progress";
    String PROGRESS_PATH = "/project/{projectId}";

    String getProgressUrl(long aProjectId);

    List<DocumentStateSnapshot> progress(long aProjectId, Optional<Instant> aFrom,
            Optional<Instant> aTo, Optional<Instant> aNow);
}
