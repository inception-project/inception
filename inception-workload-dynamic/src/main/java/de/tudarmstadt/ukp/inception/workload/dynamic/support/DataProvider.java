/*
 * Copyright 2020
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

package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class DataProvider extends SortableDataProvider
    <SourceDocument, String> implements IFilterStateLocator<Filter>, Serializable
{


    private static final long serialVersionUID = 4125678936105494485L;
    private final List<String> headers;
    private final List<SourceDocument> data;
    private final IModel<List<SourceDocument>> model;
    private final List<AnnotationDocument> allAnnotationDocuments;
    private List<SourceDocument> shownDocuments;
    private Filter filter;

    private int defaultAnnotations;

    private String input;
    private String type;
    private String selected;

    public DataProvider(
        List<SourceDocument> aData,
        List<String> headers, List<AnnotationDocument> aAllAnnotationDocuments)
    {
        this.data = aData;
        this.headers = headers;
        this.allAnnotationDocuments = aAllAnnotationDocuments;
        //TODO default value must be saved permanently, create new issue and PR
        this.defaultAnnotations = 6;

        //Init filter
        filter = new Filter();

        //Initial Sorting
        setSort(headers.get(0), SortOrder.ASCENDING);

        //Required
        model = new LoadableDetachableModel<List<SourceDocument>>() {
            private static final long serialVersionUID = -3938543310389673460L;

            @Override
            protected List<SourceDocument> load() {
                return data;
            }
        };


    }
    @Override
    public Iterator<SourceDocument> iterator(long first, long count)
    {

        input = filter.getInput();
        type = filter.getType();
        selected = filter.getSelected();

        //init
        if (selected == null) {
            selected = "false";
        }

        //Apply Filter
        List<SourceDocument> newList = filterTable(data);


        //Apply sorting
        newList.sort((o1, o2) ->
        {
            int dir = getSort().isAscending() ? 1 : -1;
            if (getSort().getProperty().equals(headers.get(0)))
            {
                return dir * (o1.getName().compareTo(o2.getName()));

            } else if (getSort().getProperty().equals(headers.get(1)))
            {
                return dir * Integer.compare(
                    getFinishedAmountForDocument(o1),
                    getFinishedAmountForDocument(o2));

            } else if (getSort().getProperty().equals(headers.get(2)))
            {
                return dir * Integer.compare(
                    getInProgressAmountForDocument(o1),
                    getInProgressAmountForDocument(o2));


            } else {
                return 0;
            }
        });


        //Reset
        this.shownDocuments = new ArrayList<>();
        shownDocuments.addAll(newList);

        if ((int)first + (int)count > newList.size())
        {
            count = newList.size() - first;
        }

        return newList.subList((int)first, ((int)first + (int)count)).iterator();
    }

    @Override
    public long size()
    {
        return data.size();
    }

    @Override
    public IModel<SourceDocument> model(SourceDocument sourceDocument)
    {
        return Model.of(sourceDocument);
    }

    @Override
    public void detach()
    {
        super.detach();
        model.detach();

    }


    public List<SourceDocument> filterTable(List<SourceDocument> data)
    {

        //TODO optimization definitely possible
        List<SourceDocument> resultList = new ArrayList<>();

        for (SourceDocument doc: data)
        {
            //Check for unused documents only and initalized
            if (this.selected.equals("true")) {
                if ((getInProgressAmountForDocument(doc) == 0)
                    && (getFinishedAmountForDocument(doc) == 0)) {
                    resultList.add(doc);
                    continue;
                }
            } else {
                if (this.type != null)
                {
                    if (this.type.equals("Document creation time:"))
                    {
                        try {
                            //Creation time filter
                            if (input != null) {
                                //TODO Rework of try catch and add Error (or regex for field input)
                                Date date = new SimpleDateFormat("dd/MM/yyyy").parse(input);
                                if (doc.getCreated() == null) {
                                } else {
                                    if (doc.getCreated().compareTo(date) >= 0)
                                    {
                                        resultList.add(doc);
                                        continue;

                                    }
                                }
                            }

                        } catch (ParseException e) {
                            e.printStackTrace();
                        }


                        //User filter
                    } else if (this.type.equals("User:"))
                    {
                        for (AnnotationDocument annotationDocument: allAnnotationDocuments)
                        {

                            if (annotationDocument.getUser().equals(this.input)
                                && annotationDocument.getName().equals(doc.getName()) &&
                                !annotationDocument.getState().equals
                                    (AnnotationDocumentState.NEW))
                            {
                                resultList.add(doc);
                                break;
                            }
                        }

                    }

                } else {
                    resultList = data;
                    break;
                }
            }

        }

        return resultList;
    }


    //Helper method, returns for a document how often it is "finished" within the project
    public int getFinishedAmountForDocument(
        SourceDocument aDocument)
    {
        int amount = 0;
        for (AnnotationDocument doc : allAnnotationDocuments)
        {
            if (aDocument.getName().equals(doc.getName()) &&
                doc.getState().equals(AnnotationDocumentState.FINISHED) )
            {
                amount++;
            }
        }
        return amount;
    }

    //Helper methods, returns for a document how often it is
    //currently "in progress" within the project
    public int getInProgressAmountForDocument(
        SourceDocument aDocument)
    {

        int amount = 0;
        for (AnnotationDocument doc : allAnnotationDocuments)
        {
            if (aDocument.getName().equals(doc.getName()) &&
                doc.getState().equals(AnnotationDocumentState.IN_PROGRESS))
            {
                amount++;
            }

        }

        return amount;
    }


    //Returns a random document out of all documents in the project.
    //Only a document is chosen which is not yet given to annotators more than the default number
    //per document number

    public SourceDocument getRandomDocument()
    {
        //Create an empty document
        SourceDocument document = null;
        Random r = new Random();

        while (document == null)
        {
            int i = r.nextInt(data.size() - 1);
            //If the random chosen document wont surpass the amount of default number combining
            // "inProgress" for the document + "finished" amount for the document + 1
            if ((getInProgressAmountForDocument(data.
                get(i)) +
                getFinishedAmountForDocument(data.
                    get(i)))
                + 1 <= defaultAnnotations)
            {
                //If that was not the case, assign this document
                document = data.get(r.nextInt(data.size() - 1));
            }
        }
        //Return the document
        //REMINDER: Document MIGHT BE NULL if there is not a single document left!
        // Annotator should then get the message: "No more documents to annotate"
        return document;
    }

    @Override
    public Filter getFilterState() {
        return filter;
    }

    @Override
    public void setFilterState(Filter filter) {
        this.filter = filter;

    }

    public List<SourceDocument> getShownDocuments() {
        return shownDocuments;
    }

}

