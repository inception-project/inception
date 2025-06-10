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
package de.tudarmstadt.ukp.inception.pdfeditor2.visual;

public interface PdfEventHandler
{
    default void documentStart() throws Exception
    {
        // Nothing by default
    }

    default void documentEnd() throws Exception
    {
        // Nothing by default
    }

    default void beforeStartParagraph() throws Exception
    {
        // Nothing by default
    }

    default void afterEndParagraph() throws Exception
    {
        // Nothing by default
    }

    default void beforeStartPage() throws Exception
    {
        // Nothing by default
    }

    default void afterEndPage() throws Exception
    {
        // Nothing by default
    }

    default void afterStartParagraph() throws Exception
    {
        // Nothing by default
    }

    default void beforeEndParagraph() throws Exception
    {
        // Nothing by default
    }

    default void afterStartPage() throws Exception
    {
        // Nothing by default
    }

    default void beforeEndPage() throws Exception
    {
        // Nothing by default
    }
}
