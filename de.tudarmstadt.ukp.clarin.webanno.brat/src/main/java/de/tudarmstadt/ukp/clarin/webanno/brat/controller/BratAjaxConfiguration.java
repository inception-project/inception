/*******************************************************************************
 * Copyright 2012
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.EntityType;
import de.tudarmstadt.ukp.clarin.webanno.brat.display.model.RelationType;
import de.tudarmstadt.ukp.clarin.webanno.model.Tag;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceChain;
import de.tudarmstadt.ukp.dkpro.core.api.coref.type.CoreferenceLink;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;

/**
 *
 * Configuring The brat JSON collection information responses getting tags from DB
 *
 * @author Seid Muhie Yimam
 *
 */
public class BratAjaxConfiguration
{

    /**
     * Get all the tags from the database, differentiate as pos,dependency, named entity... and
     * build the entity/relation types accordingly.
     * {@link de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS} is an {@link EntityType}
     * where {@link Dependency} is a child {@link EntityType} ({@code arc}). {@link NamedEntity} is
     * an {@link EntityType} without a child (no {@code arc}). {@link CoreferenceChain} is
     * {@link EntityType} with a child {@link CoreferenceLink}s.
     *
     * @return {@link Set<{@link EntityType }>}
     */
    public Set<EntityType> configureVisualizationAndAnnotation(List<Tag> aTags)
    {
        Set<EntityType> entityTypes = new HashSet<EntityType>();

        List<String> poses = new ArrayList<String>();

        List<String> dependency = new ArrayList<String>();

        List<String> namedEntity = new ArrayList<String>();

        List<String> corefRelType = new ArrayList<String>();

        List<String> coreference = new ArrayList<String>();

        for (Tag tag : aTags) {
            if (tag.getTagSet().getType().getName().equals(AnnotationTypeConstant.POS)) {
                poses.add(tag.getName());
            }
            else if (tag.getTagSet().getType().getName().equals(AnnotationTypeConstant.DEPENDENCY)) {
                dependency.add(tag.getName());
            }
            else if (tag.getTagSet().getType().getName().equals(AnnotationTypeConstant.NAMEDENTITY)) {
                namedEntity.add(tag.getName());
            }
            else if (tag.getTagSet().getType().getName().equals(AnnotationTypeConstant.COREFRELTYPE)) {
                corefRelType.add(tag.getName());
            }
            else if (tag.getTagSet().getType().getName().equals(AnnotationTypeConstant.COREFERENCE)) {
                coreference.add(tag.getName());
            }

        }

        Collections.sort(poses);
        Collections.sort(dependency);
        Collections.sort(namedEntity);
        Collections.sort(coreference);
        Collections.sort(corefRelType);

        List<EntityType> posChildren = getChildren(AnnotationTypeConstant.POS_PREFIX, poses, dependency,
                "red", "yellow", "blue", "green");
        EntityType posType = new EntityType(AnnotationTypeConstant.POS_PARENT, AnnotationTypeConstant.POS_PARENT,
                true, "", "red", "blue", "blue", new ArrayList<String>(), posChildren,
                new ArrayList<String>(), new ArrayList<RelationType>());

        if (poses.size() > 0) {
            entityTypes.add(posType);
        }



            List<EntityType> corefChildren = getChildren(AnnotationTypeConstant.COREFERENCE_PREFIX,
                    corefRelType, coreference, "red", "blue", "blue", "");
            EntityType corefType = new EntityType(AnnotationTypeConstant.COREFERENCE_PARENT,
                    AnnotationTypeConstant.COREFERENCE_PARENT, true, "", "red", "blue", "blue",
                    new ArrayList<String>(), corefChildren, new ArrayList<String>(),
                    new ArrayList<RelationType>());
            if (corefRelType.size() > 0) {
                entityTypes.add(corefType);
            }


        List<EntityType> neChildren = getChildren(AnnotationTypeConstant.NAMEDENTITY_PREFIX, namedEntity,
                new ArrayList<String>(), "black", "cyan", "green", "");
        EntityType neEntityType = new EntityType(AnnotationTypeConstant.NAMEDENTITY_PARENT,
                AnnotationTypeConstant.NAMEDENTITY_PARENT, true, "", "black", "cyan", "green",
                new ArrayList<String>(), neChildren, new ArrayList<String>(),
                new ArrayList<RelationType>());

        if (namedEntity.size() > 0) {
            entityTypes.add(neEntityType);
        }

        return entityTypes;
    }

    /**
     * returns {@link EntityType} which will be used as a child {@link EntityType} which are
     * properties/labels of an arc
     *
     * @param spansList
     *            list of possible {@code spans } to be used as {@code targets}.
     * @param aArcList
     *            all {@code arc labels} used as label on the {@code arc}
     * @param aFgColor
     *            foreground color of the arc
     * @param aBgcolor
     *            background color of the arc
     * @param aBorderColoer
     *            border color of the arc
     * @return {@link List< {@link EntityType }>}
     */
    private List<EntityType> getChildren(String aPrefix, List<String> spansList,
            List<String> aArcList, String aFgColor, String aBgcolor, String aBorderColoer,
            String aArcColor)
    {
        List<EntityType> children = new ArrayList<EntityType>();

        List<String> arcTargets = new ArrayList<String>();
        for (String span : spansList) {
                arcTargets.add(aPrefix + span);
        }
        for (String span : spansList) {
            List<RelationType> arcs = new ArrayList<RelationType>();
            Iterator<String> arcTypesResultIterator = aArcList.iterator();
            while (arcTypesResultIterator.hasNext()) {
                String arcLabels = arcTypesResultIterator.next();

                // 12 classes of colors to differentiate Co-reference chains
                if(aPrefix.equals(AnnotationTypeConstant.COREFERENCE_PREFIX)){

                String[] colors = new String[] { "#00FF00", "#0000A0", "#FF0000", "#800080 ", "#F000FF",
                      "#00FFFF ", "#FF00FF ", "#8D38C9", "#8D38C9", "#736AFF", "#C11B17", "#800000" };
                int i = 1;
                for(String color:colors){
                RelationType arc = new RelationType(color, "triangle,5",
                        Arrays.asList(arcLabels), i+aPrefix + arcLabels, arcTargets, "");
                arcs.add(arc);
                i++;
                }
                }
                else{
                    RelationType arc = new RelationType(aArcColor, "triangle,5",
                            Arrays.asList(arcLabels), aPrefix + arcLabels, arcTargets, "");
                    arcs.add(arc);
                }
            }

            EntityType entityTpe = new EntityType(span, aPrefix + span, false, "", aFgColor,
                    aBgcolor, aBorderColoer, Arrays.asList(span), new ArrayList<EntityType>(),
                    new ArrayList<String>(), arcs);
            children.add(entityTpe);
        }
        return children;
    }

}
