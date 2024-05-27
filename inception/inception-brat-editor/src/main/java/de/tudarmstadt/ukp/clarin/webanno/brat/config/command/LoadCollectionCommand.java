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
package de.tudarmstadt.ukp.clarin.webanno.brat.config.command;

import static java.lang.String.format;

import java.io.IOException;

import org.springframework.core.annotation.Order;

import de.tudarmstadt.ukp.clarin.webanno.brat.message.GetCollectionInformationResponse;
import de.tudarmstadt.ukp.inception.externaleditor.command.EditorCommand;
import de.tudarmstadt.ukp.inception.support.json.JSONUtil;

@Order(10)
public class LoadCollectionCommand
    implements EditorCommand
{
    private static final long serialVersionUID = 1779280309942407825L;

    public static final String BRAT_EVENT_COLLECTION_LOADED = "collectionLoaded";

    private final GetCollectionInformationResponse collectionInfo;

    public LoadCollectionCommand(GetCollectionInformationResponse aCollectionInfo)
    {
        collectionInfo = aCollectionInfo;
    }

    @Override
    public String command(String aEditorVariable)
    {
        var responseJson = toJson(collectionInfo);
        return format("e.post('%s', [%s]);", BRAT_EVENT_COLLECTION_LOADED, responseJson);
    }

    private String toJson(Object result)
    {
        String json = "[]";
        try {
            json = JSONUtil.toInterpretableJsonString(result);
        }
        catch (IOException e) {
            // handleError("Unable to produce JSON response", e);
        }
        return json;
    }
}
