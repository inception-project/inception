/*
 * Copyright 2017
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
package de.tudarmstadt.ukp.clarin.webanno.support.logging;

import org.slf4j.MDC;

public final class Logging
{
    public static final String KEY_PROJECT_ID = "projectId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_REPOSITORY_PATH = "repositoryPath";

    private Logging()
    {
        // No instances
    }

    public static void setMDC(long aProjectId, String aUsername)
    {
        MDC.put(KEY_PROJECT_ID, String.valueOf(aProjectId));
        MDC.put(KEY_USERNAME, aUsername);
    }

    public static void clearMDC()
    {
        MDC.remove(KEY_PROJECT_ID);
        MDC.remove(KEY_USERNAME);
    }
}
