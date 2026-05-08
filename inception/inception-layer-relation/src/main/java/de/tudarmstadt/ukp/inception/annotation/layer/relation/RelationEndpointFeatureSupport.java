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
package de.tudarmstadt.ukp.inception.annotation.layer.relation;

import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.FEAT_REL_TARGET;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.PREFIX_SOURCE;
import static de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport.PREFIX_TARGET;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.substringAfter;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.api.RelationLayerSupport;
import de.tudarmstadt.ukp.inception.annotation.layer.relation.config.RelationLayerAutoConfiguration;
import de.tudarmstadt.ukp.inception.editor.action.AnnotationActionHandler;
import de.tudarmstadt.ukp.inception.rendering.editorstate.AnnotatorState;
import de.tudarmstadt.ukp.inception.rendering.editorstate.FeatureState;
import de.tudarmstadt.ukp.inception.schema.api.adapter.AnnotationException;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureEditor;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureSupport;
import de.tudarmstadt.ukp.inception.schema.api.feature.FeatureType;

/**
 * Extension providing image-related features for annotations.
 * <p>
 * This class is exposed as a Spring Component via
 * {@link RelationLayerAutoConfiguration#relationEndpointFeatureSupport}.
 * </p>
 */
public class RelationEndpointFeatureSupport
    implements FeatureSupport<RelationEndpointFeatureTraits>
{
    private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private String featureSupportId;

    @Autowired
    public RelationEndpointFeatureSupport()
    {
        // Nothing to do
    }

    @Override
    public String getId()
    {
        return featureSupportId;
    }

    @Override
    public void setBeanName(String aBeanName)
    {
        featureSupportId = aBeanName;
    }

    @Override
    public Optional<FeatureType> getFeatureType(AnnotationFeature aFeature)
    {
        if (!RelationLayerSupport.TYPE.equals(aFeature.getLayer().getType())) {
            return Optional.empty();
        }

        if (FEAT_REL_SOURCE.equals(aFeature.getName())) {
            return Optional.of(
                    new FeatureType(aFeature.getType(), "Relation source", featureSupportId, true));
        }

        if (FEAT_REL_TARGET.equals(aFeature.getName())) {
            return Optional.of(
                    new FeatureType(aFeature.getType(), "Relation target", featureSupportId, true));
        }

        return Optional.empty();
    }

    @Override
    public List<FeatureType> getSupportedFeatureTypes(AnnotationLayer aAnnotationLayer)
    {
        if (!RelationLayerSupport.TYPE.equals(aAnnotationLayer.getType())) {
            return emptyList();
        }

        var attachType = aAnnotationLayer.getAttachType();
        String attachTypeName;
        if (attachType == null) {
            attachTypeName = CAS.TYPE_NAME_ANNOTATION;
        }
        else {
            attachTypeName = attachType.getName();
        }

        return asList( //
                new FeatureType(PREFIX_SOURCE + attachTypeName, "Relation source", featureSupportId,
                        true), //
                new FeatureType(PREFIX_TARGET + attachTypeName, "Relation target", featureSupportId,
                        true));
    }

    @Override
    public boolean accepts(AnnotationFeature aFeature)
    {
        return aFeature.getType().startsWith(PREFIX_SOURCE)
                || aFeature.getType().startsWith(PREFIX_TARGET);
    }

    @Override
    public Panel createTraitsEditor(String aId, IModel<AnnotationFeature> aFeatureModel)
    {
        return new EmptyPanel(aId);
    }

    @Override
    public FeatureEditor createEditor(String aId, MarkupContainer aOwner,
            AnnotationActionHandler aHandler, IModel<AnnotatorState> aStateModel,
            IModel<FeatureState> aFeatureStateModel)
    {
        return null;
    }

    @Override
    public RelationEndpointFeatureTraits createDefaultTraits()
    {
        return new RelationEndpointFeatureTraits();
    }

    @Override
    public void generateFeature(TypeSystemDescription aTSD, TypeDescription aTD,
            AnnotationFeature aFeature)
    {
        if (aFeature.getType().startsWith(PREFIX_SOURCE)) {
            aTD.addFeature(aFeature.getName(), "",
                    substringAfter(aFeature.getType(), PREFIX_SOURCE));
        }
        else if (aFeature.getType().startsWith(PREFIX_TARGET)) {
            aTD.addFeature(aFeature.getName(), "",
                    substringAfter(aFeature.getType(), PREFIX_TARGET));
        }
        else {
            throw new IllegalStateException(
                    "Unsupported feature type [" + aFeature.getType() + "]");
        }
    }

    @Override
    public <V> V unwrapFeatureValue(AnnotationFeature aFeature, Object aValue)
    {
        throw new NotImplementedException("Relation endpoints do not support unwrapFeatureValue");
    }

    @Override
    public Serializable wrapFeatureValue(AnnotationFeature aFeature, CAS aCAS, Object aValue)
    {
        throw new NotImplementedException("Relation endpoints do not support wrapFeatureValue");
    }

    @Override
    public <V> V getFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        throw new NotImplementedException("Relation endpoints do not support getFeatureValue");
    }

    @Override
    public <V> V getNullFeatureValue(AnnotationFeature aFeature, FeatureStructure aFS)
    {
        throw new NotImplementedException(
                "Relation endpoints do not support getDefaultFeatureValue");
    }

    @Override
    public void setFeatureValue(CAS aCas, AnnotationFeature aFeature, int aAddress, Object aValue)
        throws AnnotationException
    {
        throw new NotImplementedException("Relation endpoints do not support setFeatureValue");
    }

    @Override
    public boolean isUsingDefaultOptions(AnnotationFeature aFeature)
    {
        return false;
    }

    @Override
    public boolean isAccessible(AnnotationFeature aFeature)
    {
        return false;
    }

    @Override
    public boolean isCopyOnCurationMerge(AnnotationFeature aFeature)
    {
        // End-points are merged as part of copying the annotation position
        return false;
    }
}
