/*
 * Copyright (c) 2004-2009 Richard Eckart de Castilho.
 * 
 * This file was originally part of AnnoLab by the name DoubleIterator
 * The file was adapted to use Offset instead of Interval and to use SLF4J
 * instead of Commons Logging.
 *
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
package de.tudarmstadt.ukp.inception.recommendation.util;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;

public class OverlapIterator
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    /** The A/B lists */
    private final List<Offset> la;
    private final List<Offset> lb;

    /** A values we do not want to see again (after a rewind). */
    private final boolean[] ignorea;

    /** List iterators for the A/B lists */
    private final ListIterator<Offset> ia;
    private final ListIterator<Offset> ib;

    /** Indices of _cura/_curb within the lists */
    private int na;
    private int nb;

    /** Maximum A/B index within the lists */
    private final int maxa;
    private final int maxb;

    /** Current A/B item */
    private Offset cura;
    private Offset curb;

    private int last_b_step_na;
    private boolean done;

    private int stepCount;

    public OverlapIterator(final List<Offset> aList, final List<Offset> bList)
    {
        done = !((aList.size() > 0) && (bList.size() > 0));

        // Intialize A
        la = aList;
        maxa = la.size() - 1; // Up until here and no further
        ia = la.listIterator(); // Where we are now
        na = ia.nextIndex(); // Index of _cura within _la
        cura = ia.next(); // The current object.
        ignorea = new boolean[la.size()];

        // Initialize B
        lb = bList;
        maxb = lb.size() - 1;
        ib = lb.listIterator();
        nb = ib.nextIndex();
        curb = ib.next();

        last_b_step_na = na;
    }

    public int getStepCount()
    {
        return stepCount;
    }

    public Offset getA()
    {
        return cura;
    }

    public Offset getB()
    {
        return curb;
    }

    public void ignoraA()
    {
        ignorea[na] = true;
    }

    public boolean hasNext()
    {
        return !done;
    }

    public void step()
    {
        if (done) {
            throw new NoSuchElementException();
        }

        // Peek ahead in the A list.
        Offset nexta = null;
        if (na < maxa) {
            nexta = ia.next();
            ia.previous();
        }

        final boolean nexta_starts_before_curb_ends = (nexta != null)
                && (nexta.getBegin() <= curb.getEnd());
        final boolean cura_ends_before_or_with_curb = cura.getEnd() <= curb.getEnd();

        if (log.isTraceEnabled()) {
            log.trace("---");
            log.trace("   A                            : " + na + "/" + maxa + " " + cura
                    + " peek: " + nexta);
            log.trace("   B                            : " + nb + "/" + maxb + " " + curb);
            log.trace("   nexta starts before curb ends: " + nexta_starts_before_curb_ends);
            log.trace("   cura ends before or with curb: " + cura_ends_before_or_with_curb);
        }

        // Which one to step up A or B?
        if (nexta_starts_before_curb_ends || cura_ends_before_or_with_curb) {
            // Can A be stepped up any more?
            if (na < maxa) {
                stepA();
                // if not, try stepping up B
            }
            else if (nb < maxb) {
                stepB();
                // if both are at the end, bail out
            }
            else {
                done = true;
            }
        }
        else {
            // Can B be stepped up any more?
            if (nb < maxb) {
                stepB();
                // if not, try stepping up A
            }
            else if (na < maxa) {
                stepA();
                // if both are at the end, bail out
            }
            else {
                done = true;
            }
        }

        if (log.isTraceEnabled() && done) {
            log.trace("   -> Both lists at the end.");
        }
    }

    private void stepA()
    {
        stepCount++;
        na = ia.nextIndex();
        cura = ia.next();

        if (log.isTraceEnabled()) {
            log.trace("   -> A: " + na + "/" + maxa + " " + cura);
        }
    }

    private void stepBackA()
    {
        na = ia.previousIndex();
        cura = ia.previous();

        if (log.isTraceEnabled()) {
            log.trace("   <- A: " + na + "/" + maxa + " " + cura);
        }
    }

    private void stepB()
    {
        stepCount++;
        nb = ib.nextIndex();
        curb = ib.next();

        if (log.isTraceEnabled()) {
            log.trace("   -> B: " + nb + "/" + maxb + " " + curb);
        }

        if (curb.getBegin() < cura.getEnd()) {
            // Rewind A to the point where it was when we last stepped
            // up B.
            rewindA();
        }
        else {
            last_b_step_na = na;
        }
    }

    private void rewindA()
    {
        if (log.isTraceEnabled()) {
            log.trace("   <- rewinding A");
        }

        // Seek back to the first segment that does not overlap
        // with curb and at most until the last b step we made.
        boolean steppedBack = false;
        while ((na > last_b_step_na) && (cura.getEnd() > curb.getBegin())) {
            stepBackA();
            steppedBack = true;
        }

        // Correct pointer
        if (steppedBack) {
            // Make sure the next peek really peeks ahead.
            na = ia.nextIndex();
            cura = ia.next();
        }

        // Skip over the A's we do not want to see again.
        while (ignorea[na] && (na < maxa)) {
            stepA();
        }

        // If we skipped some As those we skip will always be skipped, so we
        // can as well update the _last_b_step_na so we don't have to skip them
        // every time.
        last_b_step_na = na;
    }
}
