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
package de.tudarmstadt.ukp.inception.support.deployment;

import org.springframework.boot.web.context.WebServerInitializedEvent;

public interface DeploymentModeService
{
    String PROFILE_CLI_MODE = "cli";
    String PROFILE_APPLICATION_MODE = "app";

    String PROFILE_DEVELOPMENT_MODE = "development";
    String PROFILE_PRODUCTION_MODE = "production";

    String PROFILE_AUTH_MODE_DATABASE = "auth-mode-builtin";
    String PROFILE_AUTH_MODE_EXTERNAL_PREAUTH = "auth-mode-preauth";

    String PROFILE_EXTERNAL_SERVER = "external-server";
    String PROFILE_INTERNAL_SERVER = "embedded-server";

    void onApplicationEvent(WebServerInitializedEvent aEvt);

    DeploymentMode getDeploymentMode();

    boolean isDesktopInstance();

    /**
     * @return if the embedded server was used (i.e. not running as a WAR) and running in Docker.
     */
    boolean isDockerized();

    /**
     * @return if the embedded server was used (i.e. not running as a WAR).
     */
    boolean isEmbeddedServerDeployment();
}
