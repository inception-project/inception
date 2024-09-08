package mtas.codec.util.distance;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.util.BytesRef;

/**
 * The Class DamerauLevenshteinDistance.
 */
public class DamerauLevenshteinDistance
    extends LevenshteinDistance
{

    /** The Constant defaultTranspositionDistance. */
    protected final static double defaultTranspositionDistance = 1.0;

    /** The transposition distance. */
    protected double transpositionDistance;

    /** The Constant PARAMETER_TRANSPOSITIONDISTANCE. */
    protected final static String PARAMETER_TRANSPOSITIONDISTANCE = "transpositionDistance";

    /**
     * Instantiates a new damerau levenshtein distance.
     *
     * @param prefix
     *            the prefix
     * @param base
     *            the base
     * @param maximum
     *            the maximum
     * @param parameters
     *            the parameters
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public DamerauLevenshteinDistance(String prefix, String base, Double minimum, Double maximum,
            Map<String, String> parameters)
        throws IOException
    {
        super(prefix, base, minimum, maximum, parameters);
        transpositionDistance = defaultTranspositionDistance;
        if (parameters != null) {
            for (Entry<String, String> entry : parameters.entrySet()) {
                if (entry.getKey().equals(PARAMETER_TRANSPOSITIONDISTANCE)) {
                    transpositionDistance = Double.parseDouble(entry.getValue());
                }
            }
        }
        if (transpositionDistance < 0) {
            throw new IOException("distances should be zero or positive");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.distance.LevenshteinDistance#validate(org.apache.lucene. util.BytesRef)
     */
    @Override
    public boolean validateMaximum(BytesRef term)
    {
        if (maximum == null) {
            return true;
        }
        else {
            double[][] state = _start();
            char ch1;
            char ch2 = 0x00;
            int i = term.offset + prefixOffset;
            for (; i < term.length; i++) {
                ch1 = (char) term.bytes[i];
                if (ch1 == 0x00) {
                    break;
                }
                state = _step(state, ch1, ch2);
                if (!_can_match(state)) {
                    return false;
                }
                ch2 = ch1;
            }
            return _is_match(state);
        }
    }

    @Override
    public double compute(BytesRef term)
    {
        double[][] state = _start();
        char ch1;
        char ch2 = 0x00;
        int i = term.offset + prefixOffset;
        for (; i < term.length; i++) {
            ch1 = (char) term.bytes[i];
            if (ch1 == 0x00) {
                break;
            }
            state = _step(state, ch1, ch2);
            ch2 = ch1;
        }
        return _distance(state);
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.distance.LevenshteinDistance#compute(java.lang.String)
     */
    @Override
    public double compute(String key)
    {
        double[][] state = _start();
        char ch2 = 0x00;
        for (char ch1 : key.toCharArray()) {
            if (ch1 == 0x00) {
                break;
            }
            state = _step(state, ch1, ch2);
            ch2 = ch1;
        }
        return _distance(state);
    }

    /**
     * Start.
     *
     * @return the double[][]
     */
    private double[][] _start()
    {
        double[][] startState = new double[3][];
        startState[0] = new double[initialState.length];
        startState[1] = new double[initialState.length];
        startState[2] = Arrays.copyOf(initialState, initialState.length);
        return startState;
    }

    /**
     * Step.
     *
     * @param state
     *            the state
     * @param ch1
     *            the ch 1
     * @param ch2
     *            the ch 2
     * @return the double[][]
     */
    private double[][] _step(double[][] state, char ch1, char ch2)
    {
        double cost;
        _shift(state);
        state[2][0] = state[1][0] + deletionDistance;
        for (int i = 0; i < base.length(); i++) {
            cost = (base.charAt(i) == ch1) ? 0 : replaceDistance;
            state[2][i + 1] = Math.min(state[2][i] + insertionDistance, state[1][i] + cost);
            state[2][i + 1] = Math.min(state[2][i + 1], state[1][i + 1] + deletionDistance);
            if (i > 0 && ch2 != 0x00 && (base.charAt(i - 1) == ch1) && (base.charAt(i) == ch2)) {
                state[2][i + 1] = Math.min(state[2][i + 1],
                        state[0][i - 1] + transpositionDistance);
            }
        }
        return state;
    }

    /**
     * Shift.
     *
     * @param state
     *            the state
     */
    private void _shift(double[][] state)
    {
        double[] tmpState = state[0];
        state[0] = state[1];
        state[1] = state[2];
        state[2] = tmpState;
    }

    /**
     * Checks if is match.
     *
     * @param state
     *            the state
     * @return true, if successful
     */
    private boolean _is_match(double[][] state)
    {
        return state[2][state[2].length - 1] < maximum;
    }

    /**
     * Can match.
     *
     * @param state
     *            the state
     * @return true, if successful
     */
    private boolean _can_match(double[][] state)
    {
        for (double d : state[2]) {
            if (d < maximum) {
                return true;
            }
        }
        return false;
    }

    /**
     * Distance.
     *
     * @param state
     *            the state
     * @return the double
     */
    private double _distance(double[][] state)
    {
        return state[2][state[2].length - 1];
    }

}
