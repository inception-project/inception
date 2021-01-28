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
package de.tudarmstadt.ukp.clarin.webanno.brat.render.model;

public interface Comment
{
    public static final String EDIT_HIGHLIGHT = "EditHighlight";

    public static final String ANNOTATION_ERROR = "AnnotationError";
    public static final String ANNOTATION_INCOMPLETE = "AnnotationIncomplete";
    public static final String ANNOTATION_UNCONFIRMED = "AnnotationUnconfirmed";
    public static final String ANNOTATION_WARNING = "AnnotationWarning";
    public static final String ANNOTATOR_NOTES = "AnnotatorNotes";
    public static final String MISSING_ANNOTATION = "MissingAnnotation";
    public static final String CHANGED_ANNOTATION = "ChangedAnnotation";
    public static final String NORMALIZED = "Normalized";
    public static final String TRUE_POSITIVE = "True_positive";
    public static final String FALSE_POSITIVE = "False_positive";
    public static final String FALSE_NEGATIVE = "False_negative";

    String getCommentType();

    String getComment();
}
