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
package de.tudarmstadt.ukp.inception.experimental.api;

import java.io.IOException;
import java.util.List;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.inception.experimental.api.message.ClientMessage;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;

public interface AnnotationSystemAPI
{
    void handleDocument(ClientMessage aClientMessage) throws IOException;

    void handleViewport(ClientMessage aClientMessage) throws IOException;

    void handleSelectAnnotation(ClientMessage aClientMessage) throws IOException;

    void handleUpdateAnnotation(ClientMessage aClientMessage) throws IOException;

    void handleCreateAnnotation(ClientMessage aClientMessage) throws IOException;

    void handleDeleteAnnotation(ClientMessage aClientMessage) throws IOException;

    /**
     * Returns CAS from websocket message. All three String parameters are contained in the header
     * of the websocket message
     *
     * @param aProject
     *            long
     * @param aDocument
     *            long
     * @param aUser
     *            string
     * @return CAS
     */
    CAS getCasForDocument(String aUser, long aProject, long aDocument);

    Character[] getViewportText(ClientMessage aClientMessage, CAS aCas);

    List<Annotation> getAnnotations(CAS aCas, long aProject);

    List<Annotation> filterAnnotations(List <Annotation> aAnnotations, int[][] aViewport);

    void createErrorMessage(String aMessage, String aUser) throws IOException;

    FeatureStructure getFeatureStructure(CAS aCas, long aProject, String aAnnotationType);

    List<Feature> getFeaturesForFeatureStructure(FeatureStructure aFeatureStructure);

    void refreshAnnotationLayers();

    Type getType(CAS aCas, String aAnnotationType);


}
