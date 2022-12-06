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
package de.tudarmstadt.ukp.clarin.webanno.export.model;

/**
 * Constants to import/export tagsets in the JSON and tabbed text format
 */
public class ExportedTagSetConstant
{
    /**
     * The key for the tagsetNAme
     */
    public static final String TAGSETNAME = "tagset name";

    /**
     * The key for tagset description. [optional]
     */
    public static final String TAGSETDESCRIPTION = "tagset description";

    /**
     * the type of the tagset, either span or arc
     */
    public static final String TAGSETTYPE = "tagset type";

    /**
     * the language of the tagset. [optional]
     */
    public static final String TAGSETLANGUAGE = "tagset language";

    /**
     * the name of the tagset type, such as pos, named entity, dependency,...
     */
    public static final String TAGSETTYPENAME = "tagset type name";

    /**
     * the description of tagset type. [optional]
     */
    public static final String TAGSETTYPEDESCRIPTION = "tagset type description";
}
