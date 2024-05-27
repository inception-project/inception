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
package de.tudarmstadt.ukp.clarin.webanno.webapp.remoteapi.menubar;

import static de.tudarmstadt.ukp.clarin.webanno.security.model.Role.ROLE_REMOTE;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;

import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;

public class SwaggerUiMenuBarItem
    extends Panel
{
    private static final long serialVersionUID = 7486091139970717604L;

    private static final String CID_SWAGGER_LINK = "swaggerLink";

    private @SpringBean UserDao userRepository;

    public SwaggerUiMenuBarItem(String aId)
    {
        super(aId);

        var user = LoadableDetachableModel.of(userRepository::getCurrentUser);

        add(new ExternalLink(CID_SWAGGER_LINK, LoadableDetachableModel.of(this::getSwaggerUiUrl))
                .add(visibleWhen(
                        user.map(u -> userRepository.hasRole(u, ROLE_REMOTE)).orElse(false))));
    }

    private String getSwaggerUiUrl()
    {
        return RequestCycle.get().getUrlRenderer().renderContextRelativeUrl("/swagger-ui.html");
    }
}
