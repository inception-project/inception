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
package de.tudarmstadt.ukp.inception.pdfeditor.pdfanno.model;

/**
 * @deprecated Superseded by the new PDF editor
 */
@Deprecated
public class Relation
{
    private String id;
    private String head;
    private String tail;
    private String label;
    private String color;

    public Relation(String aId, String aHead, String aTail, String aLabel, String aColor)
    {
        id = aId;
        head = aHead;
        tail = aTail;
        label = aLabel;
        color = aColor;
    }

    public String getId()
    {
        return id;
    }

    public String getHead()
    {
        return head;
    }

    public String getTail()
    {
        return tail;
    }

    public String getLabel()
    {
        return label;
    }

    public String getColor()
    {
        return color;
    }

    public String toAnnoFileString()
    {
        return "[[relations]]\n" + //
                "id = \"" + id + "\"\n" + //
                "head = \"" + head + "\"\n" + //
                "tail = \"" + tail + "\"\n" + //
                "label = \"" + label + "\"\n" + //
                "color = \"" + color + "\"\n";
    }

}
