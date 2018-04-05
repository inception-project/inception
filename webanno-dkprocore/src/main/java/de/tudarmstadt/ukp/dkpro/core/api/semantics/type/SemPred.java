/*
 * Copyright 2017
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
// CHECKSTYLE:OFF

/* First created by JCasGen Wed Nov 15 10:30:01 CET 2017 */
package de.tudarmstadt.ukp.dkpro.core.api.semantics.type;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.TOP_Type;
import org.apache.uima.jcas.tcas.Annotation;


/** One of the predicates of a sentence (often a main verb, but nouns and adjectives can also be predicates). 
The SemPred annotation can be attached to predicates in a sentence.
Semantic predicates express events or situations and take semantic arguments
expressing the participants in these events or situations. All forms of main verbs
can be annotated with a SemPred. However, there are also many nouns and
adjectives that take arguments and can thus be annotated with a SemanticPredicate,
e.g. event nouns, such as "suggestion" (with arguments what and by whom), or
relational adjectives, such as "proud" (with arguments who and of what).
 * Updated by JCasGen Wed Nov 15 10:30:01 CET 2017
 * XML source: /Users/bluefire/git/webanno/webanno-dkprocore/src/main/resources/desc/type/Backports.xml
 * @generated */
public class SemPred extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(SemPred.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected SemPred() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public SemPred(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public SemPred(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public SemPred(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: arguments

  /** getter for arguments - gets The predicate's arguments.
   * @generated
   * @return value of the feature 
   */
  public FSArray getArguments() {
    if (SemPred_Type.featOkTst && ((SemPred_Type)jcasType).casFeat_arguments == null) {
        jcasType.jcas.throwFeatMissing("arguments", "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred");
    }
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((SemPred_Type)jcasType).casFeatCode_arguments)));}
    
  /** setter for arguments - sets The predicate's arguments. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setArguments(FSArray v) {
    if (SemPred_Type.featOkTst && ((SemPred_Type)jcasType).casFeat_arguments == null) {
        jcasType.jcas.throwFeatMissing("arguments", "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred");
    }
    jcasType.ll_cas.ll_setRefValue(addr, ((SemPred_Type)jcasType).casFeatCode_arguments, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for arguments - gets an indexed value - The predicate's arguments.
   * @generated
   * @param i index in the array to get
   * @return value of the element at index i 
   */
  public SemArgLink getArguments(int i) {
    if (SemPred_Type.featOkTst && ((SemPred_Type)jcasType).casFeat_arguments == null) {
        jcasType.jcas.throwFeatMissing("arguments", "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred");
    }
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemPred_Type)jcasType).casFeatCode_arguments), i);
    return (SemArgLink)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemPred_Type)jcasType).casFeatCode_arguments), i)));}

  /** indexed setter for arguments - sets an indexed value - The predicate's arguments.
   * @generated
   * @param i index in the array to set
   * @param v value to set into the array 
   */
  public void setArguments(int i, SemArgLink v) { 
    if (SemPred_Type.featOkTst && ((SemPred_Type)jcasType).casFeat_arguments == null) {
        jcasType.jcas.throwFeatMissing("arguments", "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred");
    }
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((SemPred_Type)jcasType).casFeatCode_arguments), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((SemPred_Type)jcasType).casFeatCode_arguments), i, jcasType.ll_cas.ll_getFSRef(v));}
   
    
  //*--------------*
  //* Feature: category

  /** getter for category - gets A more detailed specification of the predicate type depending on the theory being used, e.g. a frame name.
   * @generated
   * @return value of the feature 
   */
  public String getCategory() {
    if (SemPred_Type.featOkTst && ((SemPred_Type)jcasType).casFeat_category == null) {
        jcasType.jcas.throwFeatMissing("category", "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred");
    }
    return jcasType.ll_cas.ll_getStringValue(addr, ((SemPred_Type)jcasType).casFeatCode_category);}
    
  /** setter for category - sets A more detailed specification of the predicate type depending on the theory being used, e.g. a frame name. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setCategory(String v) {
    if (SemPred_Type.featOkTst && ((SemPred_Type)jcasType).casFeat_category == null) {
        jcasType.jcas.throwFeatMissing("category", "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred");
    }
    jcasType.ll_cas.ll_setStringValue(addr, ((SemPred_Type)jcasType).casFeatCode_category, v);}    
  }
