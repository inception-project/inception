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
package de.tudarmstadt.ukp.clarin.webanno.ui.project.detail;

import static java.util.Comparator.comparing;
import static java.util.Locale.ROOT;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.GenericPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.inception.project.api.footprint.Footprint;
import de.tudarmstadt.ukp.inception.project.api.footprint.FootprintProviderRegistry;
import de.tudarmstadt.ukp.inception.support.lambda.LambdaStyleAttributeModifier;

public class FootprintPanel
    extends GenericPanel<Project>
{
    private static final long serialVersionUID = -1627121364400427020L;

    private @SpringBean FootprintProviderRegistry footprintProviderRegistry;

    public FootprintPanel(String aId, IModel<Project> aModel)
    {
        super(aId, aModel);

        var footPrints = footprintProviderRegistry.getExtensions(aModel.getObject()).stream() //
                .flatMap(provider -> provider.getFootprint(aModel.getObject()).stream()) //
                .filter(footprint -> footprint.size() > 0) //
                .sorted(comparing(Footprint::category)) //
                .toList();

        var total = footPrints.stream() //
                .mapToLong(Footprint::size) //
                .sum();

        add(new Label("total", byteCountToDisplaySize(total)) //
                .add(AttributeModifier.replace("title", total + " bytes")));

        add(new ListView<>("footprint", footPrints)
        {
            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<Footprint> aItem)
            {
                aItem.add(new LambdaStyleAttributeModifier(styles -> {
                    styles.put("width", String.format(ROOT, "%.3f",
                            aItem.getModelObject().size() * 100.0 / total) + "%");
                    styles.put("background-color", aItem.getModelObject().color());
                    return styles;
                }));

                aItem.add(AttributeModifier.replace("title",
                        byteCountToDisplaySize(aItem.getModelObject().size())));
                aItem.add(new Label("category", aItem.getModelObject().category()));
            }
        });
    }
}
