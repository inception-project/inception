/*
 * Copyright 2018
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

public class Relation {

    private String id;

    private String head;

    private String tail;

    private String label;

    public Relation(String aId, String aHead, String aTail, String aLabel)
    {
        id = aId;
        head = aHead;
        tail = aTail;
        label = aLabel;
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

    public String toAnnoFileString()
    {
        return "[[relations]]\n" +
            "id = \"" + id + "\"\n" +
            "head = \"" + head +  "\"\n" +
            "tail = \"" + tail + "\"\n" +
            "label = \"" + label + "\"\n";
    }

}
