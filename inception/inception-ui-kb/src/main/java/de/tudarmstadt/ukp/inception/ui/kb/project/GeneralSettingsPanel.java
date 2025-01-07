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
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import org.wicketstuff.jquery.core.JQueryBehavior;
import org.wicketstuff.jquery.core.Options;
import org.wicketstuff.kendo.ui.KendoDataSource;
import org.wicketstuff.kendo.ui.form.combobox.ComboBox;
import org.wicketstuff.kendo.ui.form.multiselect.lazy.MultiSelect;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.kb.IriConstants;
import de.tudarmstadt.ukp.inception.kb.KnowledgeBaseService;
import de.tudarmstadt.ukp.inception.kb.config.KnowledgeBaseProperties;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class GeneralSettingsPanel
    extends Panel
{
    private static final long serialVersionUID = 8824114174867195670L;

    private static final List<String> LANGUAGE_CODES = asList("aa", "ab", "ae", "af", "ak", "am",
            "an", "ar", "as", "av", "ay", "az", "ba", "be", "bg", "bi", "bm", "bn", "bo", "br",
            "bs", "ca", "ce", "ch", "co", "cr", "cs", "cu", "cv", "cy", "da", "de", "dv", "dz",
            "ee", "el", "en", "eo", "es", "et", "eu", "fa", "ff", "fi", "fj", "fo", "fr", "fy",
            "ga", "gd", "gl", "gn", "gu", "gv", "ha", "he", "hi", "ho", "hr", "ht", "hu", "hy",
            "hz", "ia", "id", "ie", "ig", "ii", "ik", "io", "is", "it", "iu", "ja", "jv", "ka",
            "kg", "ki", "kj", "kk", "kl", "km", "kn", "ko", "kr", "ks", "ku", "kv", "kw", "ky",
            "la", "lb", "lg", "li", "ln", "lo", "lt", "lu", "lv", "mg", "mh", "mi", "mk", "ml",
            "mn", "mr", "ms", "mt", "my", "na", "nb", "nd", "ne", "ng", "nl", "nn", "no", "nr",
            "nv", "ny", "oc", "oj", "om", "or", "os", "pa", "pi", "pl", "ps", "pt", "qu", "rm",
            "rn", "ro", "ru", "rw", "sa", "sc", "sd", "se", "sg", "si", "sk", "sl", "sm", "sn",
            "so", "sq", "sr", "ss", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "ti", "tk",
            "tl", "tn", "to", "tr", "ts", "tt", "tw", "ty", "ug", "uk", "ur", "uz", "ve", "vi",
            "vo", "wa", "wo", "xh", "yi", "yo", "za", "zh", "zu");

    private final IModel<Project> projectModel;
    private final CompoundPropertyModel<KnowledgeBaseWrapper> kbModel;

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

        var additionalLanguages = new MultiSelect<String>("additionalLanguages")
        {
            @Override
            protected void onConfigure(KendoDataSource aDataSource)
            {
                // This ensures that we get the user input in getChoices
                aDataSource.set("serverFiltering", true);
            }

            @Override
            public void onConfigure(JQueryBehavior aBehavior)
            {
                super.onConfigure(aBehavior);
                aBehavior.setOption("filter", Options.asString("contains"));
                aBehavior.setOption("autoClose", false);
            }

            private static final long serialVersionUID = -7735027268669019571L;

            @Override
            protected List<String> getChoices(String aInput)
            {
                var choices = new ArrayList<>(getModelObject());
                choices.addAll(LANGUAGE_CODES);
                choices.sort(String::compareTo);
                choices.remove(aInput);
                if (isNotBlank(aInput)) {
                    choices.add(0, aInput);
                }
                return choices;
            }

            @Override
            public void convertInput()
            {
                var input = getInputAsArray();
                var list = new ArrayList<String>();
                if (input != null) {
                    list.addAll(asList(input));
                    list.removeIf(StringUtils::isBlank);
                }
                this.setConvertedInput(list);
            }
        };
        additionalLanguages.setModel(kbModel.bind("kb.additionalLanguages"));
        add(additionalLanguages);
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
        if (aModel.getObject() == null) {
            aModel.setObject("en");
        }

        var comboBox = new ComboBox<String>(id, aModel, LANGUAGE_CODES);
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
