/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.export.model;

/**
 * Constants to Improt/export tagsets in the json and tabbed text format
 * @author Seid Muhie Yimam
 *
 */
public class ExportedTagSetConstant
{
    /**
     * Expor/import tagsets in json format
     */
public static final String JSON_FORMAT = "JSON";
/**
 * Export/Import TagSets in TAB separated format where the first five lines are information about
 * the tagsets such as tagsetname, tagsetdescrition and the remaining lines are tag with optional
 * tag descriptions. The format looks like:<br>
 * <pre>
 * tagSetName   TAGSETNAME
 * tagSetDescription    DESCRIPTION
 * ...
 * TAG1 <optional>TAG1Description
 * ....
 * </pre>
 */
public static final String TAB_FORMAT = "TAB-sep";

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
