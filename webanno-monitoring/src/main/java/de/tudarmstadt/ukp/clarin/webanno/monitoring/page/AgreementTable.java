/*******************************************************************************
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.monitoring.page;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
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

import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AgreementUtils;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.PairwiseAnnotationResult;
import de.tudarmstadt.ukp.clarin.webanno.brat.curation.AgreementUtils.AgreementResult;
import de.tudarmstadt.ukp.clarin.webanno.support.AJAXDownload;
import de.tudarmstadt.ukp.clarin.webanno.support.DefaultRefreshingView;

public class AgreementTable
    extends Panel
{
    private static final long serialVersionUID = 571396822546125376L;
    
    private RefreshingView<String> rows;

    public AgreementTable(String aId)
    {
        super(aId);
    }

    public AgreementTable(String aId, IModel<PairwiseAnnotationResult> aModel)
    {
        super(aId, aModel);

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

                            String label;
                            if (result.getStudy().getItemCount() == 0) {
                                label = "no data";
                            }
                            else {
                                label = String.format("%.2f", result.getAgreement());
                            }
                            
                            Label l = new Label("label", Model.of(label)); 
                            cell.add(l);
                            l.add(makeDownloadBehavior(aRowItem.getModelObject(), aCellItem.getModelObject()));
                        }
                        // Lower diagonal
                        else if (aCellItem.getIndex() < aRowItem.getIndex()) {
                            AgreementResult result = AgreementTable.this.getModelObject().getStudy(
                                    aRowItem.getModelObject(), aCellItem.getModelObject());
                            
                            String label = String.format("%d/%d", result.getCompleteSetCount(),
                                    result.getTotalSetCount());                            

                            String toolTip = String.format(
                                    "Details about annotations excluded from agreement calculation:%n" +
                                    "- Incomplete (missing): %d%n" +
                                    "- Incomplete (not labeled): %d%n" +                                  
                                    "- Plurality: %d",                                  
                                    result.getIncompleteSetsByPosition().size(),
                                    result.getIncompleteSetsByLabel().size(),
                                    result.getPluralitySets().size());
                            
                            Label l = new Label("label", Model.of(label)); 
                            l.add(new AttributeModifier("title", toolTip));
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
        return new AjaxEventBehavior("onclick")
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
                                AgreementResult result = AgreementTable.this.getModelObject()
                                        .getStudy(aKey1, aKey2);
                                
                                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                                AgreementUtils.dumpAgreementStudy(new PrintStream(buf), result);
                                return new ByteArrayInputStream(buf.toByteArray());
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
                download.initiate(aTarget, "agreement.txt");
            }
        };      
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
