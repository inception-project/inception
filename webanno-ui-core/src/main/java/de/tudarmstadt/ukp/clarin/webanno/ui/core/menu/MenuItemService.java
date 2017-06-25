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

import java.io.Serializable;
import java.util.List;

import org.apache.wicket.Page;

public interface MenuItemService
{
    static final String SERVICE_NAME = "menuItemService";

    List<MenuItemService.MenuItemDecl> getMenuItems();

    public static class MenuItemDecl
        implements Serializable
    {
        private static final long serialVersionUID = -6839143167407389149L;
        
        public Condition condition;
        public String icon;
        public String label;
        public Class<? extends Page> page;
        public int prio;
    }
    
    @FunctionalInterface
    public static interface Condition extends Serializable
    {
        boolean applies();
    }
}
