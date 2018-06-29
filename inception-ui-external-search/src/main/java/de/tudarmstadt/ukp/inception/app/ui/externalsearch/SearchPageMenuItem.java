/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.app.ui.externalsearch;

import org.apache.wicket.Page;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.menu.MenuItem;

@Component
@Order(50)
public class SearchPageMenuItem implements MenuItem
{
    @Override
    public String getIcon()
    {
        return "images/magnifier.png";
    }
    
    @Override
    public String getLabel()
    {
        return "Search";
    }
    
    @Override
    public boolean applies()
    {
        return true;
    }
    
    @Override
    public Class<? extends Page> getPageClass()
    {
        return SearchPage.class;
    }
}
