/*
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
package de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.curation.agreement.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.support.AJAXDownload;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultRefreshingView;
import de.tudarmstadt.ukp.clarin.webanno.support.DescriptionTooltipBehavior;
import de.tudarmstadt.ukp.clarin.webanno.ui.monitoring.page.AgreementPage.AgreementFormModel;

public class AgreementTable
    extends Panel
{
    private final static Logger LOG = LoggerFactory.getLogger(AgreementTable.class);
    
    private static final long serialVersionUID = 571396822546125376L;
    
    private RefreshingView<String> rows;

    private IModel<AgreementFormModel> settings;
    
    public AgreementTable(String aId)
    {
        super(aId);
    }

    public AgreementTable(String aId, IModel<AgreementFormModel> aSettings,
            IModel<PairwiseAnnotationResult> aModel)
    {
        super(aId, aModel);

        settings = aSettings;
        
        setOutputMarkupId(true);
        
        // This model makes sure we add a "null" dummy rater which accounts for the header columns
        // of the table.
        final IModel<List<String>> ratersAdapter = new AbstractReadOnlyModel<List<String>>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public List<String> getObject()
            {
                List<String> raters = new ArrayList<>();
                if (getModelObject() != null) {
                    raters.add(null);
                    raters.addAll(getModelObject().getRaters());
                }
                return raters;
            }
        };

        rows = new DefaultRefreshingView<String>("rows", ratersAdapter)
        {
            private static final long serialVersionUID = 1L;
            
            @Override
            protected void populateItem(final Item<String> aRowItem)
            {
                // Render regular row
                aRowItem.add(new DefaultRefreshingView<String>("cells", ratersAdapter)
                {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                    protected void populateItem(Item<String> aCellItem)
                    {
                        aCellItem.setRenderBodyOnly(true);
                        
                        Fragment cell; 
                        // Content cell
                        if (aRowItem.getIndex() == 0 || aCellItem.getIndex() == 0) {
                            cell = new Fragment("cell", "th", AgreementTable.this);
                        }
                        // Header cell
                        else {
                            cell = new Fragment("cell", "td", AgreementTable.this);
                        }
                        
                        // Top-left cell
                        if (aRowItem.getIndex() == 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label", Model.of("")));
                        }
                        // Raters header horizontally
                        else if (aRowItem.getIndex() == 0 && aCellItem.getIndex() != 0) {
                            cell.add(new Label("label", Model.of(aCellItem.getModelObject())));
                        }
                        // Raters header vertically
                        else if (aRowItem.getIndex() != 0 && aCellItem.getIndex() == 0) {
                            cell.add(new Label("label", Model.of(aRowItem.getModelObject())));
                        }
                        // Upper diagonal
                        else if (aCellItem.getIndex() > aRowItem.getIndex()) {
                            AgreementResult result = AgreementTable.this.getModelObject().getStudy(
                                    aRowItem.getModelObject(), aCellItem.getModelObject());

                            boolean noDataRater0 = result.isAllNull(result.getCasGroupIds().get(0));
                            boolean noDataRater1 = result.isAllNull(result.getCasGroupIds().get(1));
                            int incPos = result.getIncompleteSetsByPosition().size();
                            int incLabel = result.getIncompleteSetsByLabel().size();
                            
                            String label;
                            if (result.getStudy().getItemCount() == 0) {
                                label = "no positions";
                            }
                            else if (noDataRater0 && noDataRater1) {
                                label = "no labels";
                            }
                            else if (noDataRater0) {
                                label = "no labels from " + result.getCasGroupIds().get(0);
                            }
                            else if (noDataRater1) {
                                label = "no labels from " + result.getCasGroupIds().get(1);
                            }
                            else if (incPos == result.getRelevantSetCount()) {
                                label = "positions disjunct";
                            }
                            else if (incLabel == result.getRelevantSetCount()) {
                                label = "labels disjunct";
                            }
                            else if ((incLabel + incPos) == result.getRelevantSetCount()) {
                                label = "labels/positions disjunct";
                            }
                            else {
                                label = String.format("%.2f", result.getAgreement());
                            }

                            String tooltipTitle = result.getCasGroupIds().get(0) +
                                '/' +
                                result.getCasGroupIds().get(1);

                            String tooltipContent = "Positions annotated:\n" +
                                String.format("- %s: %d/%d%n",
                                    result.getCasGroupIds().get(0),
                                    result.getNonNullCount(result.getCasGroupIds().get(0)),
                                    result.getStudy().getItemCount()) +
                                String.format("- %s: %d/%d%n",
                                    result.getCasGroupIds().get(1),
                                    result.getNonNullCount(result.getCasGroupIds().get(1)),
                                    result.getStudy().getItemCount()) +
                                String.format("Distinct labels used: %d%n",
                                    result.getStudy().getCategoryCount());

                            Label l = new Label("label", Model.of(label)); 
                            cell.add(l);
                            l.add(makeDownloadBehavior(aRowItem.getModelObject(),
                                    aCellItem.getModelObject()));
                            DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(
                                tooltipTitle, tooltipContent);
                            tooltip.setOption("position", (Object) null);
                            l.add(tooltip);
                            l.add(new AttributeAppender("style", "cursor: pointer", ";"));
                        }
                        // Lower diagonal
                        else if (aCellItem.getIndex() < aRowItem.getIndex()) {
                            AgreementResult result = AgreementTable.this.getModelObject().getStudy(
                                    aRowItem.getModelObject(), aCellItem.getModelObject());
                            
                            String label = String.format("%d/%d", result.getCompleteSetCount(),
                                    result.getRelevantSetCount());                            

                            String tooltipTitle = "Details about annotations excluded from "
                                    + "agreement calculation";
                            
                            StringBuilder tooltipContent = new StringBuilder();
                            if (result.isExcludeIncomplete()) {
                                tooltipContent.append(String.format("- Incomplete (missing): %d%n",
                                        result.getIncompleteSetsByPosition().size()));
                                tooltipContent.append(String.format(
                                        "- Incomplete (not labeled): %d%n", result
                                                .getIncompleteSetsByLabel().size()));
                            }
                            tooltipContent.append(String.format("- Plurality: %d", result
                                    .getPluralitySets().size()));
                            
                            Label l = new Label("label", Model.of(label)); 
                            DescriptionTooltipBehavior tooltip = new DescriptionTooltipBehavior(
                                tooltipTitle, tooltipContent.toString());
                            tooltip.setOption("position", (Object) null);
                            l.add(tooltip);
                            l.add(new AttributeAppender("style", "cursor: help", ";"));
                            cell.add(l);
                        }
                        // Rest
                        else {
                            cell.add(new Label("label", Model.of("-")));
                        }
                        
                        aCellItem.add(cell);
                    }
                });
                // Odd/even coloring is reversed here to account for the header row at index 0
                aRowItem.add(new AttributeAppender("class", (aRowItem.getIndex() % 2 == 0) ? "odd"
                        : "even"));
            }
        };
        add(rows);
    }

    private Behavior makeDownloadBehavior(final String aKey1, final String aKey2)
    {
        return new AjaxEventBehavior("click")
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget aTarget)
            {
                AJAXDownload download = new AJAXDownload() {
                    private static final long serialVersionUID = 1L;
                    
                    @Override
                    protected IResourceStream getResourceStream()
                    {
                        return new AbstractResourceStream() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public InputStream getInputStream()
                                throws ResourceStreamNotFoundException
                            {
                                try {
                                    AgreementResult result = AgreementTable.this.getModelObject()
                                            .getStudy(aKey1, aKey2);
                                    
                                    switch (settings.getObject().exportFormat) {
                                    case CSV:
                                        return AgreementUtils.generateCsvReport(result);
                                    case DEBUG:
                                        return generateDebugReport(result);
                                    default:
                                        throw new IllegalStateException("Unknown export format ["
                                                + settings.getObject().exportFormat + "]");
                                    }
                                }
                                catch (Exception e) {
                                    // FIXME Is there some better error handling here?
                                    LOG.error("Unable to generate agreement report", e);
                                    throw new ResourceStreamNotFoundException(e);
                                }
                            }

                            @Override
                            public void close()
                                throws IOException
                            {
                                // Nothing to do
                            }
                        };
                    }
                };
                getComponent().add(download);
                download.initiate(aTarget,
                        "agreement" + settings.getObject().exportFormat.getExtension());
            }
        };      
    }

    private InputStream generateDebugReport(AgreementResult aResult)
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        AgreementUtils.dumpAgreementStudy(new PrintStream(buf), aResult);
        return new ByteArrayInputStream(buf.toByteArray());
    }
    
    @Override
    protected void onComponentTag(ComponentTag tag)
    {
        checkComponentTag(tag, "table");
        super.onComponentTag(tag);
    }

    public PairwiseAnnotationResult getModelObject()
    {
        return (PairwiseAnnotationResult) getDefaultModelObject();
    }

    public void setModelObject(PairwiseAnnotationResult aAgreements2)
    {
        setDefaultModelObject(aAgreements2);
    }
}
