

/* First created by JCasGen Thu Jun 27 18:20:37 CEST 2013 */
package de.tudarmstadt.ukp.dkpro.core.api.metadata.type;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.cas.TOP;


/** Description of an individual tag.
 * Updated by JCasGen Thu Jun 27 18:20:37 CEST 2013
 * XML source: /Users/bluefire/UKP/Workspaces/dkpro-juno/de.tudarmstadt.ukp.clarin.webanno-trunk/de.tudarmstadt.ukp.clarin.webanno.uima/src/main/resources/desc/type/coref.xml
 * @generated */
public class TagDescription extends TOP {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(TagDescription.class);
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
  protected TagDescription() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public TagDescription(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public TagDescription(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: name

  /** getter for name - gets The name of the tag.
   * @generated */
  public String getName() {
    if (TagDescription_Type.featOkTst && ((TagDescription_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagDescription");
    return jcasType.ll_cas.ll_getStringValue(addr, ((TagDescription_Type)jcasType).casFeatCode_name);}
    
  /** setter for name - sets The name of the tag. 
   * @generated */
  public void setName(String v) {
    if (TagDescription_Type.featOkTst && ((TagDescription_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.TagDescription");
    jcasType.ll_cas.ll_setStringValue(addr, ((TagDescription_Type)jcasType).casFeatCode_name, v);}    
  }

    