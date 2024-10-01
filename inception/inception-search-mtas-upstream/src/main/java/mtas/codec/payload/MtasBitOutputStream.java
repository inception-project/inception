package mtas.codec.payload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * The Class MtasBitOutputStream.
 */
public class MtasBitOutputStream
    extends ByteArrayOutputStream
{

    /** The bit buffer. */
    private int bitBuffer = 0;

    /** The bit count. */
    private int bitCount = 0;

    /**
     * Instantiates a new mtas bit output stream.
     */
    public MtasBitOutputStream()
    {
        // do nothing
    }

    /**
     * Write bit.
     *
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeBit(int value) throws IOException
    {
        writeBit(value, 1);
    }

    /**
     * Write bit.
     *
     * @param value
     *            the value
     * @param number
     *            the number
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeBit(int value, int number) throws IOException
    {
        int localNumber = number;
        while (localNumber > 0) {
            localNumber--;
            bitBuffer |= ((value & 1) << bitCount++);
            if (bitCount == 8) {
                createByte();
            }
        }
    }

    /**
     * Write elias gamma coding integer.
     *
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeEliasGammaCodingInteger(int value) throws IOException
    {
        if (value >= 0) {
            writeEliasGammaCodingPositiveInteger(2 * value + 1);
        }
        else {
            writeEliasGammaCodingPositiveInteger(-2 * value);
        }
    }

    /**
     * Write elias gamma coding non negative integer.
     *
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeEliasGammaCodingNonNegativeInteger(int value) throws IOException
    {
        if (value >= 0) {
            writeEliasGammaCodingPositiveInteger(value + 1);
        }
    }

    /**
     * Write elias gamma coding positive integer.
     *
     * @param value
     *            the value
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void writeEliasGammaCodingPositiveInteger(int value) throws IOException
    {
        if (value > 0) {
            if (value == 1) {
                writeBit(1);
            }
            else {
                writeBit(0);
                writeEliasGammaCodingPositiveInteger(value / 2);
                writeBit(value % 2);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.ByteArrayOutputStream#close()
     */
    @Override
    public void close() throws IOException
    {
        createByte();
        super.close();
    }

    /**
     * Creates the byte.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public void createByte() throws IOException
    {
        if (bitCount > 0) {
            bitCount = 0;
            write(bitBuffer);
            bitBuffer = 0;
        }
    }
}
