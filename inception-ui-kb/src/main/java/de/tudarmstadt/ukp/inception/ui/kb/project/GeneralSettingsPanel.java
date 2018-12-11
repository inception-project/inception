/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import com.googlecode.wicket.kendo.ui.form.combobox.ComboBox;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaModelAdapter;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;

public class GeneralSettingsPanel extends Panel
{
    private final IModel<Project> projectModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;
    private final List<String> languages = Arrays.asList("en", "de");

    private @SpringBean KnowledgeBaseService kbService;
    private @SpringBean KnowledgeBaseProperties kbproperties;

    public GeneralSettingsPanel(String id, IModel<Project> aProjectModel,
        CompoundPropertyModel<KnowledgeBaseWrapper> aModel)
    {
        super(id);
        setOutputMarkupId(true);

        projectModel = aProjectModel;
        kbModel = aModel;

        add(nameField("name", "kb.name"));
        add(languageComboBox("language", kbModel.bind("kb.defaultLanguage")));
        add(basePrefixField("basePrefix", "kb.basePrefix"));
        add(createCheckbox("enabled", "kb.enabled"));
    }

    private TextField<String> nameField(String id, String property) {
        TextField<String> nameField = new RequiredTextField<>(id, kbModel.bind(property));
        nameField.add(knowledgeBaseNameValidator());
        return nameField;
    }

    private IValidator<String> knowledgeBaseNameValidator()
    {
        return (validatable -> {
            String kbName = validatable.getValue();
            if (kbService.knowledgeBaseExists(projectModel.getObject(), kbName)) {
                String message = String.format(
                    "There already exists a knowledge base in the project with name: [%s]!",
                    kbName
                );
                validatable.error(new ValidationError(message));
            }
        });
    }

    private ComboBox<String> languageComboBox(String id, IModel<String> aModel)
    {
        // Only set kbModel object if it has not been initialized yet
        if (aModel.getObject() == null) {
            aModel.setObject(languages.get(0));
        }

        IModel<String> adapter = new LambdaModelAdapter<String>(aModel::getObject,
            aModel::setObject);

        ComboBox<String> comboBox = new ComboBox<String>(id, adapter, languages);
        comboBox.setOutputMarkupId(true);
        comboBox.setRequired(true);
        comboBox.add(new LambdaAjaxFormComponentUpdatingBehavior("change", t -> {
            // Do nothing just update the kbModel values
        }));
        return comboBox;
    }

    private CheckBox createCheckbox(String aId, String aProperty) {
        return new CheckBox(aId, kbModel.bind(aProperty));
    }

    private ComboBox<String> basePrefixField(String aId, String aProperty) {
        // Add textfield and label for basePrefix
        ComboBox<String> basePrefix = new ComboBox<String>(aId,
            kbModel.bind(aProperty), Arrays.asList(IriConstants.INCEPTION_NAMESPACE));
        basePrefix.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        basePrefix.setConvertEmptyInputStringToNull(false);
        basePrefix.setOutputMarkupId(true);
        return basePrefix;
    }

}
