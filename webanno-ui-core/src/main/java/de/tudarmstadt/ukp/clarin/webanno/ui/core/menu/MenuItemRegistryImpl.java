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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.menu;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.OrderComparator;
import org.springframework.stereotype.Component;

@Component(MenuItemRegistry.SERVICE_NAME)
public class MenuItemRegistryImpl
    implements MenuItemRegistry
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private List<MenuItem> menuItems;
    
    public MenuItemRegistryImpl(@Autowired List<MenuItem> aMenuItems)
    {
        OrderComparator.sort(aMenuItems);
        
        for (MenuItem mi : aMenuItems) {
            log.info("Found menu item: {}", ClassUtils.getAbbreviatedName(mi.getClass(), 20));
        }
        
        menuItems = Collections.unmodifiableList(aMenuItems);
    }
    
    @Override
    public List<MenuItem> getMenuItems()
    {
        return menuItems;
    }
}
