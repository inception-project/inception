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
package de.tudarmstadt.ukp.inception.security.client.auth.header;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhenNot;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.PatternValidator;
import org.apache.wicket.validation.validator.StringValidator;

import de.tudarmstadt.ukp.inception.security.client.auth.AuthenticationTraitsEditor;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

public class HeaderAuthenticationTraitsEditor
    extends AuthenticationTraitsEditor<HeaderAuthenticationTraits>
{
    private static final long serialVersionUID = -2171643131419507935L;

    private static final String BASE64_PATTERN = "(?:[A-Za-z0-9\\/]{4})*(?:[A-Za-z0-9+\\/]{2}==|[A-Za-z0-9+\\/]{3}=)?";
    private static final String VALUE_PATTERN = "[a-zA-Z0-9-_]+";

    private IModel<Boolean> editMode;
    private transient String newValue;

    public HeaderAuthenticationTraitsEditor(String aId, IModel<HeaderAuthenticationTraits> aModel)
    {
        super(aId, CompoundPropertyModel.of(aModel));

        setOutputMarkupId(true);

        editMode = Model.of(getModelObject().getHeader() == null);

        queue(new Label("status", LoadableDetachableModel.of(this::getStatus))
                .add(visibleWhenNot(editMode)));
        queue(new LambdaAjaxLink("edit", this::actionToggleEditMode) //
                .add(visibleWhenNot(editMode)));
        queue(new TextField<String>("header") //
                .setRequired(true) //
                .add(new PatternValidator("^[a-zA-Z0-9-_]{1,64}$")
                {
                    private static final long serialVersionUID = 3027357026820534676L;

                    @Override
                    protected IValidationError decorate(IValidationError aError,
                            IValidatable<String> aValidatable)
                    {
                        return new ValidationError("Value must consist only of small or capital "
                                + "letters from A-Z, numbers, dashes and underscores and be no "
                                + "longer than 64 symbols.");
                    }

                })//
                .add(visibleWhen(editMode)));
        queue(new TextField<String>("value", PropertyModel.of(this, "newValue")) //
                .setRequired(true) //
                .add(new StringValidator(1, 255)) //
                .add(new PatternValidator("^(" + BASE64_PATTERN + ")|(" + VALUE_PATTERN + ")$")
                {
                    private static final long serialVersionUID = 8749916056313473085L;

                    @Override
                    protected IValidationError decorate(IValidationError aError,
                            IValidatable<String> aValidatable)
                    {
                        return new ValidationError("Value must consist only of small or capital "
                                + "letters from A-Z, numbers, dashes and underscores or be valid "
                                + "base64-encoded value and be no longer than 255 symbols.");
                    }
                })//
                .add(visibleWhen(editMode)));
    }

    private String getStatus()
    {
        var sb = new StringBuilder();
        sb.append("Header is ");
        sb.append(getModel().map(HeaderAuthenticationTraits::getHeader) //
                .map(username -> "[" + username + "]")//
                .orElse("not set").getObject());
        sb.append("; value is ");
        sb.append(getModel().map(HeaderAuthenticationTraits::getValue) //
                .map($ -> "set")//
                .orElse("not set").getObject());
        return sb.toString();
    }

    private void actionToggleEditMode(AjaxRequestTarget aTarget)
    {
        editMode.setObject(!editMode.getObject());
        aTarget.add(this);
    }

    @Override
    public void setEditMode(boolean aMode)
    {
        editMode.setObject(aMode);
        newValue = null;
    }

    @Override
    public void commit()
    {
        if (newValue != null) {
            getModel().getObject().setValue(newValue);
        }

        setEditMode(false);
    }

    @Override
    protected void onModelChanged()
    {
        setEditMode(false);
    }
}
