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
package de.tudarmstadt.ukp.inception.diam.model.compactv2;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.tudarmstadt.ukp.inception.support.json.BeanAsArraySerializer;

@JsonSerialize(using = BeanAsArraySerializer.class)
@JsonPropertyOrder(value = { "comment", "type" })
public class CompactComment
{
    public static final String INFO = "I";
    public static final String ERROR = "E";

    private final String type;
    private final String comment;

    public CompactComment(String aComment)
    {
        type = null;
        comment = aComment;
    }

    public CompactComment(String aComment, String aType)
    {
        type = INFO.equals(aType) ? null : aType;
        comment = aComment;
    }

    public String getComment()
    {
        return comment;
    }

    /**
     * @return optional type. If empty, type {@code I} is assumed (information).
     */
    public String getType()
    {
        return type;
    }
}
