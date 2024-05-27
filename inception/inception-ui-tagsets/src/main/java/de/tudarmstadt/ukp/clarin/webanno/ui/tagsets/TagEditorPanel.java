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
package de.tudarmstadt.ukp.clarin.webanno.ui.tagsets;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;

import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.inception.schema.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaPanel;

public class TagEditorPanel
    extends LambdaPanel
{
    private static final long serialVersionUID = -3356173821217898824L;

    private @SpringBean AnnotationSchemaService annotationSchemaService;

    private IModel<TagSet> selectedTagSet;
    private IModel<Tag> selectedTag;

    public TagEditorPanel(String aId, IModel<TagSet> aTagSet, IModel<Tag> aTag)
    {
        super(aId, aTag);

        setOutputMarkupId(true);
        setOutputMarkupPlaceholderTag(true);

        selectedTagSet = aTagSet;
        selectedTag = aTag;

        Form<Tag> form = new Form<>("form", CompoundPropertyModel.of(aTag));
        add(form);

        form.add(new TextField<String>("name") //
                .add(new TagExistsValidator()) //
                .setRequired(true));
        form.add(new TextArea<String>("description"));

        form.add(new LambdaAjaxButton<>("save", this::actionSave));
        form.add(new LambdaAjaxLink("delete", this::actionDelete)
                .onConfigure(_this -> _this.setVisible(form.getModelObject().getId() != null)));
        form.add(new LambdaAjaxLink("cancel", this::actionCancel));
    }

    private void actionSave(AjaxRequestTarget aTarget, Form<Tag> aForm)
    {
        selectedTag.getObject().setTagSet(selectedTagSet.getObject());
        annotationSchemaService.createTag(selectedTag.getObject());

        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }

    private void actionDelete(AjaxRequestTarget aTarget)
    {
        annotationSchemaService.removeTag(selectedTag.getObject());
        actionCancel(aTarget);
    }

    private void actionCancel(AjaxRequestTarget aTarget)
    {
        selectedTag.setObject(null);

        // Reload whole page because master panel also needs to be reloaded.
        aTarget.add(getPage());
    }

    private class TagExistsValidator
        implements IValidator<String>
    {
        private static final long serialVersionUID = 6697292531559511021L;

        @Override
        public void validate(IValidatable<String> aValidatable)
        {
            String newName = aValidatable.getValue();
            String oldName = aValidatable.getModel().getObject();
            if (!StringUtils.equals(newName, oldName) && isNotBlank(newName)
                    && annotationSchemaService.existsTag(newName, selectedTagSet.getObject())) {
                aValidatable.error(new ValidationError(
                        "Another tag with the same name exists. Please try a different name"));
            }
        }
    }
}
