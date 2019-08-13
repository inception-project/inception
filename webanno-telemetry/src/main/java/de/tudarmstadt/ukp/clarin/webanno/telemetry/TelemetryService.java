/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
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

public interface TelemetryService
{
    List<TelemetrySupport> getTelemetrySupports();

    /**
     * Reads the telemetry settings from the DB. If there are no settings yet, this method returns
     * a new settings object.
     */
    <T> TelemetrySettings readOrCreateSettings(TelemetrySupport<T> aSupport);

    <T> Optional<TelemetrySettings> readSettings(TelemetrySupport<T> aSupport);

    void writeAllSettings(List<TelemetrySettings> aSettings);
    
    List<TelemetrySettings> listSettings();

    Optional<TelemetrySupport> getTelemetrySuppport(String aSupport);
}
