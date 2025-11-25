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
package de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static java.util.Collections.swap;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.wicket.StyleAttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.PatternValidator;

import de.agilecoders.wicket.extensions.markup.html.bootstrap.form.ColorPickerTextField;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.rendering.coloring.ColoringRule;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.schema.api.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.inception.support.Coloring;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxSubmitLink;

/**
 * Can be added to a feature support traits editor to configure coloring rules.
 */
public class ColoringRulesConfigurationPanel
    extends GenericPanel<AnnotationLayer>
{
    private static final long serialVersionUID = -8294428032177255299L;

    private @SpringBean LayerSupportRegistry layerSupportRegistry;
    private @SpringBean AnnotationSchemaService schemaService;

    private final WebMarkupContainer coloringRulesContainer;
    private final IModel<List<ColoringRule>> coloringRules;

    public ColoringRulesConfigurationPanel(String aId, IModel<AnnotationLayer> aModel,
            IModel<List<ColoringRule>> aColoringRules)
    {
        super(aId, aModel);

        coloringRules = aColoringRules;

        queue(new Form<ColoringRule>("coloringRulesForm",
                CompoundPropertyModel.of(new ColoringRule())));

        coloringRulesContainer = new WebMarkupContainer("coloringRulesContainer");
        coloringRulesContainer.setOutputMarkupPlaceholderTag(true);
        queue(coloringRulesContainer);

        queue(new TextField<String>("pattern"));
        // We cannot make the color field a required one here because then we'd get a message
        // about color not being set when saving the entire feature details form!
        queue(new ColorPickerTextField("color").add(new PatternValidator("#[0-9a-fA-F]{6}")));
        queue(new LambdaAjaxSubmitLink<>("addColoringRule", this::addColoringRule));

        queue(createRulesList("coloringRules", coloringRules));
    }

    private ListView<ColoringRule> createRulesList(String aId,
            IModel<List<ColoringRule>> aKeyBindings)
    {
        return new ListView<ColoringRule>(aId, aKeyBindings)
        {
            private static final long serialVersionUID = 432136316377546825L;

            @Override
            protected void populateItem(ListItem<ColoringRule> aItem)
            {
                var coloringRule = aItem.getModelObject();

                var value = new Label("pattern", coloringRule.getPattern());

                value.add(new StyleAttributeModifier()
                {
                    private static final long serialVersionUID = 3627596292626670610L;

                    @Override
                    protected Map<String, String> update(Map<String, String> aStyles)
                    {
                        aStyles.put("color", Coloring.bgToFgColor(coloringRule.getColor()));
                        aStyles.put("background-color", coloringRule.getColor());
                        return aStyles;
                    }
                });

                aItem.add(value);
                aItem.add(new LambdaAjaxLink("moveUp", $ -> actionMoveRuleUp($, coloringRule))
                        .add(enabledWhen(() -> aItem.getIndex() > 0)));
                aItem.add(new LambdaAjaxLink("moveDown", $ -> actionMoveRuleDown($, coloringRule))
                        .add(enabledWhen(() -> aItem.getIndex() < getModelObject().size() - 1)));
                aItem.add(new LambdaAjaxLink("removeColoringRule",
                        _target -> removeColoringRule(_target, aItem.getModelObject())));
            }

            private void actionMoveRuleUp(AjaxRequestTarget aTarget, ColoringRule aItem)
            {
                var items = getModel().getObject();
                int index = items.indexOf(aItem);
                if (index > 0) {
                    swap(items, index - 1, index);
                }

                success("Coloring rule moved. Do not forget to save the layer details!");
                aTarget.addChildren(getPage(), IFeedback.class);
                aTarget.add(coloringRulesContainer);
            }

            private void actionMoveRuleDown(AjaxRequestTarget aTarget, ColoringRule aItem)
            {
                var items = getModel().getObject();
                int index = items.indexOf(aItem);
                if (index < items.size() - 1) {
                    swap(items, index, index + 1);
                }

                success("Coloring rule moved. Do not forget to save the layer details!");
                aTarget.addChildren(getPage(), IFeedback.class);
                aTarget.add(coloringRulesContainer);
            }
        };
    }

    private void addColoringRule(AjaxRequestTarget aTarget, Form<ColoringRule> aForm)
    {
        ColoringRule coloringRule = aForm.getModelObject();

        if (isBlank(coloringRule.getColor())) {
            error("Color is required");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        if (isBlank(coloringRule.getPattern())) {
            error("Pattern is required");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        try {
            Pattern.compile(coloringRule.getPattern());
        }
        catch (PatternSyntaxException e) {
            error("Pattern is not a valid regular expression: " + e.getMessage());
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        coloringRules.getObject().add(coloringRule);

        aForm.setModelObject(new ColoringRule());

        success("Coloring rule added. Do not forget to save the layer details!");
        aTarget.addChildren(getPage(), IFeedback.class);
        aTarget.add(coloringRulesContainer);
    }

    private void removeColoringRule(AjaxRequestTarget aTarget, ColoringRule aBinding)
    {
        coloringRules.getObject().remove(aBinding);
        aTarget.add(coloringRulesContainer);
    }
}
