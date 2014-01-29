

/* First created by JCasGen Thu Jun 27 18:20:37 CEST 2013 */
package de.tudarmstadt.ukp.dkpro.core.api.metadata.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;


/** Information about a tagset (controlled vocabulary).
 * Updated by JCasGen Thu Jun 27 18:20:37 CEST 2013
 * XML source: /Users/bluefire/UKP/Workspaces/dkpro-juno/de.tudarmstadt.ukp.clarin.webanno-trunk/de.tudarmstadt.ukp.clarin.webanno.uima/src/main/resources/desc/type/coref.xml
 * @generated */
public class TagsetDescription extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(TagsetDescription.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected TagsetDescription() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public TagsetDescription(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public TagsetDescription(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public TagsetDescription(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: layer

  /** getter for layer - gets The layer to which the tagset applies. This is
						typically the name of an UIMA type such as
						"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS".
   * @generated */
  public String getLayer() {
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_layer == null)
      jcasType.jcas.throwFeatMissing("layer", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_layer);}
    
  /** setter for layer - sets The layer to which the tagset applies. This is
						typically the name of an UIMA type such as
						"de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS". 
   * @generated */
  public void setLayer(String v) {
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_layer == null)
      jcasType.jcas.throwFeatMissing("layer", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    jcasType.ll_cas.ll_setStringValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_layer, v);}    
   
    
  //*--------------*
  //* Feature: name

  /** getter for name - gets The name of the tagset.
   * @generated */
  public String getName() {
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_name);}
    
  /** setter for name - sets The name of the tagset. 
   * @generated */
  public void setName(String v) {
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    jcasType.ll_cas.ll_setStringValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_name, v);}    
   
    
  //*--------------*
  //* Feature: tags

  /** getter for tags - gets Descriptions of the tags belonging to this tagset.
   * @generated */
  public FSArray getTags() {
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_tags == null)
      jcasType.jcas.throwFeatMissing("tags", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    return (FSArray)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_tags)));}
    
  /** setter for tags - sets Descriptions of the tags belonging to this tagset. 
   * @generated */
  public void setTags(FSArray v) {
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_tags == null)
      jcasType.jcas.throwFeatMissing("tags", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    jcasType.ll_cas.ll_setRefValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_tags, jcasType.ll_cas.ll_getFSRef(v));}    
    
  /** indexed getter for tags - gets an indexed value - Descriptions of the tags belonging to this tagset.
   * @generated */
  public TagDescription getTags(int i) {
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_tags == null)
      jcasType.jcas.throwFeatMissing("tags", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_tags), i);
    return (TagDescription)(jcasType.ll_cas.ll_getFSForRef(jcasType.ll_cas.ll_getRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_tags), i)));}

  /** indexed setter for tags - sets an indexed value - Descriptions of the tags belonging to this tagset.
   * @generated */
  public void setTags(int i, TagDescription v) { 
    if (TagsetDescription_Type.featOkTst && ((TagsetDescription_Type)jcasType).casFeat_tags == null)
      jcasType.jcas.throwFeatMissing("tags", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagsetDescription");
    jcasType.jcas.checkArrayBounds(jcasType.ll_cas.ll_getRefValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_tags), i);
    jcasType.ll_cas.ll_setRefArrayValue(jcasType.ll_cas.ll_getRefValue(addr, ((TagsetDescription_Type)jcasType).casFeatCode_tags), i, jcasType.ll_cas.ll_getFSRef(v));}
  }

    