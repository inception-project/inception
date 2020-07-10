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

import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.FINISHED;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IGNORE;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.IN_PROGRESS;
import static de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState.NEW;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocumentState;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;

public class TableContentProvider extends SortableDataProvider
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

    public TableContentProvider(
        List<SourceDocument> aData,
        List<String> aHeaders, List<AnnotationDocument> aAllAnnotationDocuments)
    {
        this.data = aData;
        this.headers = aHeaders;
        this.allAnnotationDocuments = aAllAnnotationDocuments;
        this.defaultAnnotations = 6;
        this.shownDocuments = new ArrayList<>();

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
    public Iterator<SourceDocument> iterator(long aFirst, long aCount)
    {
        //Apply Filter
        List<SourceDocument> newList = filterTable(data);

        //Apply sorting
        newList.sort((o1, o2) ->
        {
            int dir = getSort().isAscending() ? 1 : -1;
            if (getSort().getProperty().equals(headers.get(0))) {
                return dir * (o1.getName().compareTo(o2.getName()));
            }
            else if (getSort().getProperty().equals(headers.get(1))) {
                return dir * Long.compare(getFinishedAmountForDocument(o1),
                        getFinishedAmountForDocument(o2));
            }
            else if (getSort().getProperty().equals(headers.get(2))) {
                return dir * Long.compare(getInProgressAmountForDocument(o1),
                        getInProgressAmountForDocument(o2));
            }
            else if (getSort().getProperty().equals(headers.get(3))) {
                return dir * (Integer.compare(getUsersWorkingOnTheDocument(o1).length(),
                        getUsersWorkingOnTheDocument(o2).length()));

            }
            else if (getSort().getProperty().equals(headers.get(4))) {
                if (o1.getUpdated() == null) {
                    return dir;
                }
                else if (o2.getUpdated() == null) {
                    return dir * -1;
                }
                else {
                    return dir * (o1.getUpdated().compareTo(o2.getUpdated()));
                }
            }
            else {
                return 0;
            }
        });


        //Reset
        this.shownDocuments.clear();
        shownDocuments.addAll(newList);


        if ((int)aFirst + (int)aCount > newList.size())
        {
            aCount = newList.size() - aFirst;
        }

        return newList.subList((int)aFirst, ((int)aFirst + (int)aCount)).iterator();
    }

    @Override
    public long size()
    {
        return filterTable(data).size();
    }

    @Override
    public IModel<SourceDocument> model(SourceDocument aSourceDocument)
    {
        return Model.of(aSourceDocument);
    }

    @Override
    public void detach() {
        super.detach();
        model.detach();
    }

    public List<SourceDocument> filterTable(List<SourceDocument> aData)
    {
        List<SourceDocument> resultList = new ArrayList<>();
        List<SourceDocument> userNameList = new ArrayList<>();
        List<SourceDocument> docNameList = new ArrayList<>();
        List<SourceDocument> dateList = new ArrayList<>();
        List<SourceDocument> unusedList = new ArrayList<>();

        //Avoid error in one specific case
        if (filter.getSelected() == null) {
            filter.setSelected("false");
        }

        for (SourceDocument doc: aData)
        {
            // Unused documents selected
            if (filter.getSelected().equals("true")) {
                if ((getInProgressAmountForDocument(doc) == 0)
                    && (getFinishedAmountForDocument(doc) == 0)) {
                    unusedList.add(doc);
                }
            }


            //Check if DocumentName was entered
            if (filter.getDocumentName() != null) {
                if (doc.getName().contains(filter.getDocumentName())) {
                    docNameList.add(doc);
                }
            }


            //Check if Username filter was entered
            if (filter.getUsername() != null) {
                //Get all entered usernames

                String [] usernames = filter.getUsername().split(",");
                //Get all documents with the given user
                for (String user: usernames) {
                    for (AnnotationDocument annotationDocument : allAnnotationDocuments) {
                        if (annotationDocument.getUser().equals(user)
                            && annotationDocument.getName().equals(doc.getName()) &&
                            !annotationDocument.getState().equals
                                (AnnotationDocumentState.NEW)) {
                            userNameList.add(doc);
                            break;
                        }
                    }
                }

            }

            //Check if one of the date fields are selected and the document
            // has a created value

            if ((filter.getFrom() != null || filter.getTo() != null)
                && doc.getUpdated() != null) {

                //between selected
                if (filter.getFrom() != null && filter.getTo() != null) {
                    if (filter.getFrom().compareTo(filter.getTo()) >= 0) {
                        if (doc.getUpdated().compareTo(filter.getTo()) > 0 &&
                            doc.getUpdated().compareTo(filter.getFrom()) <= 0) {
                            dateList.add(doc);
                        }
                    }
                    if (doc.getUpdated().compareTo(filter.getFrom()) > 0 &&
                        doc.getUpdated().compareTo(filter.getTo()) <= 0) {
                        dateList.add(doc);
                    }
                    //From selected
                } else if (filter.getFrom() != null) {
                    if (doc.getUpdated().compareTo(filter.getFrom()) >= 0) {
                        dateList.add(doc);
                    }
                } else {
                    //Until
                    if (doc.getUpdated().compareTo(filter.getTo()) <= 0) {
                        dateList.add(doc);
                    }
                }
            }
        }

        //Schnittmenge of the lists
        List<List<SourceDocument>> finalList = new ArrayList<>();
        if (dateList.size() > 0 || filter.getFrom() != null || filter.getTo() != null) {
            finalList.add(dateList);
        }

        if (docNameList.size() > 0 || filter.getDocumentName() != null) {
            finalList.add(docNameList);
        }

        if (userNameList.size() > 0 || filter.getUsername() != null) {
            finalList.add(userNameList);
        }

        if (unusedList.size() > 0 || filter.getSelected() == "true") {
            finalList.add(unusedList);
        }

        for (int i = 0; i < finalList.size() - 1; i++) {
            finalList.get(0).retainAll(finalList.get(i + 1));
        }

        if (finalList.size() == 0) {
            resultList = aData;
        } else {
            resultList = finalList.get(0);
        }

        return resultList;
    }


    /**
     * Helper method, returns for a document how often it is "finished" within the project
     */
    public long getFinishedAmountForDocument(
        SourceDocument aDocument)
    {
        return allAnnotationDocuments.stream()
                .filter(d -> d.getDocument().equals(aDocument) && d.getState().equals(FINISHED))
                .count();
    }

    /**
     * Helper methods, returns for a document how often it is currently "in progress" within the
     * project.
     */
    public long getInProgressAmountForDocument(
        SourceDocument aDocument)
    {
        return allAnnotationDocuments.stream()
            .filter(d -> d.getDocument().equals(aDocument) && d.getState().equals(IN_PROGRESS))
            .count();
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

    public String getUsersWorkingOnTheDocument(SourceDocument aDocument)
    {
        return allAnnotationDocuments.stream()
            .filter(d -> d.getDocument().equals(aDocument) &&
                    !d.getState().equals(NEW) && 
                    !d.getState().equals(IGNORE))
            .map(AnnotationDocument::getUser)
            .sorted()
            .collect(Collectors.joining(", "));
    }


    public Date lastAccessTimeForDocument(SourceDocument aDoc)
    {
        return aDoc.getUpdated();
    }
}

