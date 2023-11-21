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

public enum DeploymentMode
{
    /**
     * Probably running as a service based on the standalone JAR using the embedded server in
     * Docker.
     */
    SERVER_JAR_DOCKER,

    /**
     * Probably running as a service based on the standalone JAR using the embedded server.
     */
    SERVER_JAR,

    /**
     * Probably running as a service based on the WAR file using an external application server in
     * Docker.
     */
    SERVER_WAR_DOCKER,

    /**
     * Probably running as a service based on the WAR file using an external application server.
     */
    SERVER_WAR,

    /**
     * Probably running as a desktop application.
     */
    DESKTOP;
}
