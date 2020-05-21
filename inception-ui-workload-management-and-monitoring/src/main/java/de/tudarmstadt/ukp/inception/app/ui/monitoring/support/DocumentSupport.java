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

import java.io.Serializable;
import java.util.List;
import java.util.Random;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;


//Helper methods for documents
public class DocumentSupport implements Serializable {


    private static final long serialVersionUID = -714653412126656866L;
    private final List<SourceDocument> documentList;
    private Project project;
    private final int defaultAnnotations;

    public DocumentSupport(
        Project project, List<SourceDocument> documentList)
    {

        this.project = project;
        this.documentList = documentList;
        //TODO rework when own table entry
        this.defaultAnnotations = 6;

    }








    //Helper method, returns for a document how often it is finished within the project
    public int getFinishedAmountForDocument(
        SourceDocument aDocument, List<String> annotatedDocuments)
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
        SourceDocument aDocument, List<String> annotatedDocuments)
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

    public boolean isUserForDocument(SourceDocument aDocument, String user)
    {
        if (true) {
            return true;
        } else {
            return false;
        }

    }

}
