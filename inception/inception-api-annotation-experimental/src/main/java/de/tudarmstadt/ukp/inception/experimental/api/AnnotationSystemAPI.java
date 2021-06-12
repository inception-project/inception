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

import org.apache.uima.cas.CAS;

public interface AnnotationSystemAPI
{
    void handleDocument(String[] aData) throws IOException;

    void handleViewport(String[] aData) throws IOException;

    void handleSelectAnnotation(String[] aData) throws IOException;

    void handleCreateAnnotation(String[] aData) throws IOException;

    void handleDeleteAnnotation(String[] aData) throws IOException;

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

}
