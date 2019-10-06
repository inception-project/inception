/*
 * Copyright 2019
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.formats.perseus.internal.model;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;

public class PerseusWord
{
    @XmlID 
    @XmlAttribute
    public String id;

    @XmlAttribute
    public String form;
    
    @XmlAttribute
    public String lemma;

    @XmlAttribute
    public String postag;

    @XmlAttribute
    public String relation;

    @XmlAttribute
    public String cite;

    @XmlAttribute
    public int head;
    
    @XmlAttribute(name = "insertion_id")
    public String insertionId;
    
    @XmlAttribute
    public String artificial;
}
