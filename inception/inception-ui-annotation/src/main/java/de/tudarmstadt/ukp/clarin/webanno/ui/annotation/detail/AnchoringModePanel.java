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
package de.tudarmstadt.ukp.clarin.webanno.ui.annotation.detail;

import static de.tudarmstadt.ukp.inception.support.lambda.LambdaBehavior.enabledWhen;
import static java.util.Arrays.asList;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.model.AnchoringMode;
import de.tudarmstadt.ukp.inception.support.lambda.AjaxPayloadCallback;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaAjaxLink;
import de.tudarmstadt.ukp.inception.support.wicket.SymbolLabel;
import de.tudarmstadt.ukp.inception.support.wicket.WicketExceptionUtil;

public class AnchoringModePanel
    extends GenericPanel<AnchoringMode>
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long serialVersionUID = 5112760373769659109L;

    private static final String MID_LABEL = "label";
    private static final String MID_MODE = "mode";
    private static final String MID_MODE_LINK = "modeLink";

    private AjaxPayloadCallback<AnchoringMode> onApplied;

    public AnchoringModePanel(String aId)
    {
        this(aId, null, Model.ofList(new ArrayList<>(asList(AnchoringMode.values()))));
    }

    public AnchoringModePanel(String aId, IModel<AnchoringMode> aModel)
    {
        this(aId, aModel, Model.ofList(new ArrayList<>(asList(AnchoringMode.values()))));
    }

    public AnchoringModePanel(String aId, IModel<AnchoringMode> aModel,
            IModel<? extends List<AnchoringMode>> aAllowedModes)
    {
        super(aId, aModel);
        setOutputMarkupId(true);

        var allModes = Model.ofList(new ArrayList<>(asList(AnchoringMode.values())));

        var listView = new ListView<>(MID_MODE, allModes)
        {
            private static final long serialVersionUID = -2292408105823066466L;

            @Override
            protected void populateItem(ListItem<AnchoringMode> aItem)
            {
                var mode = aItem.getModelObject();

                var link = new LambdaAjaxLink(MID_MODE_LINK, (_target -> applyMode(_target, mode)));
                link.add(new SymbolLabel(MID_LABEL, aItem.getModel()));
                link.add(new AttributeAppender("class",
                        () -> AnchoringModePanel.this.getModelObject() == mode ? "active" : "",
                        " "));
                link.add(AttributeModifier.replace("title",
                        new ResourceModel(mode.getClass().getSimpleName() + "." + mode.name())));
                link.add(enabledWhen(aAllowedModes.map(modes -> modes.contains(mode))));

                aItem.add(link);
            }
        };

        queue(listView);
    }

    private void applyMode(AjaxRequestTarget aTarget, AnchoringMode aMode)
    {
        setModelObject(aMode);
        aTarget.add(this);

        if (onApplied != null) {
            try {
                onApplied.accept(aTarget, aMode);
            }
            catch (Exception e) {
                WicketExceptionUtil.handleException(LOG, getPage(), aTarget, e);
            }
        }
    }

    public AnchoringModePanel onApplied(AjaxPayloadCallback<AnchoringMode> aCallback)
    {
        onApplied = aCallback;
        return this;
    }
}
