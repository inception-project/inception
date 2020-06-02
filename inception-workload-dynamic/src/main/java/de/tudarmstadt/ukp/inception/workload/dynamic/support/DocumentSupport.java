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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;

//Helper methods for documents
public class DocumentSupport implements Serializable {


    private static final long serialVersionUID = -714653412126656866L;
    private final List<SourceDocument> documentList;
    private Project project;
    private final int defaultAnnotations;
    private List<String> annotatedDocuments;

    private DocumentService documentService;

    public DocumentSupport(
        Project project, List<SourceDocument> documentList)
    {

        this.project = project;
        this.documentList = documentList;
        //TODO rework when own table entry
        this.defaultAnnotations = 6;

        this.documentService = getDocumentService();
        this.annotatedDocuments = getAnnotatedDocuments();



    }

    //Helper method, returns for a document how often it is finished within the project
    public int getFinishedAmountForDocument(
        SourceDocument aDocument)
    {
        int amount = 0;
        for (String annotatedDocument : annotatedDocuments) {
            if (aDocument.getName().equals(annotatedDocument)) {
                amount++;
            }
        }
        return amount;
    }

    //Helper methods, returns for a document how often it is
    //currently in progress within the project
    public int getInProgressAmountForDocument(
        SourceDocument aDocument)
    {
        int amount = 0;
        //TODO get correct value
        int amountUser = 6;


        for (String annotatedDocument : annotatedDocuments) {
            if (annotatedDocument.equals(aDocument.getName())) {
                amount++;
            }
        }

        return amountUser - amount;
    }


    public List<String> getAnnotatedDocuments()
    {
        //List for annotated documents
        List<String> list = new ArrayList<>();

        for (AnnotationDocument doc: documentService.listFinishedAnnotationDocuments
            (project))
        {
            list.add(doc.getName());
        }

        return list;
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
            int i = r.nextInt(documentList.size() - 1);
            //If the random chosen document wont surpass the amount of default number combining
            // "inProgress" for the document + "finished" amount for the document + 1
            if ((getInProgressAmountForDocument(documentList.
                get(i)) +
                getFinishedAmountForDocument(documentList.
                    get(i)))
                + 1 <= defaultAnnotations)
            {
                //If that was not the case, assign this document
                document = documentList.get(r.nextInt(documentList.size() - 1));
            }
        }
        //Return the document
        //REMINDER: Document MIGHT BE NULL if there is not a single document left!
        // Annotator should then get the message: "No more documents to annotate"
        return document;
    }


    //Returns the UserDao bean
    public UserDao getUserRepository()
    {
        return SpringContext.getBean(UserDao.class);
    }

    //Returns the ProjectService bean
    public ProjectService getProjectService()
    {
        return SpringContext.getBean(ProjectService.class);
    }

    //Returns the DocumentService bean
    public DocumentService getDocumentService()
    {
        return SpringContext.getBean(DocumentService.class);
    }

}
