/*
 * Copyright 2021
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.support.axios;

import de.agilecoders.wicket.webjars.request.resource.WebjarsJavaScriptResourceReference;

public class AxiosResourceReference
    extends WebjarsJavaScriptResourceReference
{
    private static final long serialVersionUID = 1L;

    private static final AxiosResourceReference INSTANCE = new AxiosResourceReference();

    /**
     * Gets the instance of the resource reference
     *
     * @return the single instance of the resource reference
     */
    public static AxiosResourceReference get()
    {
        return INSTANCE;
    }

    /**
     * Private constructor
     */
    private AxiosResourceReference()
    {
        super("axios/current/dist/axios.js");
    }
}
