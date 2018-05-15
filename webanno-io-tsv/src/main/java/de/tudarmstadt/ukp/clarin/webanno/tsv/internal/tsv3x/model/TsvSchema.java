/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 */package de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model;

import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isChainLayer;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isRelationLayer;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.Tsv3XCasSchemaAnalyzer.isSpanLayer;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_ROLE;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.FeatureType.SLOT_TARGET;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.CHAIN;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.RELATION;
import static de.tudarmstadt.ukp.clarin.webanno.tsv.internal.tsv3x.model.LayerType.SPAN;
import static org.apache.commons.lang3.StringUtils.compare;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.cas.Type;

public class TsvSchema
{
    // Mind these here are define as the reverse of the respective contants in WebAnnoConst 
    // because here we want that "TARGET" identifies the FS which should be pointed to in
    // the TSV.
    public static final String FEAT_REL_TARGET = "Dependent";
    public static final String FEAT_REL_SOURCE = "Governor";

    public static final String FEAT_SLOT_ROLE = "role";
    public static final String FEAT_SLOT_TARGET = "target";
    
    public static final String CHAIN_FIRST_FEAT = "first";
    public static final String CHAIN_NEXT_FEAT = "next";
    public static final String COREFERENCE_RELATION_FEATURE = "referenceRelation";
    public static final String COREFERENCE_TYPE_FEATURE = "referenceType";
    
    private List<TsvColumn> columns = new ArrayList<>();
    private Set<Type> chainHeadTypes = new HashSet<>();

    public void addColumn(TsvColumn aColumn)
    {
        columns.add(aColumn);
    }
    
    public List<TsvColumn> getColumns()
    {
        return columns;
    }
    
    public List<TsvColumn> getColumns(Type aType)
    {
        return getColumns().stream()
                .filter(c -> c.uimaType.equals(aType))
                // Sort by name so we get a stable column order even if type systems are merged
                // in different orders, e.g. in unit tests.
                .sorted((a, b) -> compare(
                        a.getUimaFeature() != null ? a.getUimaFeature().getShortName() : null,
                        b.getUimaFeature() != null ? b.getUimaFeature().getShortName() : null))
                .collect(Collectors.toList());
    }
    
    /**
     * Returns the columns in the same order as they are in the TSV header.
     * 
     * @param aActiveColumns
     *            columns for which actual annotations exist.
     * @return the list of columns in the order as defined in the schema header.
     */
    public List<TsvColumn> getHeaderColumns(Collection<TsvColumn> aActiveColumns)
    {
        List<TsvColumn> cols = new ArrayList<>();
        
        // COMPATIBILITY NOTE
        // We try to maintain the same order of columns as the WebAnnoTsv3Writer because the 
        // WebAnnoTsv3Reader needs that order. Our own reader does not rely on this order.
        // - SPAN layers without slot features
        // - SPAN layers with slot features
        // - CHAIN layers
        // - RELATION layers
        List<Type> headerTypes = new ArrayList<>();
        headerTypes.addAll(getUimaTypes(SPAN, false));
        headerTypes.addAll(getUimaTypes(SPAN, true));
        headerTypes.addAll(getUimaTypes(CHAIN, false));
        headerTypes.addAll(getUimaTypes(RELATION, false));
        
        for (Type type : headerTypes) {
            List<TsvColumn> typeColumns = getColumns(type);
            typeColumns.retainAll(aActiveColumns);
            if (typeColumns.isEmpty()) {
                continue;
            }
            
            // Ensure that relation source columns come last.
            { 
                TsvColumn relRefCol = null;
                for (TsvColumn col : typeColumns) {
                    if (col.layerType.equals(RELATION)
                            && FEAT_REL_SOURCE.equals(col.uimaFeature.getShortName())) {
                        relRefCol = col;
                        continue;
                    }
    
                    cols.add(col);
                }
                
                if (relRefCol != null) {
                    cols.add(relRefCol);
                }
            }
        }
        
        return cols;
    }
        
    public Set<Type> getUimaTypes()
    {
        Set<Type> types = new LinkedHashSet<>();
        for (TsvColumn col : columns) {
            types.add(col.uimaType);
        }
        return types;
    }
    
    /**
     * @param aSlotFeatures
     *            if {@code true}, returns only types <b>with slot features</b>, otherwise returns
     *            only types <b>without slot features</b>.
     */
    private Set<Type> getUimaTypes(LayerType aLayerType, boolean aSlotFeatures)
    {
        Set<Type> types = new LinkedHashSet<>();
        for (TsvColumn col : columns) {
            if (aLayerType.equals(col.layerType)) {
                boolean hasSlotFeatures = columns.stream().anyMatch(c -> 
                        c.uimaType.equals(col.uimaType) &&
                        (SLOT_ROLE.equals(c.featureType) || SLOT_TARGET.equals(c.featureType)));

                if (hasSlotFeatures == aSlotFeatures) {
                    types.add(col.uimaType);
                }
            }
        }
        return types;
    }

    public LayerType getLayerType(Type aType)
    {
        if (isRelationLayer(aType)) {
            return LayerType.RELATION;
        }
        else if (isChainLayer(aType)) {
            return LayerType.CHAIN;
        }
        else if (isSpanLayer(aType)) {
            return LayerType.SPAN;
        }
        else {
            return LayerType.INCOMPATIBLE;
        }
    }

    public void addChainHeadType(Type aType)
    {
        chainHeadTypes.add(aType);
    }
    
    public Set<Type> getChainHeadTypes()
    {
        return chainHeadTypes;
    }
}
