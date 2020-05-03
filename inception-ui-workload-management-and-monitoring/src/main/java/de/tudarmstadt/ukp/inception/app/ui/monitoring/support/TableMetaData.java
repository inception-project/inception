/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
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

package de.tudarmstadt.ukp.inception.app.ui.monitoring.support;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import java.util.List;

public class TableMetaData extends AbstractColumn
{
    private int column;
    private List<SourceDocument> documentList;

    private int loop; //Special purpose, get right column on each call, could be made better I guess

    private @SpringBean DocumentService documentService;

    private static final long serialVersionUID = 1L;

    public TableMetaData(final DataProvider provider,
                         final int aColumn, List<SourceDocument> documentList)
    {
        super(new AbstractReadOnlyModel<String>()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject()
            {
                return provider.getTableHeaders().get(aColumn);

            }
        });
        this.column = aColumn;
        this.documentList = documentList;
        this.loop = 0;
    }


    @Override
    public void populateItem(Item aItem, String aID, IModel iModel)
    {

        //Populate items of the table, one column, check which column is called
        //to enter correct data
        if (column == 0)
        {
            aItem.add(new Label(aID, documentList.get(loop).getName()));

        }
        else if (column == 1)
        {
            //TODO get data for: In pogress
            aItem.add(new Label(aID, "-"));

        }
        else
        {
            //TODO get data for: Finished
            aItem.add(new Label(aID, "-"));
        }

        //For next column use correct data
        loop++;
    }
}
