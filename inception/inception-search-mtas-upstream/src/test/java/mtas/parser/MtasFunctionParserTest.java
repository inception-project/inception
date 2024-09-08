package mtas.parser;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Random;

import mtas.parser.function.MtasFunctionParser;
import mtas.parser.function.ParseException;
import mtas.parser.function.util.MtasFunctionParserFunction;
import mtas.parser.function.util.MtasFunctionParserFunctionResponse;
import mtas.parser.function.util.MtasFunctionParserFunctionResponseDouble;
import mtas.parser.function.util.MtasFunctionParserFunctionResponseLong;

/**
 * The Class MtasFunctionParserTest.
 */
public class MtasFunctionParserTest
{

    /** The generator. */
    Random generator = new Random();

    /**
     * Test function.
     *
     * @param pf
     *            the pf
     * @param args
     *            the args
     * @param n
     *            the n
     * @param r
     *            the r
     */
    private void testFunction(MtasFunctionParserFunction pf, long[] argsQ, long[] argsD, int n,
            int d, MtasFunctionParserFunctionResponse r)
    {
        assertEquals(pf + "\tn:" + n + "\td:" + d + "\targsQ:" + Arrays.toString(argsQ) + "\targsD:"
                + Arrays.toString(argsD), pf.getResponse(argsQ, argsD, n, d), r);
    }

    /**
     * Gets the args.
     *
     * @param n
     *            the n
     * @param min
     *            the min
     * @param max
     *            the max
     * @return the args
     */
    private long[] getArgsQ(int n, int min, int max)
    {
        long[] args = new long[n];
        for (int i = 0; i < n; i++) {
            args[i] = min + generator.nextInt((1 + max - min));
        }
        return args;
    }

    private long[] getArgsD(int n, long[] argsQ, int max)
    {
        long[] args = new long[n];
        for (int i = 0; i < n; i++) {
            args[i] = (argsQ[i] > 0) ? generator.nextInt(Math.min(max, (int) argsQ[i] + 1)) : 1;
        }
        return args;
    }

    /**
     * Gets the n.
     *
     * @param min
     *            the min
     * @param max
     *            the max
     * @return the n
     */
    private int getN(int min, int max)
    {
        return min + generator.nextInt((1 + max - min));
    }

    /**
     * Basic test function 1.
     */
    @org.junit.Test
    public void basicTestFunction1()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            for (int i = 0; i < 1000; i++) {
                int n = getN(0, 10000);
                int d = getN(0, 100);
                int k = generator.nextInt(10);
                function = "$q" + k;
                p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
                pf = p.parse();
                int l = 1 + k + generator.nextInt(20);
                argsQ = getArgsQ(l, -1000, 1000);
                argsD = getArgsQ(l, 10, 100);
                testFunction(pf, argsQ, argsD, n, d,
                        new MtasFunctionParserFunctionResponseLong(argsQ[k], true));
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 2.
     */
    @org.junit.Test
    public void basicTestFunction2()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        function = "$n";
        p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
        try {
            pf = p.parse();
            for (int i = 0; i < 1000; i++) {
                int n = getN(0, 10000);
                int d = getN(0, 100);
                int k = generator.nextInt(10);
                int l = 1 + k + generator.nextInt(20);
                argsQ = getArgsQ(l, -1000, 1000);
                argsD = getArgsQ(l, 10, 100);
                testFunction(pf, argsQ, argsD, n, d,
                        new MtasFunctionParserFunctionResponseLong(n, true));
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 3.
     */
    @org.junit.Test
    public void basicTestFunction3()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            for (int i = 0; i < 1000; i++) {
                int n = getN(0, 10);
                int d = getN(0, 3);
                int i1 = generator.nextInt(100) - 50;
                int o0 = generator.nextInt(4);
                int k1 = generator.nextInt(10);
                int o1 = generator.nextInt(4);
                int k2 = generator.nextInt(10);
                int o2 = generator.nextInt(4);
                int k3 = generator.nextInt(10);
                int o3 = generator.nextInt(4);
                int k4 = generator.nextInt(10);
                int o4 = generator.nextInt(4);
                int k5 = generator.nextInt(10);
                int o5 = generator.nextInt(4);
                int k6 = generator.nextInt(3);
                int o6 = generator.nextInt(4);
                function = i1 + " " + getOperator(o0) + " $q" + k1 + " " + getOperator(o1) + " $q"
                        + k2 + " " + getOperator(o2) + " $q" + k3 + " " + getOperator(o3) + " $q"
                        + k4 + " " + getOperator(o4) + " $q" + k5 + " " + getOperator(o5) + " $n "
                        + getOperator(o6) + " $q" + k6;
                p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
                pf = p.parse();
                int l = 10 + generator.nextInt(20);
                argsQ = getArgsQ(l, -10, 10);
                argsD = getArgsQ(l, 2, 8);
                Object answer = null;
                try {
                    answer = compute(o0, i1, argsQ[k1]);
                    answer = answer instanceof Double ? compute(o1, (double) answer, argsQ[k2])
                            : compute(o1, (int) answer, argsQ[k2]);
                    answer = answer instanceof Double ? compute(o2, (double) answer, argsQ[k3])
                            : compute(o2, (int) answer, argsQ[k3]);
                    answer = answer instanceof Double ? compute(o3, (double) answer, argsQ[k4])
                            : compute(o3, (int) answer, argsQ[k4]);
                    answer = answer instanceof Double ? compute(o4, (double) answer, argsQ[k5])
                            : compute(o4, (int) answer, argsQ[k5]);
                    answer = answer instanceof Double ? compute(o5, (double) answer, n)
                            : compute(o5, (int) answer, n);
                    answer = answer instanceof Double ? compute(o6, (double) answer, argsQ[k6])
                            : compute(o6, (int) answer, argsQ[k6]);
                    if (answer instanceof Double) {
                        testFunction(pf, argsQ, argsD, n, d,
                                new MtasFunctionParserFunctionResponseDouble((double) answer,
                                        true));
                    }
                    else {
                        testFunction(pf, argsQ, argsD, n, d,
                                new MtasFunctionParserFunctionResponseLong((int) answer, true));
                    }
                }
                catch (IOException | IllegalArgumentException e) {
                    if (answer != null && answer instanceof Double) {
                        testFunction(pf, argsQ, argsD, n, d,
                                new MtasFunctionParserFunctionResponseDouble((double) answer,
                                        false));
                    }
                    else {
                        testFunction(pf, argsQ, argsD, n, d,
                                new MtasFunctionParserFunctionResponseDouble((int) 0, false));
                    }
                }
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 4.
     */
    @org.junit.Test
    public void basicTestFunction4()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            int n = getN(1, 10000);
            int d = getN(1, 100);
            int k1 = generator.nextInt(10);
            function = "100/$q" + k1;
            p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
            pf = p.parse();
            int l = 10 + generator.nextInt(20);
            argsQ = getArgsQ(l, 100, 1000);
            argsD = getArgsD(l, argsQ, d);
            double answer = 100.0 / argsQ[k1];
            testFunction(pf, argsQ, argsD, n, d,
                    new MtasFunctionParserFunctionResponseDouble(answer, true));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 5.
     */
    @org.junit.Test
    public void basicTestFunction5()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            function = "$n+100/$q0";
            p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
            pf = p.parse();
            argsQ = new long[] { 0 };
            argsD = new long[] { 0 };
            testFunction(pf, argsQ, argsD, 10, 10,
                    new MtasFunctionParserFunctionResponseDouble(0, false));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 6.
     */
    @org.junit.Test
    public void basicTestFunction6()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            for (int i = 0; i < 1000; i++) {
                int n = getN(0, 10000);
                int d = getN(0, 100);
                int k = generator.nextInt(10);
                function = "$n+1.3+2.6/$q" + k;
                p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
                pf = p.parse();
                int l = 10 + generator.nextInt(20);
                argsQ = getArgsQ(l, -1000, 1000);
                argsD = getArgsQ(l, 10, 100);
                double answer = (argsQ[k] != 0) ? (n + 1.3 + 2.6) / argsQ[k] : 0;
                testFunction(pf, argsQ, argsD, n, d,
                        new MtasFunctionParserFunctionResponseDouble(answer, argsQ[k] != 0));
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 7.
     */
    @org.junit.Test
    public void basicTestFunction7()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            for (int i = 0; i < 1000; i++) {
                int n = getN(0, 10000);
                int d = getN(0, 100);
                int k1 = generator.nextInt(10);
                int k2 = generator.nextInt(10);
                int k3 = generator.nextInt(10);
                function = "$n * ($q" + k1 + "+$q" + k2 + ")/$q" + k3;
                p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
                pf = p.parse();
                int l = 10 + generator.nextInt(20);
                argsQ = getArgsQ(l, -1000, 1000);
                argsD = getArgsQ(l, 10, 100);
                double answer = (argsQ[k3] != 0)
                        ? (double) (n * (argsQ[k1] + argsQ[k2])) / argsQ[k3]
                        : 0;
                testFunction(pf, argsQ, argsD, n, d,
                        new MtasFunctionParserFunctionResponseDouble(answer, argsQ[k3] != 0));
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 8.
     */
    @org.junit.Test
    public void basicTestFunction8()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            for (int i = 0; i < 100000; i++) {
                int n = getN(0, 10000);
                int d = getN(0, 100);
                int k1 = generator.nextInt(10);
                int k2 = generator.nextInt(10);
                int k3 = generator.nextInt(10);
                int k4 = generator.nextInt(10);
                function = "1+(($q" + k1 + "+$q" + k2 + ")/($q" + k3 + "+$q" + k4 + "))-$n";
                p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
                pf = p.parse();
                int l = 10 + generator.nextInt(20);
                argsQ = getArgsQ(l, -1000, 1000);
                argsD = getArgsQ(l, 10, 100);
                double answer = (argsQ[k3] + argsQ[k4] != 0)
                        ? ((double) ((argsQ[k1] + argsQ[k2])) / (double) ((argsQ[k3] + argsQ[k4])))
                                + 1 - n
                        : 0;
                testFunction(pf, argsQ, argsD, n, d, new MtasFunctionParserFunctionResponseDouble(
                        answer, (argsQ[k3] + argsQ[k4]) != 0));
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 9.
     */
    @org.junit.Test
    public void basicTestFunction9()
    {
        String function = null;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            int n = getN(0, 100);
            int d = getN(1, 10);
            int k1 = generator.nextInt(10);
            function = "$n^$q" + k1;
            p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
            pf = p.parse();
            int l = 10 + generator.nextInt(20);
            argsQ = getArgsQ(l, 0, 2);
            argsD = getArgsD(l, argsQ, d);
            long answer = n ^ argsQ[k1];
            testFunction(pf, argsQ, argsD, n, d,
                    new MtasFunctionParserFunctionResponseLong(answer, true));
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Basic test function 10.
     */
    @org.junit.Test
    public void basicTestFunction10()
    {
        String function;
        MtasFunctionParser p;
        MtasFunctionParserFunction pf;
        long[] argsQ = null;
        long[] argsD = null;
        try {
            int n = getN(0, 100);
            int d = getN(0, 10);
            int k1 = generator.nextInt(10);
            int k2 = generator.nextInt(10);
            int k3 = generator.nextInt(10);
            function = "($d0 + 1 + $d)*(" + k1 + " + " + k2 + ")/($q0 + 1 + " + k3 + " - 2)";
            p = new MtasFunctionParser(new BufferedReader(new StringReader(function)));
            pf = p.parse();
            int l = 10 + generator.nextInt(20);
            argsQ = getArgsQ(l, 0, 2);
            argsD = getArgsQ(l, 0, 2);
            if ((argsQ[0] + 1 + k3 - 2) != 0) {
                double answer = (double) (argsD[0] + 1 + d) * (k1 + k2) / (argsQ[0] + 1 + k3 - 2);
                testFunction(pf, argsQ, argsD, n, d,
                        new MtasFunctionParserFunctionResponseDouble(answer, true));
            }
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * Compute.
     *
     * @param op
     *            the op
     * @param v1
     *            the v 1
     * @param v2
     *            the v 2
     * @return the object
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private Object compute(int op, long v1, long v2) throws IOException
    {
        if (op == 0) {
            Long s;
            s = (long) (v1 + v2);
            if (s > Integer.MAX_VALUE || s < Integer.MIN_VALUE) {
                throw new IOException("too big");
            }
            else {
                return s.intValue();
            }
        }
        else if (op == 1) {
            Long s;
            s = (long) (v1 - v2);
            if (s > Integer.MAX_VALUE || s < Integer.MIN_VALUE) {
                throw new IOException("too big");
            }
            else {
                return s.intValue();
            }
        }
        else if (op == 2) {
            Long s;
            s = (long) (v1 * v2);
            if (s > Integer.MAX_VALUE || s < Integer.MIN_VALUE) {
                throw new IOException("too big");
            }
            else {
                return s.intValue();
            }
        }
        else if (op == 3) {
            if (v2 == 0) {
                throw new IllegalArgumentException("division by zero");
            }
            else {
                return (double) v1 / v2;
            }
        }
        else if (op == 4) {
            Long s;
            s = (long) (v1 ^ v2);
            if (s > Integer.MAX_VALUE || s < Integer.MIN_VALUE) {
                throw new IOException("too big");
            }
            else {
                return s.intValue();
            }
        }
        else {
            throw new IOException("unknown operator");
        }
    }

    /**
     * Compute.
     *
     * @param op
     *            the op
     * @param v1
     *            the v 1
     * @param v2
     *            the v 2
     * @return the double
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private Double compute(int op, double v1, long v2) throws IOException
    {
        return compute(op, v1, (double) v2);
    }

    /**
     * Compute.
     *
     * @param op
     *            the op
     * @param v1
     *            the v 1
     * @param v2
     *            the v 2
     * @return the double
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private Double compute(int op, double v1, double v2) throws IOException
    {
        if (op == 0) {
            return v1 + v2;
        }
        else if (op == 1) {
            return v1 - v2;
        }
        else if (op == 2) {
            return v1 * v2;
        }
        else if (op == 3) {
            if (v2 == 0) {
                throw new IllegalArgumentException("division by zero");
            }
            else {
                return v1 / v2;
            }
        }
        else if (op == 4) {
            return Math.pow(v1, v2);
        }
        else {
            throw new IOException("unknown operator");
        }
    }

    /**
     * Gets the operator.
     *
     * @param op
     *            the op
     * @return the operator
     */
    private String getOperator(int op)
    {
        if (op == 0) {
            return "+";
        }
        else if (op == 1) {
            return "-";
        }
        else if (op == 2) {
            return "*";
        }
        else if (op == 3) {
            return "/";
        }
        else if (op == 4) {
            return "^";
        }
        else {
            return "?";
        }
    }

}
