package de.tudarmstadt.ukp.clarin.webanno.brat.page.monitoring;

import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

public class DocumentColumnMetaData extends AbstractColumn<List<String>> {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    int columnNumber;

    public DocumentColumnMetaData(final UserAnnotatedDocumentProvider prov,final int colNumber) {
        super(new AbstractReadOnlyModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                    return prov.getColNames().get(colNumber);

            }
        });
        columnNumber = colNumber;
    }

    public void populateItem(Item<ICellPopulator<List<String>>> cellItem,
            String componentId, IModel<List<String>> rowModel) {
        cellItem.add(new Label(componentId,rowModel.getObject().get(columnNumber)));
    }

}