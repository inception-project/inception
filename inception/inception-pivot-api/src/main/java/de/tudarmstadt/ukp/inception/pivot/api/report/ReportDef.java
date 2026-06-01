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
package de.tudarmstadt.ukp.inception.pivot.api.report;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "schemaVersion", "aggregator", "rowExtractors", "colExtractors",
        "cellExtractors", "filter" })
public class ReportDef
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    public static final int CURRENT_SCHEMA_VERSION = 1;

    private int schemaVersion = CURRENT_SCHEMA_VERSION;
    private AggregatorDef aggregator;
    private List<ExtractorDef> rowExtractors = new ArrayList<>();
    private List<ExtractorDef> colExtractors = new ArrayList<>();
    private List<ExtractorDef> cellExtractors = new ArrayList<>();
    private FilterDef filter = new FilterDef();

    public int getSchemaVersion()
    {
        return schemaVersion;
    }

    public void setSchemaVersion(int aSchemaVersion)
    {
        schemaVersion = aSchemaVersion;
    }

    public AggregatorDef getAggregator()
    {
        return aggregator;
    }

    public void setAggregator(AggregatorDef aAggregator)
    {
        aggregator = aAggregator;
    }

    public List<ExtractorDef> getRowExtractors()
    {
        return rowExtractors;
    }

    public void setRowExtractors(List<ExtractorDef> aRowExtractors)
    {
        rowExtractors = aRowExtractors != null ? aRowExtractors : new ArrayList<>();
    }

    public List<ExtractorDef> getColExtractors()
    {
        return colExtractors;
    }

    public void setColExtractors(List<ExtractorDef> aColExtractors)
    {
        colExtractors = aColExtractors != null ? aColExtractors : new ArrayList<>();
    }

    public List<ExtractorDef> getCellExtractors()
    {
        return cellExtractors;
    }

    public void setCellExtractors(List<ExtractorDef> aCellExtractors)
    {
        cellExtractors = aCellExtractors != null ? aCellExtractors : new ArrayList<>();
    }

    public FilterDef getFilter()
    {
        return filter;
    }

    public void setFilter(FilterDef aFilter)
    {
        filter = aFilter != null ? aFilter : new FilterDef();
    }

    @Override
    public boolean equals(Object aOther)
    {
        if (this == aOther) {
            return true;
        }
        if (!(aOther instanceof ReportDef that)) {
            return false;
        }
        return schemaVersion == that.schemaVersion //
                && Objects.equals(aggregator, that.aggregator) //
                && Objects.equals(rowExtractors, that.rowExtractors) //
                && Objects.equals(colExtractors, that.colExtractors) //
                && Objects.equals(cellExtractors, that.cellExtractors) //
                && Objects.equals(filter, that.filter);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(schemaVersion, aggregator, rowExtractors, colExtractors, cellExtractors,
                filter);
    }
}
