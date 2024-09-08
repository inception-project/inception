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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.menu;

import static de.tudarmstadt.ukp.inception.support.logging.BaseLoggers.BOOT_LOG;
import static java.util.Collections.emptyList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.ClassUtils;
import org.apache.wicket.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.stereotype.Component;

@Component(MenuItemRegistry.SERVICE_NAME)
public class MenuItemRegistryImpl
    implements MenuItemRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final List<MenuItem> extensionsProxy;

    private List<MenuItem> extensions;

    public MenuItemRegistryImpl(@Lazy @Autowired(required = false) List<MenuItem> aExtensions)
    {
        extensionsProxy = aExtensions;
    }

    @EventListener
    public void onContextRefreshedEvent(ContextRefreshedEvent aEvent)
    {
        init();
    }

    /* package private */ void init()
    {
        var exts = new ArrayList<MenuItem>();

        if (extensionsProxy != null) {
            exts.addAll(extensionsProxy);
            AnnotationAwareOrderComparator.sort(exts);

            for (var fs : exts) {
                log.debug("Found menu item: {}", ClassUtils.getAbbreviatedName(fs.getClass(), 20));

                if (fs.getPageClass() == null) {
                    throw new IllegalStateException("Menu item [" + fs.getClass().getName()
                            + "] does not declare a page class.");
                }
            }
        }

        BOOT_LOG.info("Found [{}] menu items", exts.size());

        extensions = Collections.unmodifiableList(exts);
    }

    @Override
    public Optional<MenuItem> getMenuItem(Class<? extends Page> aClass)
    {
        if (extensions == null) {
            return Optional.empty();
        }

        return extensions.stream().filter(mi -> mi.getPageClass().equals(aClass)).findFirst();
    }

    @Override
    public List<MenuItem> getMenuItems()
    {
        if (extensions == null) {
            return emptyList();
        }

        return extensions;
    }
}
