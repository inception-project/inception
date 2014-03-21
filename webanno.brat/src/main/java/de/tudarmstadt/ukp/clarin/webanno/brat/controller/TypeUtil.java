/*******************************************************************************
 * Copyright 2012
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
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.clarin.webanno.model.TagSet;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;

/**
 * Utility Class for {@link TypeAdapter} with static methods such as geting {@link TypeAdapter}
 * based on its {@link CAS} {@link Type}
 *
 * @author Richard Eckart de Castilho
 * @author Seid Muhie Yimam
 *
 */
public final class TypeUtil
{
    private TypeUtil()
    {
        // No instances
    }

    public static TypeAdapter getAdapter(TagSet aTagSet, AnnotationService aAnnotationService)
    {
        AnnotationType type = aTagSet.getFeature().getLayer();
        AnnotationFeature feature = aTagSet.getFeature();
        String name = type.getName();

        if (type.getType().equals("span")) {
            TypeAdapter adapter = new SpanAdapter(feature.getId(), type.getName(),
                    feature.getName(), type.getAttachFeature() == null ? null : type
                            .getAttachFeature().getName(), type.getAttachType() == null ? null
                            : type.getAttachType().getName());
            return adapter;
        }
        else if (type.getType().equals("relation")) {

            TypeAdapter adapter = new ArcAdapter(feature.getId(), type.getName(),
                    feature.getName(), "Dependent", "Governor",
                    type.getAttachFeature() == null ? null : type.getAttachFeature().getName(),
                    type.getAttachType().getName());
            return adapter;
        }
        else if (type.getType().equals("chain")) {
            if (feature.getName().equals("referenceType")) {
                ChainAdapter adapter = new ChainAdapter(feature.getId(), type.getName() + "Link",
                        feature.getName(), "first", "next");
                adapter.setChain(false);
                return adapter;
            }
            else {
                ChainAdapter adapter = new ChainAdapter(feature.getId(), type.getName() + "Chain",
                        feature.getName(), "first", "next");
                adapter.setChain(true);
                return adapter;
            }

        }
        /*
         * if (name.equals(AnnotationTypeConstant.POS)) { return SpanAdapter.getPosAdapter(); } else
         * if (name.equals(AnnotationTypeConstant.LEMMA)) { return SpanAdapter.getLemmaAdapter(); }
         * else if (name.equals(AnnotationTypeConstant.NAMEDENTITY)) { return
         * SpanAdapter.getNamedEntityAdapter(); } else if
         * (name.equals(AnnotationTypeConstant.DEPENDENCY)) { return
         * ArcAdapter.getDependencyAdapter(); } else if
         * (name.equals(AnnotationTypeConstant.COREFERENCE)) { return
         * ChainAdapter.getCoreferenceChainAdapter(); } else if
         * (name.equals(AnnotationTypeConstant.COREFRELTYPE)) { return
         * ChainAdapter.getCoreferenceLinkAdapter(); }
         */
        else {
            throw new IllegalArgumentException("No adapter for type with name [" + name + "]");
        }

    }

    /**
     * Get the annotation type, using the request sent from brat. If the request have type POS_NN,
     * the the annotation type is POS
     *
     * @param aType
     *            the type sent from brat annotation as request while annotating
     */
    public static String getLabelPrefix(String aType)
    {
        String annotationType;
        if (Character.isDigit(aType.charAt(0))) {
            annotationType = aType.substring(0, aType.indexOf("_") + 1).replaceAll("[0-9]+", "");
        }
        else {
            annotationType = aType.substring(0, aType.indexOf("_") + 1);
        }
        return annotationType;
    }

    /**
     * Get the annotation type the way it is used in Brat visualization page (PREFIX+Type), such as
     * (POS_+NN)
     */
    public static String getQualifiedLabel(Tag aSelectedTag)
    {
        String annotationType = "";
        if (aSelectedTag.getTagSet().getLayer().getName().equals(WebAnnoConst.POS)) {
            annotationType = WebAnnoConst.POS_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getLayer().getName()
                .equals(WebAnnoConst.DEPENDENCY)) {
            annotationType = WebAnnoConst.DEP_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getLayer().getName()
                .equals(WebAnnoConst.NAMEDENTITY)) {
            annotationType = WebAnnoConst.NAMEDENTITY_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getLayer().getName()
                .equals(WebAnnoConst.COREFRELTYPE)) {
            annotationType = WebAnnoConst.COREFRELTYPE_PREFIX + aSelectedTag.getName();
        }
        else if (aSelectedTag.getTagSet().getLayer().getName()
                .equals(WebAnnoConst.COREFERENCE)) {
            annotationType = WebAnnoConst.COREFERENCE_PREFIX + aSelectedTag.getName();
        }
        return annotationType;
    }

    /**
     * Get the annotation layer name for arc {@link AnnotationType} such as
     * {@link WebAnnoConst#DEPENDENCY} or {@link WebAnnoConst#COREFERENCE} based
     * on the origin span type. This is assumed that an arc is drawn only from single span type such
     * as from {@link POS}. For Free Annotation type, the method should be changed.
     */
    public static String getArcLayerName(String aPrefix)
    {
        String layer = "";
        if (aPrefix.equals(WebAnnoConst.POS_PREFIX)) {
            layer = WebAnnoConst.DEPENDENCY;
        }
        else if (aPrefix.equals(WebAnnoConst.COREFRELTYPE_PREFIX)) {
            layer = WebAnnoConst.COREFERENCE;
        }
        return layer;
    }
}
