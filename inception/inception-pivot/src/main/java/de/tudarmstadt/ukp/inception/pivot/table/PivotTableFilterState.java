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
package de.tudarmstadt.ukp.inception.pivot.table;

import java.io.Serializable;

public class PivotTableFilterState
    implements Serializable
{
    private static final long serialVersionUID = 5086370150071680287L;

    private boolean hideRowsWithSameValuesInAllColumns = false;
    private boolean hideRowsWithAnyDifferentValue = false;
    private boolean hideRowsWithEmptyValues = false;
    private boolean hideRowsWithoutEmptyValues = false;

    public boolean isHideRowsWithSameValuesInAllColumns()
    {
        return hideRowsWithSameValuesInAllColumns;
    }

    public void setHideRowsWithSameValuesInAllColumns(boolean aHideRowsWithSameValuesInAllColumns)
    {
        if (aHideRowsWithSameValuesInAllColumns) {
            hideRowsWithAnyDifferentValue = false;
        }

        hideRowsWithSameValuesInAllColumns = aHideRowsWithSameValuesInAllColumns;
    }

    public boolean isHideRowsWithAnyDifferentValue()
    {
        return hideRowsWithAnyDifferentValue;
    }

    public void setHideRowsWithAnyDifferentValue(boolean aHideRowsWithAnyDifferentValue)
    {
        if (aHideRowsWithAnyDifferentValue) {
            hideRowsWithSameValuesInAllColumns = false;
        }
        hideRowsWithAnyDifferentValue = aHideRowsWithAnyDifferentValue;
    }

    public boolean isHideRowsWithEmptyValues()
    {
        return hideRowsWithEmptyValues;
    }

    public void setHideRowsWithEmptyValues(boolean aHideRowsWithEmptyValues)
    {
        if (aHideRowsWithEmptyValues) {
            hideRowsWithoutEmptyValues = false;
        }

        hideRowsWithEmptyValues = aHideRowsWithEmptyValues;
    }

    public boolean isHideRowsWithoutEmptyValues()
    {
        return hideRowsWithoutEmptyValues;
    }

    public void setHideRowsWithoutEmptyValues(boolean aHideRowsWithoutEmptyValues)
    {
        if (aHideRowsWithoutEmptyValues) {
            hideRowsWithEmptyValues = false;
        }

        hideRowsWithoutEmptyValues = aHideRowsWithoutEmptyValues;
    }
}
