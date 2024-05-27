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

import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.StringUtils.lowerCase;
import static org.apache.commons.lang3.StringUtils.splitByCharacterTypeCamelCase;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class StartupWatcher
    implements BeanPostProcessor
{
    private @Autowired ApplicationEventPublisher eventPublisher;

    @Override
    public Object postProcessBeforeInitialization(Object aBean, String aBeanName)
        throws BeansException
    {
        String className = aBean.getClass().getName();
        boolean isOurs = className.contains(".webanno.") || className.contains(".inception.");

        String name = aBean.getClass().getSimpleName();
        if (name.contains("$")) {
            name = StringUtils.substringBefore(name, "$");
        }

        if (name.isBlank()) {
            name = aBean.getClass().getSimpleName();
        }

        if (name.isBlank()) {
            name = aBean.getClass().getName();
        }

        boolean isInterestingForUser = isOurs && name.endsWith("AutoConfiguration");

        if (BOOT_LOG.isTraceEnabled() && !isInterestingForUser) {
            BOOT_LOG.trace("Initializing " + name);
        }

        if (isInterestingForUser) {
            name = StringUtils.removeEnd(name, "AutoConfiguration");
            name = lowerCase(join(splitByCharacterTypeCamelCase(name), SPACE));

            BOOT_LOG.info("Initializing " + name);

            eventPublisher
                    .publishEvent(new StartupProgressInfoEvent(aBean, "Initializing " + name));
        }

        return aBean;
    }
}
