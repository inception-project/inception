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


package de.tudarmstadt.ukp.inception.app.ui.monitoring.support;

import java.util.List;
import java.util.Random;

import org.apache.wicket.spring.injection.annot.SpringBean;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;





//Helper methods for documents
public class Documents {


    private List<SourceDocument> documentList;
    private static Project project;
    private int defaultAnnotations;
    private static List<String> annotatedDocuments;

    private static @SpringBean ProjectService projectService;
    private @SpringBean DocumentService documentService;

    public Documents(Project project,List<SourceDocument> documentList)
    {
        this.project = project;
        this.documentList = documentList;
        annotatedDocuments = getAnnotatedDocuments();
        //TODO rework when own tableentry
        this.defaultAnnotations = 6;

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
                get(i)) + getFinishedAmountForDocument(documentList.get(i)))
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



    //Helper method, returns for a document how often it is finished within the project
    public static int getFinishedAmountForDocument(SourceDocument aDocument)
    {
        int amount = 0;
        for (int i = 0; i < annotatedDocuments.size(); i++)
        {
            if (aDocument.getName().equals(annotatedDocuments.get(i)))
            {
                amount++;
            }
        }
        return amount;
    }

    //Helper methods, returns for a document how often it is
    //currently in progress within the project
    public static int getInProgressAmountForDocument(SourceDocument aDocument)
    {
        int amount = 0;
        int amountUser = projectService.listProjectUsersWithPermissions
            (project).size();


        for (int i = 0; i <  annotatedDocuments.size(); i++)
        {
            if (annotatedDocuments.get(i).equals(aDocument.getName()))
            {
                amount++;
            }
        }

        return amountUser - amount;
    }

    public List<String> getAnnotatedDocuments()
    {
        //List for annotated documents
        for (int i = 0; i < documentService.listFinishedAnnotationDocuments
            (project).size(); i++)
        {
            annotatedDocuments.add(documentService.
                listFinishedAnnotationDocuments(project).get(i).getName());
        }

        return annotatedDocuments;
    }
}
