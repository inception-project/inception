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
package org.dkpro.core.io.nif.internal;

/**
 * NIF vocabulary.
 * <p>
 * JavaDoc in this class was sourced from the NIF 2.0 and 2.1 Core Ontologies and which are licensed
 * under Apache 2.0 (http://www.apache.org/licenses/LICENSE-2.0) and CC-BY
 * (http://creativecommons.org/licenses/by/3.0/).
 * 
 * @see <a href="http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html">NIF
 *      2.0 Core Ontology</a>
 */
public class NIF
{
    public static final String PREFIX_NIF = "nif";

    public static final String NS_NIF = "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#";

    /**
     * The begin index of a character range as defined in
     * <a href="http://tools.ietf.org/html/rfc5147#section-2.2.1">RFC 5147 Section 2.2.1</a> and
     * <a href="http://tools.ietf.org/html/rfc5147#section-2.2.2">RFC 5147 Section 2.2.2</a>,
     * measured as the gap between two characters, starting to count from 0 (the position before the
     * first character of a text).
     */
    public static final String PROP_BEGIN_INDEX = NS_NIF + "beginIndex";

    /**
     * The end index of a character range as defined in
     * <a href="http://tools.ietf.org/html/rfc5147#section-2.2.1">RFC 5147 Section 2.2.1</a> and
     * <a href="http://tools.ietf.org/html/rfc5147#section-2.2.2">RFC 5147 Section 2.2.2</a>,
     * measured as the gap between two characters, starting to count from 0 (the position before the
     * first character of a text).
     */
    public static final String PROP_END_INDEX = NS_NIF + "endIndex";

    /**
     * Links a URI of a string to its reference context of type nif:Context. The reference context
     * determines the calculation of begin and end index
     * 
     * Each String that is not an instance of nif:Context MUST have exactly one reference context.
     */
    public static final String PROP_REFERENCE_CONTEXT = NS_NIF + "referenceContext";

    /**
     * The reference text as rdf:Literal for this nif:Context resource.
     * 
     * NIF requires that the reference text (i.e. the context) is always included in the RDF as an
     * rdf:Literal.
     * 
     * Note, that the isString property is <b>the</b> place to keep the string itself in RDF.
     * 
     * All other nif:Strings and nif:URISchemes relate to the text of this property to calculate
     * character position and indices.
     */
    public static final String PROP_IS_STRING = NS_NIF + "isString";

    /**
     * The string, which the URI is representing as an RDF Literal. Some use cases require this
     * property, as it is necessary for certain sparql queries.
     */
    public static final String PROP_ANCHOR_OF = NS_NIF + "anchorOf";

    /**
     * This property links sentences to their words.
     */
    public static final String PROP_WORD = NS_NIF + "word";

    /**
     * See nif:nextSentence
     */
    public static final String PROP_NEXT_WORD = NS_NIF + "nextWord";

    /**
     * see nif:nextSentence
     */
    public static final String PROP_PREVIOUS_WORD = NS_NIF + "previousWord";

    /**
     * This property links words and other structures to their sentence.
     */
    public static final String PROP_SENTENCE = NS_NIF + "sentence";

    /**
     * This property (and nif:previousSentence, nif:nextWord, nif:previousWord and their transitive
     * extension) can be used to make resources of nif:Sentence and nif:Word traversable, it can not
     * be assumed that no gaps or whitespaces between sentences or words exist, i.e. string
     * adjacency is not mandatory. The transitivity axioms are included in nif-core-inf.ttl and need
     * to be included separately to keep a low reasoning profile. They are modeled after
     * skos:broader and skos:broaderTransitive.
     */
    public static final String PROP_NEXT_SENTENCE = NS_NIF + "nextSentence";

    /**
     * see nif:nextSentence
     */
    public static final String PROP_PREVIOUS_SENTENCE = NS_NIF + "previousSentence";

    /**
     * The lemma(s) of the nif:String.
     */
    public static final String PROP_LEMMA = NS_NIF + "lemma";

    /**
     * The stem(s) of the nif:String.
     */
    public static final String PROP_STEM = NS_NIF + "stem";

    /**
     * To include the pos tag as it comes out of the NLP tool as RDF Literal. This property is
     * discouraged to use alone, please use oliaLink and oliaCategory. We included it, because some
     * people might still want it and will even create their own property, if the string variant is
     * missing.
     * 
     * @deprecated Use oliaLink and oliaCategory.
     */
    @Deprecated
    public static final String PROP_POS_TAG = NS_NIF + "posTag";

    /**
     * The confidence of an annotation as decimal between 0 and 1.
     */
    public static final String PROP_CONFIDENCE = NS_NIF + "confidence";

    /**
     * This property marks the most specific class from itsrdf:taClassRef. The rule is: from the set
     * S of itsrdf:taClassRef attached to this resource taMscRef points to the one that does not
     * have any subclasses in the set S except itself. So if taClassRef is owl:Thing, dbo:Agent,
     * dbo:Person, dbp:Actor taMsClassRef is dbo:Actor
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e1206">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String PROP_TA_MS_CLASS_REF = NS_NIF + "taMsClassRef";

    /**
     * A title within a text.
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e339">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_TITLE = NS_NIF + "Title";

    /**
     * A paragraph.
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e209">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_PARAGRAPH = NS_NIF + "Paragraph";

    /**
     * The Word class represents strings that are tokens or words. A string is a Word, if it is a
     * word. We don't nitpic about whether it is a a pronoun, a name, a punctuation mark or an
     * apostrophe or whether it is separated by white space from another Word or something else. The
     * string 'He enters the room.' for example has 5 words. Words are assigned by a tokenizer NIF
     * Implementation. Single word phrases might be tagged as nif:Word and nif:Phrase.
     * 
     * Example 1: "The White House" are three Words separated by whitespace
     * 
     * Comment 1: We adopted the definition style from foaf:Person, see here:
     * http://xmlns.com/foaf/spec/#term_Person We are well aware that the world out there is much
     * more complicated, but we are ignorant about it, for the following reasons:
     * 
     * Comment 2:
     * 
     * 1. NIF has a client-server and the client has the ability to dictate the tokenization to the
     * server (i.e. the NIF Implementation) by sending properly tokenized NIF annotated with
     * nif:Word. All NIF Implementations are supposed to honor and respect the current assignment of
     * the Word class. Thus the client should decide which NIF Implementation should create the
     * tokenization. Therefore this class is not descriptive, but prescriptive.
     * 
     * 2. The client may choose to send an existing tokenization to a NIF Implementation, with the
     * capability to change (for better or for worse) the tokenization.
     * 
     * The class has not been named 'Token' as the NLP definition of 'token' is descriptive (and not
     * well-defined), while the assignment of what is a Word and what not is prescriptive, e.g.
     * "can't" could be described as one, two or three tokens or defined as being one, two or three
     * words. For further reading, we refer the reader to: By all these lovely tokens... Merging
     * conflicting tokenizations by Christian Chiarcos, Julia Ritz, and Manfred Stede. Language
     * Resources and Evaluation 46(1):53-74 (2012) or the short form:
     * http://www.aclweb.org/anthology/W09-3005
     * 
     * There the task at hand is to merge two tokenization T_1 and T_2 which is normally not the
     * case in the NIF world as tokenization is prescribed, i.e. given as a baseline (Note that this
     * ideal state might not be achieved by all implementations.)
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e367">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_WORD = NS_NIF + "Word";

    /**
     * A sentence.
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e251">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_SENTENCE = NS_NIF + "Sentence";

    /**
     * The string that serves as a context for its substrings. The Unicode String given in the
     * nif:isString property must be used to calculate the begin and endIndex for all nif:Strings
     * that have a nif:referenceContext property to this URI. For further information, see
     * http://svn.aksw.org/papers/2013/ISWC_NIF/public.pdf
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e125">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_CONTEXT = NS_NIF + "Context";

    /**
     * Individuals of this class are a string, i.e. Unicode characters, who have been given a URI
     * and are used in the subject of an RDF statement.
     * 
     * This class is abstract and should not be serialized.
     * 
     * NIF-Stanbol (nif-stanbol.ttl):
     * 
     * subclassOf nifs:Annotation because it "annotates" strings for example with begin and end
     * index. The class is similar to fise:TextAnnotation
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e265">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_STRING = NS_NIF + "String";

    /**
     * A nif:Phrase can be a nif:String, that is a chunk of several words or a word itself (e.g. a
     * NounPhrase as a Named Entity). The term is underspecified and can be compatible with many
     * defintitions of phrase. Please subClass it to specify the meaning (e.g. for Chunking or
     * Phrase Structure Grammar). Example: ((My dog)(also)(likes)(eating (sausage)))
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e223">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_PHRASE = NS_NIF + "Phrase";

    /**
     * cf. Linked-Data Aware URI Schemes for Referencing Text Fragments by Sebastian Hellmann, Jens
     * Lehmann und Sören Auer in EKAW 2012 http://jens-lehmann.org/files/2012/ekaw_nif.pdf
     * 
     * requires the existence of begin, endIndex and referenceContext
     * 
     * @see <a href=
     *      "https://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core/nif-core.html#d4e195">NIF
     *      2.0 Core Ontology</a>
     */
    public static final String TYPE_OFFSET_BASED_STRING = NS_NIF + "OffsetBasedString";

    /**
     * Text span annotation denoting that a word or phrase has been detected as occurrence of a
     * named entity. (Use this without further annotation property assertions if you just want to
     * express the detection of the occurrence when neither the mentioned entity nor its category
     * was identified.)
     * 
     * @see <a href= "https://github.com/NLP2RDF/ontologies/blob/master/nif-core/nif-core.ttl">NIF
     *      2.1 Core Ontology</a>
     */
    public static final String TYPE_ENTITY_OCCURRENCE = NS_NIF + "EntityOccurrence";
}
