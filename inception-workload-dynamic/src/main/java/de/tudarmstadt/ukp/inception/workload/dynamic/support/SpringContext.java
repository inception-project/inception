/*
 * Copyright 2020
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

package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import java.io.Serializable;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;




//Helper class to get content of spring beans in other classes

@Component
public class SpringContext implements ApplicationContextAware, Serializable
{

    private static final long serialVersionUID = -7535828272527355826L;
    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext aContext) throws BeansException
    {
        this.context = aContext;
    }

    public static <T extends Object> T getBean(Class<T> aBeanClass)
    {
        return context.getBean(aBeanClass);
    }
}
