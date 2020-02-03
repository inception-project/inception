/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.ui.core.footer;

import org.apache.wicket.Component;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.footer.FooterItem;

@Order(100)
@org.springframework.stereotype.Component
public class TutorialFooterItem
    implements FooterItem
{
    @Override
    public Component create(String aId)
    {
        return new TutorialFooterPanel(aId);
    }
}
