/*
 * Copyright (c) 2004-2009 Richard Eckart de Castilho.
 * 
 * This file was originally part of AnnoLab by the name DoubleIteratorTest.
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
package de.tudarmstadt.ukp.inception.recommendation.api.util;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import de.tudarmstadt.ukp.inception.recommendation.api.model.Offset;

public class OverlapIteratorTest
{
    @Test
    public void testStep2()
    {
        List<Offset> a = new ArrayList<>();
        List<Offset> b = new ArrayList<>();

        a.add(new Offset(2775, 2820));
        a.add(new Offset(2810, 2869));

        b.add(new Offset(2351, 2371));
        b.add(new Offset(2760, 2839));

        test(a, b, false);
    }

    @Test
    public void testStep3()
    {
        List<Offset> a = new ArrayList<>();
        List<Offset> b = new ArrayList<>();

        a.add(new Offset(8274, 8335));
        a.add(new Offset(8326, 8407));

        b.add(new Offset(8275, 8329));
        b.add(new Offset(8768, 8861));

        test(a, b, false);
    }

    @Test
    public void testStep4()
    {
        // Expected: 1563-1652, 1635, 1635

        List<Offset> a = new ArrayList<>();
        List<Offset> b = new ArrayList<>();

        a.add(new Offset(63, 152));
        a.add(new Offset(135, 135));

        b.add(new Offset(64, 135));
        b.add(new Offset(200, 204));

        test(a, b, false);
    }

    @Test
    public void testStep5()
    {
        // Expected: 80-117, 80-120

        final ArrayList<Offset> a = new ArrayList<Offset>();
        final ArrayList<Offset> b = new ArrayList<Offset>();

        a.add(new Offset(80, 117));
        a.add(new Offset(80, 120));

        b.add(new Offset(45, 80));
        b.add(new Offset(62, 97));

        test(a, b, false);
    }

    @Test
    public void testStep6()
    {
        // Expected: 89-90, 89-101

        final ArrayList<Offset> a = new ArrayList<Offset>();
        final ArrayList<Offset> b = new ArrayList<Offset>();

        a.add(new Offset(89, 90));
        a.add(new Offset(89, 101));

        b.add(new Offset(59, 94));
        b.add(new Offset(62, 120));

        test(a, b, false);
    }

    @Test
    public void testStep7()
    {
        // Expected: 69-96, 69-104

        final ArrayList<Offset> a = new ArrayList<Offset>();
        final ArrayList<Offset> b = new ArrayList<Offset>();

        a.add(new Offset(69, 96));
        a.add(new Offset(69, 104));

        b.add(new Offset(20, 69));
        b.add(new Offset(88, 121));

        test(a, b, false);
    }

    @Test
    public void testStep8()
    {
        final ArrayList<Offset> a = new ArrayList<Offset>();
        final ArrayList<Offset> b = new ArrayList<Offset>();

        a.add(new Offset(63, 63));
        a.add(new Offset(63, 109));

        b.add(new Offset(62, 63));
        b.add(new Offset(65, 111));

        test(a, b, false);
    }

    @Test
    public void testStep9()
    {
        // We needed too many steps here (> 75)

        final ArrayList<Offset> a = new ArrayList<Offset>();
        final ArrayList<Offset> b = new ArrayList<Offset>();

        a.add(new Offset(0, 2)); // 0
        a.add(new Offset(7, 19)); // 1
        a.add(new Offset(13, 16)); // 2
        a.add(new Offset(24, 31)); // 3
        a.add(new Offset(37, 42)); // 5
        a.add(new Offset(39, 87)); // big
        a.add(new Offset(46, 66));
        a.add(new Offset(57, 78));
        a.add(new Offset(60, 61));
        a.add(new Offset(83, 90));

        b.add(new Offset(15, 34)); // 0
        b.add(new Offset(34, 50)); // 4
        b.add(new Offset(41, 76));
        b.add(new Offset(44, 67));
        b.add(new Offset(45, 53));
        b.add(new Offset(46, 50));
        b.add(new Offset(51, 89));
        b.add(new Offset(54, 67));
        b.add(new Offset(68, 108));
        b.add(new Offset(71, 107));

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
        final ArrayList<Offset> a = new ArrayList<Offset>();
        final ArrayList<Offset> b = new ArrayList<Offset>();

        a.add(new Offset(1, 16));
        a.add(new Offset(8, 52));
        a.add(new Offset(13, 60));
        a.add(new Offset(17, 65));
        a.add(new Offset(26, 61));
        a.add(new Offset(31, 46));
        a.add(new Offset(42, 60));
        a.add(new Offset(48, 92));
        a.add(new Offset(80, 88));
        a.add(new Offset(91, 120));

        b.add(new Offset(24, 33));
        b.add(new Offset(25, 71));
        b.add(new Offset(27, 63));
        b.add(new Offset(29, 63));
        b.add(new Offset(30, 64));
        b.add(new Offset(34, 82));
        b.add(new Offset(39, 84));
        b.add(new Offset(41, 87));
        b.add(new Offset(58, 66));
        b.add(new Offset(74, 100));

        test(a, b, true);
    }

    @Test
    public void testStep11()
    {
        // A : [[8-16], [10-50], [22-48], [28-71], [36-39], [40-67], [42-64], [57-95], [84-116],
        // [96-108]]
        // B : [[40-67], [46-46], [51-81], [66-95], [68-73], [68-82], [75-104], [79-107], [86-122],
        // [87-92]]
        final ArrayList<Offset> a = new ArrayList<Offset>();
        final ArrayList<Offset> b = new ArrayList<Offset>();

        a.add(new Offset(8, 16));
        a.add(new Offset(10, 50));
        a.add(new Offset(22, 48));
        a.add(new Offset(28, 71));
        a.add(new Offset(36, 39));
        a.add(new Offset(40, 67));
        a.add(new Offset(42, 64));
        a.add(new Offset(57, 95));
        a.add(new Offset(84, 116));
        a.add(new Offset(96, 108));

        b.add(new Offset(40, 67));
        b.add(new Offset(46, 46));
        b.add(new Offset(51, 81));
        b.add(new Offset(66, 95));
        b.add(new Offset(68, 73));
        b.add(new Offset(68, 82));
        b.add(new Offset(75, 104));
        b.add(new Offset(79, 107));
        b.add(new Offset(86, 122));
        b.add(new Offset(87, 92));

        test(a, b, true);
    }

    @Test
    public void testStep11a()
    {
        var a = asList(new Offset(28, 71), new Offset(36, 39));
        var b = asList(new Offset(66, 95), new Offset(68, 73));

        test(a, b, true);
    }

    @Test
    public void testStep12()
    {
        var b = asList(new Offset(0, 14), new Offset(7, 14), new Offset(114, 116),
                new Offset(114, 124), new Offset(135, 154), new Offset(160, 167));
        var a = asList(new Offset(0, 14), new Offset(114, 124), new Offset(135, 154),
                new Offset(160, 167));

        test(a, b, true);
    }

    private void test(final List<Offset> a, final List<Offset> b, final boolean debug)
    {
        var r1 = overlappingRef(a, b);
        var r2 = overlapping(a, b, debug, false);

        r1 = r1.stream().sorted().distinct().toList();
        r2 = r2.stream().sorted().distinct().toList();

        if (!r1.equals(r2)) {
            System.out.println("Expected overlapping pairs: " + r1);
            System.out.println("Actual overlapping pairs  : " + r2);
            System.out.println("Comparing... Mismatch!");
            System.out.println("A        : " + a);
            System.out.println("B        : " + b);
            System.out.println("Expected : " + r1);
            System.out.println("Actual   : " + r2);

            // Repeat test with debugging turned on.
            overlapping(a, b, true, true);
            assertEquals(r1, r2, "Overlapping regions do not match");
        }
    }

    private List<Pair<Offset, Offset>> overlappingRef(final List<Offset> a, final List<Offset> b)
    {
        var result = new ArrayList<Pair<Offset, Offset>>();

        for (var ia : a) {
            for (var ib : b) {
                if (ia.overlaps(ib)) {
                    result.add(Pair.of(ia, ib));
                }
            }
        }

        return result;
    }

    private List<Pair<Offset, Offset>> overlapping(final List<Offset> a, final List<Offset> b,
            final boolean debug, final boolean showSteps)
    {
        var result = new ArrayList<Pair<Offset, Offset>>();

        var it = new OverlapIterator(a, b);

        while (it.hasNext()) {
            result.add(it.next());
        }

        return result;
    }
}
