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
package de.tudarmstadt.ukp.clarin.webanno.model;

import de.tudarmstadt.ukp.inception.support.db.PersistentEnum;

/**
 * Annotation overlap mode.
 * <p>
 * For span layers, overlap is defined in terms of the span offsets. If any character offset that is
 * part of span A is also part of span B, then they are considered to be <b>overlapping</b>. If two
 * spans have exactly the same offsets, then they are considered to be <b>stacking</b>. Note that in
 * UIMA spans are half-open intervals [begin,end), such that a begin offset which is equal to the
 * end offset of another span does not entail that the two spans are overlapping - they are
 * adjacent.
 * </p>
 * <p>
 * For relation layers, overlap is defined in terms of the end points of the relation. If two
 * relations share any end point (source or target), they are considered to be <b>overlapping</b>.
 * If two relations have exactly the same end points, they are considered to be <b>stacking</b>.
 * </p>
 */
public enum OverlapMode
    implements PersistentEnum
{
    /**
     * No overlap.
     * <p>
     * For span layers (the examples below do <b>not</b> include all possible overlapping cases):
     * </p>
     * 
     * <pre>
     * <code>
     * OK:  AAAA
     *          BBBB
     *      
     * BAD: AAAA
     *      BBBB
     *      
     * BAD:   AAAA
     *      BBBB
     *      
     * BAD: AAAA
     *        BBBB
     * 
     * BAD: AAAA
     *       BB
     * </code>
     * </pre>
     * <p>
     * For relation layers (the examples below do <b>not</b> include all possible overlapping
     * cases):
     * </p>
     * 
     * <pre>
     * <code>
     * OK:   
     *        ┏━━ A ━━┓
     *       XXXX [XXX] XXXX
     *       
     *                        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX XXXX [XXX] XXXX
     *
     * BAD:   
     *        ┏━━ A ━━┓
     *       XXXX [XXX] XXXX
     *       
     *        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX
     *
     * BAD:   
     *        ┏ A ┓
     *       XXXX XXXX [XXX]
     *       
     *        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX
     * </code>
     * </pre>
     */
    NO_OVERLAP("none"),

    /**
     * Stacking only - overlapping annotations must be at the exactly the same position.
     * <p>
     * For span layers (the examples below do <b>not</b> include all possible overlapping cases):
     * </p>
     * 
     * <pre>
     * <code>
     * OK:  AAAA
     *      BBBB
     *      
     * OK:  AAAA
     *          BBBB
     *      
     * BAD:   AAAA
     *      BBBB
     *      
     * BAD: AAAA
     *        BBBB
     * 
     * BAD: AAAA
     *       BB
     * </code>
     * </pre>
     * 
     * <p>
     * For relation layers (the examples below do <b>not</b> include all possible overlapping
     * cases):
     * </p>
     * 
     * <pre>
     * <code>
     * OK:   
     *        ┏━━ A ━━┓
     *       XXXX [XXX] XXXX
     *       
     *        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX
     *
     * BAD:   
     *        ┏ A ┓
     *       XXXX XXXX [XXX]
     *       
     *        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX
     * </code>
     * </pre>
     */
    STACKING_ONLY("stackingOnly"),

    /**
     * Overlap only - overlapping annotations must <b>not</b> be at the same position. For
     * {@link AnchoringMode#SINGLE_TOKEN}, this is equivalent to {@link #NO_OVERLAP} since
     * non-stacking overlaps are not possible.
     * <p>
     * For span layers (the examples below do <b>not</b> include all possible overlapping cases):
     * </p>
     * 
     * <pre>
     * <code>
     * OK:  AAAA
     *          BBBB
     *      
     * OK:    AAAA
     *      BBBB
     *      
     * OK:  AAAA
     *        BBBB
     * 
     * OK:  AAAA
     *       BB
     *       
     * BAD: AAAA
     *      BBBB
     * </code>
     * </pre>
     * 
     * <p>
     * For relation layers (the examples below do <b>not</b> include all possible overlapping
     * cases):
     * </p>
     * 
     * <pre>
     * <code>
     * OK:    ┏ A ┓
     *       XXXX XXXX [XXX]
     *       
     *        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX
     *       
     * OK:          ┏ A ┓
     *       [XXX] XXXX XXXX 
     *       
     *        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX
     *       
     * BAD:   ┏━━ A ━━┓
     *       XXXX [XXX] XXXX
     *       
     *        ┏━━ B ━━┓
     *       XXXX [XXX] XXXX
     * </code>
     * </pre>
     */
    OVERLAP_ONLY("overlapOnly"),

    /**
     * Any overlap - any kind of overlapping annotations is permitted. For
     * {@link AnchoringMode#SINGLE_TOKEN}, this is equivalent to {@link #STACKING_ONLY} since
     * non-stacking overlaps are not possible.
     */
    ANY_OVERLAP("any");

    public static final OverlapMode DEFAULT_OVERLAP_MODE = OverlapMode.NO_OVERLAP;

    private final String id;

    OverlapMode(String aId)
    {
        id = aId;
    }

    @Override
    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return getId();
    }

    @Override
    public String toString()
    {
        return getId();
    }
}
