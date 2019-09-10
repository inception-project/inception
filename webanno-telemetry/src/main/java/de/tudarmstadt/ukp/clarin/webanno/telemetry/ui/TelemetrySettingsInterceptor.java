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
package de.tudarmstadt.ukp.clarin.webanno.telemetry.ui;

import org.apache.wicket.Page;
import org.apache.wicket.RestartResponseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.interceptors.GlobalInterceptor;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetryService;
import de.tudarmstadt.ukp.clarin.webanno.telemetry.TelemetrySupport;

/**
 * Make sure that administrator users are force to provide valid choices for telemetry collection
 * before they can use the application.
 */
@Component
public class TelemetrySettingsInterceptor implements GlobalInterceptor
{
    private final UserDao userService;
    private final TelemetryService telemetryService;
    
    private boolean allValid = false;
    
    @Autowired
    public TelemetrySettingsInterceptor(UserDao aUserService, TelemetryService aTelemetryService)
    {
        userService = aUserService;
        telemetryService = aTelemetryService;
    }

    @Override
    public void intercept(Page aPage)
    {
        // We actually have to check for this interceptor only once since the telemetry settings
        // cannot (normally) become invalid while the application is running and therefore
        // once we have asserted that they are all valid, we don't have to check anymore.
        if (allValid) {
            return;
        }
        
        // Do nothing if we are already on the telemetry settings page
        if (aPage instanceof TelemetrySettingsPage) {
            return;
        }

        // Intercept only if the user is already logged in
        if (SecurityContextHolder.getContext()
                .getAuthentication() instanceof AnonymousAuthenticationToken) {
            return;
        }
        
        // Only direct admin users to the telemetry settings page
        if (!userService.isAdministrator(userService.getCurrentUser())) {
            return;
        }
        
        
        for (TelemetrySupport<?> support : telemetryService.getTelemetrySupports()) {
            if (!support.hasValidSettings()) {
                throw new RestartResponseException(TelemetrySettingsPage.class);
            }
        }
        
        allValid = true;
    }
}
