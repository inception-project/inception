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
package de.tudarmstadt.ukp.clarin.webanno.ui.automation.util;

import java.io.Serializable;

/**
 * A model representing the tab-sep training document which is to be used as target/train value
 *
 *
 */
public class TabSepDocModel
    implements Serializable
{
    private static final long serialVersionUID = 300260529426288095L;
    /**
     * This tab-Sep document is used as a target training document
     */
    boolean isTraining = false;
    /**
     * If the imported training document is tab-sep or not
     */
    boolean isTabSep = false;

    public TabSepDocModel(boolean aIsTraining, boolean aIsTabSep)
    {
        isTraining = aIsTraining;
        isTabSep = aIsTabSep;
    }

    public boolean isTraining()
    {
        return isTraining;
    }

    public void setTraining(boolean isTraining)
    {
        this.isTraining = isTraining;
    }

    public boolean isTabSep()
    {
        return isTabSep;
    }

    public void setTabSep(boolean isTabSep)
    {
        this.isTabSep = isTabSep;
    }

}
