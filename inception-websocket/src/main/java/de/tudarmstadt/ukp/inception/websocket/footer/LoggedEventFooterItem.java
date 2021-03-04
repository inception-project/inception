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
package de.tudarmstadt.ukp.inception.websocket.footer;

import org.apache.wicket.Component;
import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.ui.core.footer.FooterItem;

//FIXME does not show up in footer without this order, would like to move to config-file
@Order(FooterItem.ORDER_LEFT + 500)
@org.springframework.stereotype.Component
public class LoggedEventFooterItem implements FooterItem
{

    @Override
    public Component create(String aId)
    {
        return new LoggedEventFooterPanel(aId);
    }

}
