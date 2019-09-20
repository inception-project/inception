/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
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
package de.tudarmstadt.ukp.inception.recommendation.sidebar;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import de.tudarmstadt.ukp.inception.recommendation.model.RecommenderEvaluationScoreMetricEnum;

public class MetricSelectDropDownPanel
    extends Panel
{

    private static final long serialVersionUID = 4988942370126340112L;

    private static final List<RecommenderEvaluationScoreMetricEnum> METRICS = Arrays
            .asList(RecommenderEvaluationScoreMetricEnum.values());
    
    private static final String MID_METRIC_SELECT = "select";
    private static final String MID_METRIC_LINK = "link";

    private final AjaxLink<Void> link;
    private boolean isDropdownVisible = false;

    public MetricSelectDropDownPanel(String aId)
    {
        super(aId);

        final DropDownChoice<RecommenderEvaluationScoreMetricEnum> dropdown = new 
                DropDownChoice<RecommenderEvaluationScoreMetricEnum>(
                MID_METRIC_SELECT, new Model<RecommenderEvaluationScoreMetricEnum>(METRICS.get(0)),
                new ListModel<RecommenderEvaluationScoreMetricEnum>(METRICS));
        dropdown.setRequired(true);
        dropdown.setOutputMarkupId(true);

        dropdown.add(new AjaxFormComponentUpdatingBehavior("change")
        {
            private static final long serialVersionUID = -6744838136235652577L;

            protected void onUpdate(AjaxRequestTarget target)
            {
                DropDownEvent dropDownEvent = new DropDownEvent();
                dropDownEvent.setSelectedValue(dropdown.getModelObject());
                dropDownEvent.setTarget(target);

                send(getPage(), Broadcast.BREADTH, dropDownEvent); 
                
                Effects.hide(target, dropdown);
                Effects.show(target, dropdown);
                target.appendJavaScript("document.getElementById('" + link.getMarkupId()
                        + "').classList.remove('fa-chevron-circle-right');");
                target.appendJavaScript("document.getElementById('" + link.getMarkupId()
                        + "').classList.add('fa-chevron-circle-left');");
            }
        });

        add(dropdown);

        link = new AjaxLink<Void>(MID_METRIC_LINK)
        {            
            private static final long serialVersionUID = 1L;

            
            @Override
            public void onClick(AjaxRequestTarget target)
            {
                if (isDropdownVisible) {
                    Effects.hide(target, dropdown);
                    target.appendJavaScript("document.getElementById('" + link.getMarkupId()
                            + "').classList.remove('fa-chevron-circle-left');");
                    target.appendJavaScript("document.getElementById('" + link.getMarkupId()
                            + "').classList.add('fa-chevron-circle-right');");
                    isDropdownVisible = false;

                }
                else {

                    Effects.show(target, dropdown);
                    target.appendJavaScript("document.getElementById('" + link.getMarkupId()
                            + "').classList.remove('fa-chevron-circle-right');");
                    target.appendJavaScript("document.getElementById('" + link.getMarkupId()
                            + "').classList.add('fa-chevron-circle-left');");
                    isDropdownVisible = true;
                }
            }
        };

        link.setOutputMarkupId(true);
        add(link);
    }

    private static class Effects
    {
        private static void hide(AjaxRequestTarget target, Component component)
        {
            component.add(new DisplayNoneBehavior());
            String js = "$('#" + component.getMarkupId()
                    + "').animate({'width': '-=100'},  100); $('#"
                    + ((DropDownChoice) component).getMarkupId() + "').hide();";
            target.appendJavaScript(js);
        }

        private static void show(AjaxRequestTarget target, Component component)
        {
            component.add(new DisplayNoneBehavior());

            String js = "$('#" + component.getMarkupId()
                    + "').animate({'width': '+=100'},  100); $('#"
                    + ((DropDownChoice) component).getMarkupId() + "').show();";
            target.appendJavaScript(js);
        }
    }

    private static class DisplayNoneBehavior
        extends AttributeModifier
    {

        private static final long serialVersionUID = 1539674355578272254L;

        private DisplayNoneBehavior()
        {
            super("style", Model.of("display: none"));
        }

        @Override
        public boolean isTemporary(Component component)
        {
            return true;
        }
    }
}
