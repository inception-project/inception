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
package de.tudarmstadt.ukp.inception.support.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.inception.support.findbugs.SuppressFBWarnings;

/**
 * Permits access to the Spring context anywhere in the application. If any other means of accessing
 * the Spring context exist, these should be used. This class is mainly meant to be used by the
 * {@code EntityModel}.
 */
@Component("applicationContextProvider")
public class ApplicationContextProvider
    implements ApplicationContextAware
{
    private static ApplicationContext context = null;

    public static ApplicationContext getApplicationContext()
    {
        return context;
    }

    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD")
    @Override
    public void setApplicationContext(ApplicationContext aContext) throws BeansException
    {
        context = aContext;
    }
}
