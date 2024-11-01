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
package de.tudarmstadt.ukp.inception.guidelines;

import static org.apache.wicket.markup.html.link.PopupSettings.RESIZABLE;
import static org.apache.wicket.markup.html.link.PopupSettings.SCROLLBARS;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.PopupSettings;
import org.apache.wicket.markup.html.link.ResourceLink;
import org.apache.wicket.markup.html.list.AbstractItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.resource.FileResourceStream;

import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;

/**
 * Modal window to display annotation guidelines
 */
public class GuidelinesDialogContent
    extends Panel
{
    private static final long serialVersionUID = -2102136855109258306L;

    private @SpringBean GuidelinesService guidelinesService;

    private final LambdaAjaxLink cancelButton;

    public GuidelinesDialogContent(String aId, final IModel<AnnotatorState> aModel)
    {
        super(aId);

        // Overall progress by Projects
        var guidelineRepeater = new RepeatingView("guidelineRepeater");
        add(guidelineRepeater);

        for (var guidelineFileName : guidelinesService
                .listGuidelines(aModel.getObject().getProject())) {
            var item = new AbstractItem(guidelineRepeater.newChildId());

            guidelineRepeater.add(item);

            // Add a pop-up window link to display annotation guidelines
            var popupSettings = new PopupSettings(RESIZABLE | SCROLLBARS).setHeight(500)
                    .setWidth(700);

            var stream = new FileResourceStream(guidelinesService
                    .getGuideline(aModel.getObject().getProject(), guidelineFileName));
            var resource = new ResourceStreamResource(stream);
            var rlink = new ResourceLink<Void>("guideine", resource);
            rlink.setPopupSettings(popupSettings);
            item.queue(rlink);

            item.queue(new Label("guidelineName", guidelineFileName));
        }

        cancelButton = new LambdaAjaxLink("cancel", this::actionCloseDialog);
        cancelButton.setOutputMarkupId(true);
        queue(cancelButton);
        queue(new LambdaAjaxLink("closeDialog", this::actionCloseDialog));
    }

    protected void actionCloseDialog(AjaxRequestTarget aTarget)
    {
        findParent(ModalDialog.class).close(aTarget);
    }

    public void onShow(AjaxRequestTarget aTarget)
    {
        aTarget.focusComponent(cancelButton);
    }
}
