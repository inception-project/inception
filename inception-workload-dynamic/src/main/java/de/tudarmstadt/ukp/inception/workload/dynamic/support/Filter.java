/*
 * Copyright 2020
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

package de.tudarmstadt.ukp.inception.workload.dynamic.support;

import java.io.Serializable;
import java.util.Date;

//Helper class for the Filter
public class Filter implements Serializable
{

    private static final long serialVersionUID = 256259364194000084L;

    //Input fields appended value
    private String username;
    private String documentName;

    //Checkbox
    private String selected;

    //dates
    private Date from;
    private Date to;

    //Default constructor
    public Filter() {
        this.selected = "false";
    }

    public String getUsername() {
        return username;
    }

    public String getDocumentName() {
        return documentName;
    }

    public String getSelected() {
        return selected;
    }

    public Date getFrom() {
        return from;
    }

    public Date getTo() {
        return to;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public void setSelected(String selected) {
        this.selected = selected;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public void setTo(Date to) {
        this.to = to;
    }
}
