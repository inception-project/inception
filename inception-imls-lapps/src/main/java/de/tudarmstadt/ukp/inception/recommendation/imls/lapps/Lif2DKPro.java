/*
 * Copyright 2019
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
package de.tudarmstadt.ukp.inception.recommendation.imls.lapps;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.uima.fit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.lappsgrid.discriminator.Discriminators;
import org.lappsgrid.serialization.lif.Annotation;
import org.lappsgrid.serialization.lif.Container;
import org.lappsgrid.serialization.lif.View;
import org.lappsgrid.vocabulary.Features;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.pos.POSUtils;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Paragraph;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.ROOT;

public class Lif2DKPro
{
    public void convert(Container aContainer, JCas aJCas)
    {
        aJCas.setDocumentLanguage(aContainer.getLanguage());
        aJCas.setDocumentText(aContainer.getText());

        View view = aContainer.getView(0);

        // Paragraph
        view.getAnnotations().stream()
            .filter(a -> Discriminators.Uri.PARAGRAPH.equals(a.getAtType()))
            .forEach(para -> {
                Paragraph paraAnno = new Paragraph(aJCas, para.getStart().intValue(),
                        para.getEnd().intValue());
                paraAnno.addToIndexes();
            });

        // Sentence
        view.getAnnotations().stream()
            .filter(a -> Discriminators.Uri.SENTENCE.equals(a.getAtType()))
            .forEach(sent -> {
                Sentence sentAnno = new Sentence(aJCas, sent.getStart().intValue(),
                        sent.getEnd().intValue());
                sentAnno.addToIndexes();
            });

        Map<String, Token> tokenIdx = new HashMap<>();
        
        // Token, POS, Lemma
        view.getAnnotations().stream()
            .filter(a -> Discriminators.Uri.TOKEN.equals(a.getAtType()))
            .forEach(token -> {
                Token tokenAnno = new Token(aJCas, token.getStart().intValue(), token
                        .getEnd().intValue());
                String pos = token.getFeature(Features.Token.POS);
                String lemma = token.getFeature(Features.Token.LEMMA);

                if (isNotEmpty(pos)) {
                    POS posAnno = new POS(aJCas, tokenAnno.getBegin(), tokenAnno.getEnd());
                    posAnno.setPosValue(pos != null ? pos.intern() : null);
                    POSUtils.assignCoarseValue(posAnno);
                    posAnno.addToIndexes();
                    tokenAnno.setPos(posAnno);
                }

                if (isNotEmpty(lemma)) {
                    Lemma lemmaAnno = new Lemma(aJCas, tokenAnno.getBegin(), tokenAnno.getEnd());
                    lemmaAnno.setValue(lemma);
                    lemmaAnno.addToIndexes();
                    tokenAnno.setLemma(lemmaAnno);
                }

                tokenAnno.addToIndexes();
                tokenIdx.put(token.getId(), tokenAnno);
            });

        // NamedEntity
        view.getAnnotations().stream()
            .filter(a -> isNamedEntity(a.getAtType()))
            .forEach(ne -> {
                NamedEntity neAnno = new NamedEntity(aJCas, ne.getStart().intValue(),
                        ne.getEnd().intValue());
                neAnno.setValue(ne.getLabel());
                neAnno.addToIndexes();
            });
        
        // Dependencies
        view.getAnnotations().stream()
            .filter(a -> Discriminators.Uri.DEPENDENCY.equals(a.getAtType()))
            .forEach(dep -> {
                String dependent = dep.getFeature(Features.Dependency.DEPENDENT);
                String governor = dep.getFeature(Features.Dependency.GOVERNOR);
                
                if (isEmpty(governor) || governor.equals(dependent)) {
                    ROOT depAnno = new ROOT(aJCas);
                    depAnno.setDependencyType(dep.getLabel());
                    depAnno.setDependent(tokenIdx.get(dependent));
                    depAnno.setGovernor(tokenIdx.get(dependent));
                    depAnno.setBegin(depAnno.getDependent().getBegin());
                    depAnno.setEnd(depAnno.getDependent().getEnd());
                    depAnno.addToIndexes();
                }
                else {
                    Dependency depAnno = new Dependency(aJCas);
                    depAnno.setDependencyType(dep.getLabel());
                    depAnno.setDependent(tokenIdx.get(dependent));
                    depAnno.setGovernor(tokenIdx.get(governor));
                    depAnno.setBegin(depAnno.getDependent().getBegin());
                    depAnno.setEnd(depAnno.getDependent().getEnd());
                    depAnno.addToIndexes();
                }
            });
        
        // Constituents
        view.getAnnotations().stream()
            .filter(a -> Discriminators.Uri.PHRASE_STRUCTURE.equals(a.getAtType()))
            .forEach(ps -> {
                String rootId = findRoot(view, ps);
                // Get the constituent IDs
                Set<String> constituentIDs;
                constituentIDs = new HashSet<>(
                        getSetFeature(ps,Features.PhraseStructure.CONSTITUENTS));
                
                List<Annotation> constituents = new ArrayList<>();
                Map<String, Constituent> constituentIdx = new HashMap<>();

                // Instantiate all the constituents
                view.getAnnotations().stream()
                    .filter(a -> constituentIDs.contains(a.getId()))
                    .forEach(con -> {
                        if (Discriminators.Uri.CONSTITUENT.equals(con.getAtType())) {
                            Constituent conAnno;
                            if (rootId.equals(con.getId())) {
                                conAnno = new de.tudarmstadt.ukp.dkpro.core.api.syntax.type.
                                        constituent.ROOT(aJCas);
                            }
                            else {
                                conAnno = new Constituent(aJCas);
                            }
                            if (con.getStart() != null) {
                                conAnno.setBegin(con.getStart().intValue());
                            }
                            if (con.getEnd() != null) {
                                conAnno.setEnd(con.getEnd().intValue());
                            }
                            conAnno.setConstituentType(con.getLabel());
                            constituentIdx.put(con.getId(), conAnno);
                            constituents.add(con);
                        }
                        // If it is not a constituent, it must be a token ID - we already
                        // have created the tokens and recorded them in the tokenIdx
                    });
                
                // Set parent and children features
                constituents.forEach(con -> {
                    // Check if it is a constituent or token
                    Constituent conAnno = constituentIdx.get(con.getId());
                    Set<String> childIDs = getSetFeature(con, 
                            Features.Constituent.CHILDREN);
                    
                    List<org.apache.uima.jcas.tcas.Annotation> children = new ArrayList<>();
                    childIDs.forEach(childID -> {
                        Constituent conChild = constituentIdx.get(childID);
                        Token tokenChild = tokenIdx.get(childID);
                        if (conChild != null && tokenChild == null) {
                            conChild.setParent(conAnno);
                            children.add(conChild);
                        }
                        else if (conChild == null && tokenChild != null) {
                            tokenChild.setParent(conAnno);
                            children.add(tokenChild);
                        }
                        else if (conChild == null && tokenChild == null) {
                            throw new IllegalStateException("ID [" + con.getId()
                                    + "] not found");
                        }
                        else {
                            throw new IllegalStateException("ID [" + con.getId()
                                    + "] is constituent AND token? Impossible!");
                        }
                    });
                    
                    conAnno.setChildren(FSCollectionFactory.createFSArray(aJCas, children));
                });
                
                // Percolate offsets - they might not have been set on the constituents!
                Constituent root = constituentIdx.get(rootId);
                percolateOffsets(root);
                
                // Add to indexes
                constituentIdx.values().forEach(conAnno -> {
                    conAnno.addToIndexes();
                });
            });
    }

    @SuppressWarnings("unchecked")
    private <T> Set<T> getSetFeature(Annotation aAnnotation, String aName)
    {
        return aAnnotation.getFeatureSet(aName);
    }
    
    private void percolateOffsets(org.apache.uima.jcas.tcas.Annotation aNode)
    {
        if (aNode instanceof Constituent) {
            Constituent conAnno = (Constituent) aNode;
            int begin = Integer.MAX_VALUE;
            int end = 0;
            for (org.apache.uima.jcas.tcas.Annotation a : select(conAnno.getChildren(),
                    org.apache.uima.jcas.tcas.Annotation.class)) {
                percolateOffsets(a);
                
                begin = Math.min(a.getBegin(), begin);
                end = Math.max(a.getEnd(), end);
            }
            
            if (aNode.getBegin() != 0) {
                assert begin == aNode.getBegin();
            }
            else {
                aNode.setBegin(begin);
            }

            if (aNode.getEnd() != 0) {
                assert end == aNode.getEnd();
            }
            else {
                aNode.setEnd(end);
            }
        }
    }
    
    private String findRoot(View aView, Annotation aPS)
    {
        // Get all the constituents int he phrase structure
        Set<String> constituents = new HashSet<>(
                getSetFeature(aPS, Features.PhraseStructure.CONSTITUENTS));

        List<Annotation> psConstituents = aView.getAnnotations().stream()
                .filter(a -> Discriminators.Uri.CONSTITUENT.equals(a.getAtType()))
                .filter(con -> constituents.contains(con.getId()))
                .collect(Collectors.toList());
        
        // Remove all constituents that are children of other constituents within the PS
        psConstituents.forEach(con -> {
            Set<String> children = getSetFeature(con, Features.Constituent.CHILDREN);
            children.forEach(child -> constituents.remove(child));
        });
        
        // If all went well, only one constituent should be left and that is the root constituent
        assert 1 == constituents.size();
        
        // Return the ID of the root constituent
        return constituents.iterator().next();
    }

    private boolean isNamedEntity(String aTypeName) {
        return Discriminators.Uri.NE.equals(aTypeName) ||
               Discriminators.Uri.DATE.equals(aTypeName) ||
               Discriminators.Uri.LOCATION.equals(aTypeName) ||
               Discriminators.Uri.ORGANIZATION.equals(aTypeName) ||
               Discriminators.Uri.PERSON.equals(aTypeName);
    }
}
