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

import org.apache.wicket.Page;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.WicketApplicationBase;

public interface MenuItem
{
    String getPath();
    String getIcon();
    String getLabel();
    Class<? extends Page> getPageClass();
    boolean applies();
    
    /**
     * {@link WicketApplicationBase} introduces a custom authorization service which prevents access
     * to a page if the user is not able to see the corresponding menu item (cf.
     * {@link #applies()}). However, in some cases, it may be necessary to grant the user direct
     * access to a page even if the user does not see the corresponding menu item. For example,
     * users should not see the menu item for the user management page, but they may access this
     * page directly in order to edit their own profile. In such cases, this method should return
     * {@code true}. Use this very carefully and ensure that the proper access checks are performed
     * on the target page.
     */
    default boolean isDirectAccessAllowed() {
        return false;
    }
}
