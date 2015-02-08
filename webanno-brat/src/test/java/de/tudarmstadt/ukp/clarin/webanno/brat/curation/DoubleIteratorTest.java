/*******************************************************************************
 * Copyright (c) 2004-2009 Richard Eckart de Castilho.
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
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
 * 
 * Contributors:
 *     Richard Eckart de Castilho - initial API and implementation
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.curation;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.junit.Test;

public class DoubleIteratorTest
{
    private static final Log _log = LogFactory.getLog(DoubleIteratorTest.class);

    private final Random _rnd = new Random();

    // @Test
    // public
    // void testStep()
    // {
    // ArrayList<MarkableInterval> a = prepareList(1000);
    // ArrayList<MarkableInterval> b = prepareList(1000);
    //
    // long stime = System.currentTimeMillis();
    // DoubleIterator<MarkableInterval, MarkableInterval> it =
    // new DoubleIterator<MarkableInterval, MarkableInterval>(
    // a, b);
    //
    // int steps = 0;
    // while (it.hasNext()) {
    // it.getA().marked = true;
    // it.getB().marked = true;
    // it.step();
    // steps ++;
    // }
    //
    // for (int i = 0; i < 1000; i++) {
    // assertEquals(true, a.get(i).marked);
    // assertEquals(true, b.get(i).marked);
    // }
    //
    // long time = System.currentTimeMillis() - stime;
    //
    // _log.info("Step 1 - Time: "+time+"ms - Steps: "+steps);
    // }

    @Test
    public void testStep2()
    {
        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(2775, 2820));
        a.add(new ImmutableInterval(2810, 2869));

        b.add(new ImmutableInterval(2351, 2371));
        b.add(new ImmutableInterval(2760, 2839));

        test(a, b, false);
    }

    @Test
    public void testStep3()
    {

        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(8274, 8335));
        a.add(new ImmutableInterval(8326, 8407));

        b.add(new ImmutableInterval(8275, 8329));
        b.add(new ImmutableInterval(8768, 8861));

        test(a, b, false);
    }

    @Test
    public void testStep4()
    {
        // Expected: 1563-1652, 1635, 1635

        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(63, 152));
        a.add(new ImmutableInterval(135, 135));

        b.add(new ImmutableInterval(64, 135));
        b.add(new ImmutableInterval(200, 204));

        test(a, b, false);
    }

    @Test
    public void testStep5()
    {
        // Expected: 80-117, 80-120

        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(80, 117));
        a.add(new ImmutableInterval(80, 120));

        b.add(new ImmutableInterval(45, 80));
        b.add(new ImmutableInterval(62, 97));

        test(a, b, false);
    }

    @Test
    public void testStep6()
    {
        // Expected: 89-90, 89-101

        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(89, 90));
        a.add(new ImmutableInterval(89, 101));

        b.add(new ImmutableInterval(59, 94));
        b.add(new ImmutableInterval(62, 120));

        test(a, b, false);
    }

    @Test
    public void testStep7()
    {
        // Expected: 69-96, 69-104

        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(69, 96));
        a.add(new ImmutableInterval(69, 104));

        b.add(new ImmutableInterval(20, 69));
        b.add(new ImmutableInterval(88, 121));

        test(a, b, false);
    }

    @Test
    public void testStep8()
    {
        // Expected: 63-109, 63,63

        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(63, 63));
        a.add(new ImmutableInterval(63, 109));

        b.add(new ImmutableInterval(62, 63));
        b.add(new ImmutableInterval(65, 111));

        test(a, b, false);
    }

    @Test
    public void testStep9()
    {
        // We needed too many steps here (> 75)

        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(0, 2)); // 0
        a.add(new ImmutableInterval(7, 19)); // 1
        a.add(new ImmutableInterval(13, 16)); // 2
        a.add(new ImmutableInterval(24, 31)); // 3
        a.add(new ImmutableInterval(37, 42)); // 5
        a.add(new ImmutableInterval(39, 87)); // big
        a.add(new ImmutableInterval(46, 66));
        a.add(new ImmutableInterval(57, 78));
        a.add(new ImmutableInterval(60, 61));
        a.add(new ImmutableInterval(83, 90));

        b.add(new ImmutableInterval(15, 34)); // 0
        b.add(new ImmutableInterval(34, 50)); // 4
        b.add(new ImmutableInterval(41, 76));
        b.add(new ImmutableInterval(44, 67));
        b.add(new ImmutableInterval(45, 53));
        b.add(new ImmutableInterval(46, 50));
        b.add(new ImmutableInterval(51, 89));
        b.add(new ImmutableInterval(54, 67));
        b.add(new ImmutableInterval(68, 108));
        b.add(new ImmutableInterval(71, 107));

        test(a, b, false);
    }

    @Test
    public void testStep10()
    {
        // Steps : 138
        // A :[[1-16], [8-52], [13-60], [17-65], [26-61], [31-46], [42-60], [48-92], [80-88],
        // [91-120]]
        // B :[[24-33], [25-71], [27-63], [29-63], [30-64], [34-82], [39-84], [41-87], [58-66],
        // [74-100]]
        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(1, 16));
        a.add(new ImmutableInterval(8, 52));
        a.add(new ImmutableInterval(13, 60));
        a.add(new ImmutableInterval(17, 65));
        a.add(new ImmutableInterval(26, 61));
        a.add(new ImmutableInterval(31, 46));
        a.add(new ImmutableInterval(42, 60));
        a.add(new ImmutableInterval(48, 92));
        a.add(new ImmutableInterval(80, 88));
        a.add(new ImmutableInterval(91, 120));

        b.add(new ImmutableInterval(24, 33));
        b.add(new ImmutableInterval(25, 71));
        b.add(new ImmutableInterval(27, 63));
        b.add(new ImmutableInterval(29, 63));
        b.add(new ImmutableInterval(30, 64));
        b.add(new ImmutableInterval(34, 82));
        b.add(new ImmutableInterval(39, 84));
        b.add(new ImmutableInterval(41, 87));
        b.add(new ImmutableInterval(58, 66));
        b.add(new ImmutableInterval(74, 100));

        test(a, b, true);
    }

    @Test
    public void testStep11()
    {
        // A : [[8-16], [10-50], [22-48], [28-71], [36-39], [40-67], [42-64], [57-95], [84-116],
        // [96-108]]
        // B : [[40-67], [46-46], [51-81], [66-95], [68-73], [68-82], [75-104], [79-107], [86-122],
        // [87-92]]
        final ArrayList<ImmutableInterval> a = new ArrayList<ImmutableInterval>();
        final ArrayList<ImmutableInterval> b = new ArrayList<ImmutableInterval>();

        a.add(new ImmutableInterval(8, 16));
        a.add(new ImmutableInterval(10, 50));
        a.add(new ImmutableInterval(22, 48));
        a.add(new ImmutableInterval(28, 71));
        a.add(new ImmutableInterval(36, 39));
        a.add(new ImmutableInterval(40, 67));
        a.add(new ImmutableInterval(42, 64));
        a.add(new ImmutableInterval(57, 95));
        a.add(new ImmutableInterval(84, 116));
        a.add(new ImmutableInterval(96, 108));

        b.add(new ImmutableInterval(40, 67));
        b.add(new ImmutableInterval(46, 46));
        b.add(new ImmutableInterval(51, 81));
        b.add(new ImmutableInterval(66, 95));
        b.add(new ImmutableInterval(68, 73));
        b.add(new ImmutableInterval(68, 82));
        b.add(new ImmutableInterval(75, 104));
        b.add(new ImmutableInterval(79, 107));
        b.add(new ImmutableInterval(86, 122));
        b.add(new ImmutableInterval(87, 92));

        test(a, b, true);
    }

    // @Test
    // public
    // void testLargeSet()
    // {
    // for (int i = 1; i <= 4; i++) {
    //
    // int size = 1;
    // for (int n = 0; n < i; n++) {
    // size = size * 10;
    // }
    //
    // ArrayList<MarkableInterval> a = prepareList(size);
    // ArrayList<MarkableInterval> b = prepareList(size);
    //
    // long stime = System.currentTimeMillis();
    // overlapping(a, b, false, true);
    // long time = System.currentTimeMillis() - stime;
    //
    // _log.info("Size test for "+size+" done: "+time+"ms");
    // }
    // }

    // @Test
    // public
    // void testStepRandom()
    // {
    // long stime = System.currentTimeMillis();
    //
    // for (int i = 0; i < 1000000; i++) {
    // ArrayList<MarkableInterval> a = prepareList(10);
    // ArrayList<MarkableInterval> b = prepareList(10);
    //
    // test(a, b, false);
    //
    // if (i % 1000 == 0) {
    // _log.info("Running... "+i);
    // }
    // }
    //
    // long time = System.currentTimeMillis() - stime;
    //
    // _log.info("Random test done: "+time+"ms");
    // }

    private <T extends AnnotationFS> void test(final List<T> a, final List<T> b, final boolean debug)
    {
        final List<? extends AnnotationFS> r1 = overlappingRef(a, b);
        final List<? extends AnnotationFS> r2 = overlapping(a, b, debug, false);

        uniq(r1, SEG_START_CMP);
        uniq(r2, SEG_START_CMP);

        if (!r1.equals(r2)) {
            System.out.println("Comparing... Mismatch!");
            System.out.println("A        : " + a);
            System.out.println("B        : " + b);
            System.out.println("Expected : " + r1);
            System.out.println("Actual   : " + r2);

            // Repeat test with debugging turned on.
            overlapping(a, b, true, true);
            assertEquals("Overlapping regions do not match", r1, r2);
        }
    }

    private ArrayList<MarkableInterval> prepareList(final int size)
    {
        final ArrayList<MarkableInterval> a = new ArrayList<MarkableInterval>(size);

        for (int i = 0; i < size; i++) {
            final int start = _rnd.nextInt(100);
            final int end = start + _rnd.nextInt(50);
            a.add(new MarkableInterval(start, end));
        }

        Collections.sort(a, SEG_START_CMP);
        return a;
    }

    private <T extends AnnotationFS> ArrayList<T> overlappingRef(final List<T> a, final List<T> b)
    {
        final ArrayList<T> result = new ArrayList<T>();

        for (final T ia : a) {
            for (final T ib : b) {
                if (overlaps(ia, ib)) {
                    result.add(ia);
                }
            }
        }

        return result;
    }

    private <T extends AnnotationFS> ArrayList<T> overlapping(final List<T> a, final List<T> b,
            final boolean debug, final boolean showSteps)
    {
        final ArrayList<T> result = new ArrayList<T>();

        final DoubleIterator<T, T> it = new DoubleIterator<T, T>(a, b);

        while (it.hasNext()) {
            final boolean overlaps = overlaps(it.getA(), it.getB());

            if (debug) {
                System.out.println("   ->A:" + it.getA() + " B:" + it.getB() + " :: " + overlaps);
            }

            if (overlaps) {
                result.add(it.getA());
                it.ignoraA();
            }
            it.step();
        }

        if (showSteps) {
            System.out.println("- Steps  : " + it.getStepCount());
        }

        return result;
    }

    public boolean overlaps(final AnnotationFS a, final AnnotationFS b)
    {
        // Cases:
        //
        //         start                     end
        //           |                        |
        //  1     #######                     |
        //  2        |                     #######
        //  3   ####################################
        //  4        |        #######         |
        //           |                        |

        return (((b.getBegin() <= a.getBegin()) && (a.getBegin() < b.getEnd())) || // Case 1-3
                ((b.getBegin() < a.getEnd()) && (a.getEnd() <= b.getEnd())) || // Case 1-3
        ((a.getBegin() <= b.getBegin()) && (b.getEnd() <= a.getEnd()))); // Case 4
    }

    class MarkableInterval
        extends ImmutableInterval
    {
        boolean marked;

        MarkableInterval(final int start, final int end)
        {
            super(start, end);
        }
    }

    class ImmutableInterval
        implements AnnotationFS
    {
        private int begin;
        private int end;

        public ImmutableInterval(int aBegin, int aEnd)
        {
            begin = aBegin;
            end = aEnd;
        }

        @Override
        public CAS getView()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Type getType()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeatureValue(Feature aFeat, FeatureStructure aFs)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public FeatureStructure getFeatureValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setStringValue(Feature aFeat, String aS)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getStringValue(Feature aF)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public float getFloatValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFloatValue(Feature aFeat, float aF)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getIntValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setIntValue(Feature aFeat, int aI)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte getByteValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setByteValue(Feature aFeat, byte aI)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getBooleanValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setBooleanValue(Feature aFeat, boolean aI)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public short getShortValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setShortValue(Feature aFeat, short aI)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getLongValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLongValue(Feature aFeat, long aI)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getDoubleValue(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setDoubleValue(Feature aFeat, double aI)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getFeatureValueAsString(Feature aFeat)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setFeatureValueFromString(Feature aFeat, String aS)
            throws CASRuntimeException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CAS getCAS()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getBegin()
        {
            return begin;
        }

        @Override
        public int getEnd()
        {
            return end;
        }

        @Override
        public String getCoveredText()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object clone()
        {
            throw new UnsupportedOperationException();
        }
        
        @Override
        public
        String toString()
        {
            return "["+getBegin()+"-"+getEnd()+"]";
        }
    }
    
    public static <T> void uniq(final List<T> c, final Comparator<? super T> cmp)
    {
        if (c.size() < 2) {
            return;
        }

        Collections.sort(c, cmp);
        final Iterator<T> i = c.iterator();
        Object last = i.next();
        while (i.hasNext()) {
            final Object cur = i.next();
            if (last.equals(cur)) {
                i.remove();
            }
            else {
                last = cur;
            }
        }

    }
    public final static Comparator<AnnotationFS> SEG_START_CMP = new Comparator<AnnotationFS>()
    {
        @Override
        public int compare(final AnnotationFS a0, final AnnotationFS a1)
        {
            final int a0s = a0.getBegin();
            final int a1s = a1.getBegin();

            if (a0s == a1s) {
                return a0.getEnd() - a1.getEnd();
            }
            else {
                return a0s - a1s;
            }
        }
    };
}
