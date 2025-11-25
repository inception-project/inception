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
package de.tudarmstadt.ukp.inception.schema.api.feature;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.CURATOR;
import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.visibleWhen;
import static java.util.Arrays.asList;
import static org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog.CONTENT_ID;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapCheckBoxMultipleChoice;
import de.tudarmstadt.ukp.inception.bootstrap.BootstrapModalDialog;
import de.tudarmstadt.ukp.inception.support.lambda.HtmlElementEvents;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxFormComponentUpdatingBehavior;

public class RetainSuggestionInfoPanel
    extends GenericPanel<RecommendableFeatureTrait>
{
    private static final long serialVersionUID = 942413732195454015L;

    private final CheckBox retainSuggestionInfo;
    private final ModalDialog dialog;
    private final boolean originalRetainSuggestionInfo;
    private final IModel<AnnotationFeature> feature;

    public RetainSuggestionInfoPanel(String aId, IModel<AnnotationFeature> aFeature,
            IModel<RecommendableFeatureTrait> aTraits)
    {
        super(aId, aTraits);
        setOutputMarkupId(true);

        feature = aFeature;

        dialog = new BootstrapModalDialog("dialog");
        dialog.trapFocus();
        add(dialog);

        retainSuggestionInfo = new CheckBox("retainSuggestionInfo",
                PropertyModel.of(aTraits, "retainSuggestionInfo"));
        retainSuggestionInfo.setOutputMarkupId(true);
        retainSuggestionInfo.add(new LambdaAjaxFormComponentUpdatingBehavior(
                HtmlElementEvents.CHANGE_EVENT, this::actionToggleRetainSuggestionInfo));
        queue(retainSuggestionInfo);

        var rolesSeeingSuggestionInfo = new BootstrapCheckBoxMultipleChoice<PermissionLevel>(
                "rolesSeeingSuggestionInfo");
        rolesSeeingSuggestionInfo.setOutputMarkupPlaceholderTag(true);
        rolesSeeingSuggestionInfo.setModel(PropertyModel.of(aTraits, "rolesSeeingSuggestionInfo"));
        rolesSeeingSuggestionInfo.setChoices(asList(ANNOTATOR, CURATOR));
        rolesSeeingSuggestionInfo
                .setChoiceRenderer(new EnumChoiceRenderer<>(rolesSeeingSuggestionInfo));
        rolesSeeingSuggestionInfo.add(visibleWhen(retainSuggestionInfo.getModel()));
        queue(rolesSeeingSuggestionInfo);

        originalRetainSuggestionInfo = retainSuggestionInfo.getModelObject();
    }

    private void actionToggleRetainSuggestionInfo(AjaxRequestTarget aTarget)
    {
        if (originalRetainSuggestionInfo == true
                && retainSuggestionInfo.getModelObject() == false) {
            var content = new DisableRetainSuggestionInfoConfirmationDialogContentPanel(CONTENT_ID);
            content.setExpectedResponseModel(feature.map(AnnotationFeature::getUiName));
            content.setConfirmAction(this::actionDisableRetainSuggestionInfo);
            content.setCancelAction(this::actionCancelToggle);
            dialog.open(content, aTarget);
            return;
        }

        aTarget.add(this);
    }

    private void actionDisableRetainSuggestionInfo(AjaxRequestTarget aTarget)
    {
        retainSuggestionInfo.setModelObject(false);
        aTarget.add(this);
    }

    private void actionCancelToggle(AjaxRequestTarget aTarget)
    {
        retainSuggestionInfo.setModelObject(true);
        aTarget.add(this);
    }
}
