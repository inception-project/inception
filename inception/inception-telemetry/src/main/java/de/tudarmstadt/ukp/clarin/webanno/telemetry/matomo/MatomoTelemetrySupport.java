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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.matomo;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.security.web.session.HttpSessionCreatedEvent;

import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetrySupport;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.event.TelemetrySettingsSavedEvent;

/*
 * Interface to allow Spring to invoke the event and to avoid the error "Need to invoke method 
 * 'onApplicationEvent' declared on target class 'MatomoTelemetry', but not found in any 
 * interface(s) of the exposed proxy type. Either pull the method up to an interface or switch 
 * to CGLIB proxies by enforcing proxy-target-class mode in your configuration."
 */
public interface MatomoTelemetrySupport
    extends TelemetrySupport<MatomoTelemetryTraits>
{
    void onApplicationReady(ApplicationReadyEvent aEvent);

    void onTelemetrySettingsSaved(TelemetrySettingsSavedEvent aEvent);

    void onSessionCreated(HttpSessionCreatedEvent aEvent);
}
