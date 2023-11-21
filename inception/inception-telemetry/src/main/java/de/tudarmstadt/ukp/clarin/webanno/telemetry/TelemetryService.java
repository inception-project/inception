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
package de.tudarmstadt.ukp.clarin.webanno.telemetry;

import java.util.List;
import java.util.Optional;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;
import de.tudarmstadt.ukp.inception.support.deployment.DeploymentMode;

public interface TelemetryService
{
    List<TelemetrySupport<?>> getTelemetrySupports();

    /**
     * @param aSupport
     *            a support.
     * @param <T>
     *            a support traits type.
     * @return the telemetry settings from the DB. If there are no settings yet, this method returns
     *         a new settings object.
     */
    <T> TelemetrySettings readOrCreateSettings(TelemetrySupport<T> aSupport);

    <T> Optional<TelemetrySettings> readSettings(TelemetrySupport<T> aSupport);

    void writeAllSettings(List<TelemetrySettings> aSettings);

    List<TelemetrySettings> listSettings();

    <T> Optional<TelemetrySupport<T>> getTelemetrySuppport(String aSupport);

    DeploymentMode getDeploymentMode();
}
