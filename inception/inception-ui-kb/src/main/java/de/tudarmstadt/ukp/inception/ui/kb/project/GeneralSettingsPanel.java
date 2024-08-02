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
package de.tudarmstadt.ukp.inception.ui.kb.project;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.wicketstuff.kendo.ui.form.combobox.ComboBox;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class GeneralSettingsPanel
    extends Panel
{
    private static final long serialVersionUID = 8824114174867195670L;

    private final IModel<Project> projectModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;
    private final List<String> languages = asList("en", "de");

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

    private TextField<String> nameField(String id, String property)
    {
        var nameField = new RequiredTextField<String>(id, kbModel.bind(property));
        nameField.add(new KnowledgeBaseNameValidator());
        return nameField;
    }

    private ComboBox<String> languageComboBox(String id, IModel<String> aModel)
    {
        // Only set kbModel object if it has not been initialized yet
        if (aModel.getObject() == null && !languages.isEmpty()) {
            aModel.setObject(languages.get(0));
        }

        var comboBox = new ComboBox<String>(id, aModel, languages);
        comboBox.setOutputMarkupId(true);
        comboBox.setRequired(true);
        // Do nothing just update the kbModel values
        comboBox.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        return comboBox;
    }

    private CheckBox createCheckbox(String aId, String aProperty)
    {
        var cb = new CheckBox(aId, kbModel.bind(aProperty));
        cb.setOutputMarkupId(true);
        return cb;
    }

    private ComboBox<String> basePrefixField(String aId, String aProperty)
    {
        // Add textfield and label for basePrefix
        var basePrefix = new ComboBox<String>(aId, kbModel.bind(aProperty),
                asList(IriConstants.INCEPTION_NAMESPACE));
        basePrefix.add(new LambdaAjaxFormComponentUpdatingBehavior("change"));
        basePrefix.setConvertEmptyInputStringToNull(false);
        basePrefix.setOutputMarkupId(true);
        return basePrefix;
    }

    private class KnowledgeBaseNameValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = 4125256951093164889L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            var kbName = aValidatable.getValue();
            if (kbService.knowledgeBaseExists(projectModel.getObject(), kbName)) {
                var message = String.format(
                        "There already exists a knowledge base in the project with name: [%s]!",
                        kbName);
                aValidatable.error(new ValidationError(message));
            }
        }
    }
}
