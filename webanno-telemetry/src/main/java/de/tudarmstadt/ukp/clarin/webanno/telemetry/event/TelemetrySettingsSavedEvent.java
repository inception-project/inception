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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.event;

import java.util.List;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.model.TelemetrySettings;

/**
 * Sent when the telemetry settings are saved by the user. This event does not indicate whether any
 * of the settings actually changed.
 */
public class TelemetrySettingsSavedEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 3421993849783695318L;

    private final List<TelemetrySettings> settings;

    public TelemetrySettingsSavedEvent(Object aSource, List<TelemetrySettings> aSettings)
    {
        super(aSource);
        settings = aSettings;
    }

    public List<TelemetrySettings> getSettings()
    {
        return settings;
    }
}
