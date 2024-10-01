package mtas.codec.util.distance;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.util.BytesRef;

import mtas.analysis.token.MtasToken;

/**
 * The Class MorseDistance.
 */
public class MorseDistance
    extends Distance
{

    /** The initial state. */
    protected final double[] initialState;

    /** The Constant defaultDeletionDistance. */
    protected final static double defaultDeletionDistance = 1.0;

    /** The Constant defaultInsertionDistance. */
    protected final static double defaultInsertionDistance = 10.0;

    /** The Constant defaultReplaceDistance. */
    protected final static double defaultReplaceDistance = 10.0;

    /** The Constant defaultTranspositionDistance. */
    protected final static double defaultTranspositionDistance = 10.0;

    /** The deletion distance. */
    protected double deletionDistance;

    /** The insertion distance. */
    protected double insertionDistance;

    /** The replace distance. */
    protected double replaceDistance;

    /** The transposition distance. */
    protected double transpositionDistance;

    /** The Constant PARAMETER_DELETIONDISTANCE. */
    protected final static String PARAMETER_DELETIONDISTANCE = "deletionDistance";

    /** The Constant PARAMETER_INSERTIONDISTANCE. */
    protected final static String PARAMETER_INSERTIONDISTANCE = "insertionDistance";

    /** The Constant PARAMETER_REPLACEDISTANCE. */
    protected final static String PARAMETER_REPLACEDISTANCE = "replaceDistance";

    /** The Constant PARAMETER_TRANSPOSITIONDISTANCE. */
    protected final static String PARAMETER_TRANSPOSITIONDISTANCE = "transpositionDistance";

    /** The morse base. */
    protected final String morseBase;

    /** The Constant ALPHABET_MORSE. */
    private static final Map<Byte, String> ALPHABET_MORSE;
    static {
        Map<Byte, String> m = new HashMap<>();
        m.put((byte) 'a', ".-");
        m.put((byte) 'b', "-...");
        m.put((byte) 'c', "-.-.");
        m.put((byte) 'd', "-..");
        m.put((byte) 'e', ".");
        m.put((byte) 'f', "..-.");
        m.put((byte) 'g', "--.");
        m.put((byte) 'h', "....");
        m.put((byte) 'i', "..");
        m.put((byte) 'j', ".---");
        m.put((byte) 'k', "-.-");
        m.put((byte) 'l', ".-..");
        m.put((byte) 'm', "--");
        m.put((byte) 'n', "-.");
        m.put((byte) 'o', "---");
        m.put((byte) 'p', ".--.");
        m.put((byte) 'q', "--.-");
        m.put((byte) 'r', ".-.");
        m.put((byte) 's', "...");
        m.put((byte) 't', "-");
        m.put((byte) 'u', "..-");
        m.put((byte) 'v', "...-");
        m.put((byte) 'w', ".--");
        m.put((byte) 'x', "-..-");
        m.put((byte) 'y', "-.--");
        m.put((byte) 'z', "--..");
        m.put((byte) '1', ".----");
        m.put((byte) '2', "..---");
        m.put((byte) '3', "...--");
        m.put((byte) '4', "....-");
        m.put((byte) '5', ".....");
        m.put((byte) '6', "-....");
        m.put((byte) '7', "--...");
        m.put((byte) '8', "---..");
        m.put((byte) '9', "----.");
        m.put((byte) '0', "-----");
        ALPHABET_MORSE = Collections.unmodifiableMap(m);
    }

    /**
     * Instantiates a new morse distance.
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
    public MorseDistance(String prefix, String base, Double minimum, Double maximum,
            Map<String, String> parameters)
        throws IOException
    {
        super(prefix, base, minimum, maximum, parameters);
        deletionDistance = defaultDeletionDistance;
        insertionDistance = defaultInsertionDistance;
        replaceDistance = defaultReplaceDistance;
        transpositionDistance = defaultTranspositionDistance;
        if (parameters != null) {
            for (Entry<String, String> entry : parameters.entrySet()) {
                if (entry.getKey().equals(PARAMETER_DELETIONDISTANCE)) {
                    deletionDistance = Double.parseDouble(entry.getValue());
                }
                else if (entry.getKey().equals(PARAMETER_INSERTIONDISTANCE)) {
                    insertionDistance = Double.parseDouble(entry.getValue());
                }
                else if (entry.getKey().equals(PARAMETER_REPLACEDISTANCE)) {
                    replaceDistance = Double.parseDouble(entry.getValue());
                }
                else if (entry.getKey().equals(PARAMETER_TRANSPOSITIONDISTANCE)) {
                    transpositionDistance = Double.parseDouble(entry.getValue());
                }
            }
        }
        if (deletionDistance < 0 || insertionDistance < 0 || replaceDistance < 0
                || transpositionDistance < 0) {
            throw new IOException("distances should be zero or positive");
        }
        morseBase = computeMorse(new BytesRef(prefix + MtasToken.DELIMITER + base));
        initialState = new double[morseBase.length() + 1];
        for (int i = 0; i <= morseBase.length(); i++) {
            initialState[i] = i * insertionDistance;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see mtas.codec.util.distance.Distance#validate(org.apache.lucene.util.BytesRef)
     */
    @Override
    public boolean validateMaximum(BytesRef term)
    {
        if (morseBase == null) {
            return false;
        }
        else if (maximum == null) {
            return true;
        }
        else {
            double[][] state = _start();
            byte b0;
            String morse;
            char ch2 = 0x00;
            int i = term.offset + prefixOffset;
            for (; i < term.length; i++) {
                b0 = term.bytes[i];
                if (b0 == 0x00) {
                    break;
                }
                if (ALPHABET_MORSE.containsKey(b0)) {
                    morse = ALPHABET_MORSE.get(b0) + " ";
                    for (char ch1 : morse.toCharArray()) {
                        state = _step(state, ch1, ch2);
                        if (!_can_match(state)) {
                            return false;
                        }
                        ch2 = ch1;
                    }
                }
                else {
                    return false;
                }
            }
            return _is_match(state);
        }
    }

    @Override
    public boolean validateMinimum(BytesRef term)
    {
        if (minimum == null) {
            return true;
        }
        else {
            return compute(term) > minimum;
        }
    }

    @Override
    public double compute(BytesRef term)
    {
        String morseKey = computeMorse(term);
        if (morseKey == null) {
            return Double.MAX_VALUE;
        }
        else {
            double[][] state = _start();
            char ch2 = 0x00;
            for (char ch1 : morseKey.toCharArray()) {
                if (ch1 == 0x00) {
                    break;
                }
                state = _step(state, ch1, ch2);
                ch2 = ch1;
            }
            return _distance(state);
        }
    }

    @Override
    public double compute(String key)
    {
        String morseKey = computeMorse(key);
        if (morseKey == null) {
            return Double.MAX_VALUE;
        }
        else {
            double[][] state = _start();
            char ch2 = 0x00;
            for (char ch1 : morseKey.toCharArray()) {
                if (ch1 == 0x00) {
                    break;
                }
                state = _step(state, ch1, ch2);
                ch2 = ch1;
            }
            return _distance(state);
        }
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
        for (int i = 0; i < morseBase.length(); i++) {
            cost = (morseBase.charAt(i) == ch1) ? 0 : replaceDistance;
            state[2][i + 1] = Math.min(state[2][i] + insertionDistance, state[1][i] + cost);
            state[2][i + 1] = Math.min(state[2][i + 1], state[1][i + 1] + deletionDistance);
            if (i > 0 && ch2 != 0x00 && (morseBase.charAt(i - 1) == ch1)
                    && (morseBase.charAt(i) == ch2)) {
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

    /**
     * Compute morse.
     *
     * @param term
     *            the term
     * @return the string
     */
    private String computeMorse(BytesRef term)
    {
        StringBuilder stringBuilder = new StringBuilder();
        int i = term.offset + prefixOffset;
        for (; i < term.length; i++) {
            if (ALPHABET_MORSE.containsKey(term.bytes[i])) {
                stringBuilder.append(ALPHABET_MORSE.get(term.bytes[i]) + " ");
            }
            else if (term.bytes[i] != 0x00) {
                return null;
            }
            else {
                break;
            }
        }
        return stringBuilder.toString();
    }

    private String computeMorse(String key)
    {
        StringBuilder stringBuilder = new StringBuilder();
        Byte b;
        for (char ch : key.toCharArray()) {
            b = (byte) ch;
            if (ALPHABET_MORSE.containsKey(b)) {
                stringBuilder.append(ALPHABET_MORSE.get(b) + " ");
            }
            else {
                return null;
            }
        }
        return stringBuilder.toString();
    }

}
