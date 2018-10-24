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
package de.tudarmstadt.ukp.clarin.webanno.ui.core.users;

import java.util.List;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.ListPanel_ImplBase;
import de.tudarmstadt.ukp.clarin.webanno.support.wicket.OverviewListChoice;

class UserSelectionPanel
    extends ListPanel_ImplBase
{
    private static final long serialVersionUID = -1L;

    private @SpringBean UserDao userRepository;

    private OverviewListChoice<User> overviewList;

    public UserSelectionPanel(String id, IModel<User> aModel)
    {
        super(id);
        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        overviewList = new OverviewListChoice<>("user");
        overviewList.setChoiceRenderer(new ChoiceRenderer<User>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(User aUser)
            {
                return aUser.getUsername() + (aUser.isEnabled() ? "" : " (disabled)");
            }
        });
        overviewList.setModel(aModel);
        overviewList.setChoices(this::listUsers);
        overviewList.add(new LambdaAjaxFormComponentUpdatingBehavior("change", this::onChange));
        add(overviewList);

        add(new LambdaAjaxLink("create", this::actionCreate));
    }

    private List<User> listUsers()
    {
        return userRepository.list();
    }
}
