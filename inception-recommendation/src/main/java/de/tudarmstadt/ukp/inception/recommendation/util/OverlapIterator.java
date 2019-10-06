/*
 * Copyright (c) 2004-2009 Richard Eckart de Castilho.
 * 
 * This file was originally part of AnnoLab by the name DoubleIterator
 * The file was adapted to use Offset instead of Interval and to use SLF4J
 * instead of Commons Logging.
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
package de.tudarmstadt.ukp.inception.recommendation.util;

import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;

public class OverlapIterator
{
    private final Logger l_log = LoggerFactory.getLogger(getClass());

    /** The A/B lists */
    private final List<Offset> l_la;
    private final List<Offset> l_lb;

    /** A values we do not want to see again (after a rewind). */
    private final boolean[] i_ignorea;

    /** List iterators for the A/B lists */
    private final ListIterator<Offset> l_ia;
    private final ListIterator<Offset> l_ib;

    /** Indices of _cura/_curb within the lists */
    private int n_na;
    private int n_nb;

    /** Maximum A/B index within the lists */
    private final int m_maxa;
    private final int m_maxb;

    /** Current A/B item */
    private Offset c_cura;
    private Offset c_curb;

    private int l_last_b_step_na;
    private boolean d_done;

    private int s_stepCount;

    public OverlapIterator(final List<Offset> la, final List<Offset> lb)
    {
        d_done = !((la.size() > 0) && (lb.size() > 0));

        // Intialize A
        l_la = la;
        m_maxa = l_la.size() - 1; // Up until here and no further
        l_ia = l_la.listIterator(); // Where we are now
        n_na = l_ia.nextIndex(); // Index of _cura within _la
        c_cura = l_ia.next(); // The current object.
        i_ignorea = new boolean[l_la.size()];

        // Initialize B
        l_lb = lb;
        m_maxb = l_lb.size() - 1;
        l_ib = l_lb.listIterator();
        n_nb = l_ib.nextIndex();
        c_curb = l_ib.next();

        l_last_b_step_na = n_na;
    }

    public int getStepCount()
    {
        return s_stepCount;
    }

    public Offset getA()
    {
        return c_cura;
    }

    public Offset getB()
    {
        return c_curb;
    }

    public void ignoraA()
    {
        i_ignorea[n_na] = true;
    }

    public boolean hasNext()
    {
        return !d_done;
    }

    public void step()
    {
        if (d_done) {
            throw new NoSuchElementException();
        }

        // Peek ahead in the A list.
        Offset nexta = null;
        if (n_na < m_maxa) {
            nexta = l_ia.next();
            l_ia.previous();
        }

        final boolean nexta_starts_before_curb_ends = (nexta != null)
                && (nexta.getBeginCharacter() <= c_curb.getEndCharacter());
        final boolean cura_ends_before_or_with_curb = c_cura.getEndCharacter() <= c_curb
                .getEndCharacter();

        if (l_log.isTraceEnabled()) {
            l_log.trace("---");
            l_log.trace("   A                            : " + n_na + "/" + m_maxa + " " + c_cura
                    + " peek: " + nexta);
            l_log.trace("   B                            : " + n_nb + "/" + m_maxb + " " + c_curb);
            l_log.trace("   nexta starts before curb ends: " + nexta_starts_before_curb_ends);
            l_log.trace("   cura ends before or with curb: " + cura_ends_before_or_with_curb);
        }

        // Which one to step up A or B?
        if (nexta_starts_before_curb_ends || cura_ends_before_or_with_curb) {
            // Can A be stepped up any more?
            if (n_na < m_maxa) {
                stepA();
                // if not, try stepping up B
            }
            else if (n_nb < m_maxb) {
                stepB();
                // if both are at the end, bail out
            }
            else {
                d_done = true;
            }
        }
        else {
            // Can B be stepped up any more?
            if (n_nb < m_maxb) {
                stepB();
                // if not, try stepping up A
            }
            else if (n_na < m_maxa) {
                stepA();
                // if both are at the end, bail out
            }
            else {
                d_done = true;
            }
        }

        if (l_log.isTraceEnabled() && d_done) {
            l_log.trace("   -> Both lists at the end.");
        }
    }

    private void stepA()
    {
        s_stepCount++;
        n_na = l_ia.nextIndex();
        c_cura = l_ia.next();

        if (l_log.isTraceEnabled()) {
            l_log.trace("   -> A: " + n_na + "/" + m_maxa + " " + c_cura);
        }
    }

    private void stepBackA()
    {
        n_na = l_ia.previousIndex();
        c_cura = l_ia.previous();

        if (l_log.isTraceEnabled()) {
            l_log.trace("   <- A: " + n_na + "/" + m_maxa + " " + c_cura);
        }
    }

    private void stepB()
    {
        s_stepCount++;
        n_nb = l_ib.nextIndex();
        c_curb = l_ib.next();

        if (l_log.isTraceEnabled()) {
            l_log.trace("   -> B: " + n_nb + "/" + m_maxb + " " + c_curb);
        }

        if (c_curb.getBeginCharacter() < c_cura.getEndCharacter()) {
            // Rewind A to the point where it was when we last stepped
            // up B.
            rewindA();
        }
        else {
            l_last_b_step_na = n_na;
        }
    }

    private void rewindA()
    {
        final String method = "rewindA";
        if (l_log.isTraceEnabled()) {
            l_log.trace("   <- rewinding A");
        }

        // Seek back to the first segment that does not overlap
        // with curb and at most until the last b step we made.
        boolean steppedBack = false;
        while ((n_na > l_last_b_step_na) && (c_cura.getEndCharacter() > c_curb.getBeginCharacter())) {
            stepBackA();
            steppedBack = true;
        }

        // Correct pointer
        if (steppedBack) {
            // Make sure the next peek really peeks ahead.
            n_na = l_ia.nextIndex();
            c_cura = l_ia.next();
        }

        // Skip over the A's we do not want to see again.
        while (i_ignorea[n_na] && (n_na < m_maxa)) {
            stepA();
        }

        // If we skipped some As those we skip will always be skipped, so we
        // can as well update the _last_b_step_na so we don't have to skip them
        // every time.
        l_last_b_step_na = n_na;
    }
}
