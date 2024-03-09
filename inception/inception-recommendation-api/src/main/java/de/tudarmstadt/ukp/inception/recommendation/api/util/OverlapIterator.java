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
package de.tudarmstadt.ukp.inception.recommendation.api.util;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;

public class OverlapIterator
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Iterator<Offset> ia;
    private final Iterator<Offset> ib;

    private List<Offset> openB;
    private List<Offset> nextOpenB;
    private ListIterator<Offset> iob;

    private Offset a;
    private Offset b;

    private boolean done;

    public OverlapIterator(List<Offset> aList, List<Offset> bList)
    {
        ia = aList.iterator();
        ib = bList.iterator();
        openB = Collections.emptyList();
        nextOpenB = new ArrayList<>();
        iob = openB.listIterator();
        done = aList.isEmpty() || bList.isEmpty();
        step();
    }

    private void stepA()
    {
        LOG.trace("Stepping A");

        if (!ia.hasNext()) {
            done = true;
            return;
        }
        a = ia.next();

        b = null;
        // When moving to the next A, we can forget all open intervals
        // that end before the new A
        openB = nextOpenB;
        var beforePrune = openB.size();
        openB.removeIf(o -> o.getEnd() < a.getBegin());
        LOG.trace("Reset B (open {}, {} pruned)", openB.size(), beforePrune - openB.size());
        iob = openB.listIterator();
        nextOpenB = new ArrayList<>();
    }

    private void stepB()
    {
        if (iob.hasNext()) {
            // Step to the next B from open intervals list
            if (LOG.isTraceEnabled()) {
                LOG.trace("Stepping B from open Bs: {}", iob.nextIndex());
            }
            b = iob.next();
        }
        else if (ib.hasNext()) {
            // Step to the next B from the source list
            LOG.trace("Stepping B from source Bs");
            b = ib.next();
            if (a != null && b.getBegin() > a.getEnd()) {
                // All remaining b's are beyond the end of the a's and won't overlap
                a = null;
            }
        }
        else {
            // Prepare to step to the next A
            a = null;
        }
    }

    private void step()
    {
        while (!done) {
            if (a == null) {
                stepA();
            }

            stepB();

            if (b != null) {
                // openB is pruned in stepA
                nextOpenB.add(b);
            }

            if (a != null && b != null && a.overlaps(b)) {
                LOG.trace("Found overlap {} {}", a, b);
                // next() should return this combo
                break;
            }
        }
    }

    public boolean hasNext()
    {
        return !done;
    }

    public Pair<Offset, Offset> next()
    {
        var result = Pair.of(a, b);
        step();
        return result;
    }
}
