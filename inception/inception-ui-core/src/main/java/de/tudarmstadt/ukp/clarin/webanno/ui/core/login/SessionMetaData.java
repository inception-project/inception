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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.login;

import java.util.LinkedHashMap;

import org.apache.wicket.MetaDataKey;
import org.apache.wicket.util.string.StringValue;

public final class SessionMetaData
{
    /**
     * Holds parameters that were supplied at login time via an URL fragment. The login page stores
     * them here because they cannot be safely forwarded via the redirection URL.
     */
    public static final MetaDataKey<LinkedHashMap<String, StringValue>> LOGIN_URL_FRAGMENT_PARAMS = //
            new MetaDataKey<LinkedHashMap<String, StringValue>>()
            {
                private static final long serialVersionUID = 1L;
            };

    private SessionMetaData()
    {
        // No instances
    }
}
