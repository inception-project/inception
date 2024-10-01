package mtas.analysis.util;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * The Class MtasBufferedReader.
 */
public class MtasBufferedReader
    extends Reader
{

    /** The in. */
    private Reader in;

    /** The cb. */
    private char cb[];

    /** The n chars. */
    private int nChars;

    /** The next char. */
    private int nextChar;

    /** The previous buffer size. */
    private int previousBufferSize;

    /** The skip LF. */
    private boolean skipLF = false;

    /** The default char buffer size. */
    private static int defaultCharBufferSize = 8192;

    /** The default expected line length. */
    private static int defaultExpectedLineLength = 80;

    /**
     * Instantiates a new mtas buffered reader.
     *
     * @param in
     *            the in
     * @param sz
     *            the sz
     */
    public MtasBufferedReader(Reader in, int sz)
    {
        super(in);
        if (sz <= 0)
            throw new IllegalArgumentException("Buffer size <= 0");
        this.in = in;
        cb = new char[sz];
        nextChar = nChars = 0;
    }

    /**
     * Instantiates a new mtas buffered reader.
     *
     * @param in
     *            the in
     */
    public MtasBufferedReader(Reader in)
    {
        this(in, defaultCharBufferSize);
    }

    /**
     * Ensure open.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void ensureOpen() throws IOException
    {
        if (in == null)
            throw new IOException("Stream closed");
    }

    /**
     * Fill.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void fill() throws IOException
    {
        int n;
        previousBufferSize += nChars;
        do {
            n = in.read(cb, 0, cb.length);
        }
        while (n == 0);
        if (n > 0) {
            nChars = n;
            nextChar = 0;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Reader#read()
     */
    @Override
    public int read() throws IOException
    {
        synchronized (lock) {
            ensureOpen();
            for (;;) {
                if (nextChar >= nChars) {
                    fill();
                    if (nextChar >= nChars)
                        return -1;
                }
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                        continue;
                    }
                }
                return cb[nextChar++];
            }
        }
    }

    /**
     * Read 1.
     *
     * @param cbuf
     *            the cbuf
     * @param off
     *            the off
     * @param len
     *            the len
     * @return the int
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private int read1(char[] cbuf, int off, int len) throws IOException
    {
        if (nextChar >= nChars) {
            /*
             * If the requested length is at least as large as the buffer, and if there is no
             * mark/reset activity, and if line feeds are not being skipped, do not bother to copy
             * the characters into the local buffer. In this way buffered streams will cascade
             * harmlessly.
             */
            if (len >= cb.length && !skipLF) {
                return in.read(cbuf, off, len);
            }
            fill();
        }
        if (nextChar >= nChars)
            return -1;
        if (skipLF) {
            skipLF = false;
            if (cb[nextChar] == '\n') {
                nextChar++;
                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars)
                    return -1;
            }
        }
        int n = Math.min(len, nChars - nextChar);
        System.arraycopy(cb, nextChar, cbuf, off, n);
        nextChar += n;
        return n;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Reader#read(char[], int, int)
     */
    @Override
    public int read(char cbuf[], int off, int len) throws IOException
    {
        synchronized (lock) {
            ensureOpen();
            if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length)
                    || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            }
            else if (len == 0) {
                return 0;
            }

            int n = read1(cbuf, off, len);
            if (n <= 0)
                return n;
            while ((n < len) && in.ready()) {
                int n1 = read1(cbuf, off + n, len - n);
                if (n1 <= 0)
                    break;
                n += n1;
            }
            return n;
        }
    }

    /**
     * Read line.
     *
     * @param ignoreLF
     *            the ignore LF
     * @return the string
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    String readLine(boolean ignoreLF) throws IOException
    {
        StringBuffer s = null;
        int startChar;

        synchronized (lock) {
            ensureOpen();
            boolean omitLF = ignoreLF || skipLF;

            for (;;) {

                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars) { /* EOF */
                    if (s != null && s.length() > 0)
                        return s.toString();
                    else
                        return null;
                }
                boolean eol = false;
                char c = 0;
                int i;

                /* Skip a leftover '\n', if necessary */
                if (omitLF && (cb[nextChar] == '\n'))
                    nextChar++;
                skipLF = false;
                omitLF = false;

                charLoop: for (i = nextChar; i < nChars; i++) {
                    c = cb[i];
                    if ((c == '\n') || (c == '\r')) {
                        eol = true;
                        break charLoop;
                    }
                }

                startChar = nextChar;
                nextChar = i;

                if (eol) {
                    String str;
                    if (s == null) {
                        str = new String(cb, startChar, i - startChar);
                    }
                    else {
                        s.append(cb, startChar, i - startChar);
                        str = s.toString();
                    }
                    nextChar++;
                    if (c == '\r') {
                        skipLF = true;
                    }
                    return str;
                }

                if (s == null)
                    s = new StringBuffer(defaultExpectedLineLength);
                s.append(cb, startChar, i - startChar);
            }
        }
    }

    /**
     * Read line.
     *
     * @return the string
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public String readLine() throws IOException
    {
        return readLine(false);
    }

    /**
     * Lines.
     *
     * @return the stream
     */
    public Stream<String> lines()
    {
        Iterator<String> iter = new Iterator<String>()
        {
            String nextLine = null;

            @Override
            public boolean hasNext()
            {
                if (nextLine != null) {
                    return true;
                }
                else {
                    try {
                        nextLine = readLine();
                        return (nextLine != null);
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }

            @Override
            public String next()
            {
                if (nextLine != null || hasNext()) {
                    String line = nextLine;
                    nextLine = null;
                    return line;
                }
                else {
                    throw new NoSuchElementException();
                }
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter,
                Spliterator.ORDERED | Spliterator.NONNULL), false);
    }

    /**
     * Gets the position.
     *
     * @return the position
     */
    public int getPosition()
    {
        return previousBufferSize + nextChar;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Reader#skip(long)
     */
    @Override
    public long skip(long n) throws IOException
    {
        if (n < 0L) {
            throw new IllegalArgumentException("skip value is negative");
        }
        synchronized (lock) {
            ensureOpen();
            long r = n;
            while (r > 0) {
                if (nextChar >= nChars)
                    fill();
                if (nextChar >= nChars) /* EOF */
                    break;
                if (skipLF) {
                    skipLF = false;
                    if (cb[nextChar] == '\n') {
                        nextChar++;
                    }
                }
                long d = (long) nChars - nextChar;
                if (r <= d) {
                    nextChar += r;
                    r = 0;
                    break;
                }
                else {
                    r -= d;
                    nextChar = nChars;
                }
            }
            return n - r;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Reader#ready()
     */
    @Override
    public boolean ready() throws IOException
    {
        synchronized (lock) {
            ensureOpen();

            /*
             * If newline needs to be skipped and the next char to be read is a newline character,
             * then just skip it right away.
             */
            if (skipLF) {
                /*
                 * Note that in.ready() will return true if and only if the next read on the stream
                 * will not block.
                 */
                if (nextChar >= nChars && in.ready()) {
                    fill();
                }
                if (nextChar < nChars) {
                    if (cb[nextChar] == '\n')
                        nextChar++;
                    skipLF = false;
                }
            }
            return (nextChar < nChars) || in.ready();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Reader#reset()
     */
    @Override
    public void reset() throws IOException
    {
        synchronized (lock) {
            ensureOpen();
            nextChar = -1;
            previousBufferSize = 0;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.Reader#close()
     */
    @Override
    public void close() throws IOException
    {
        synchronized (lock) {
            if (in == null)
                return;
            try {
                in.close();
            }
            finally {
                in = null;
                cb = null;
            }
        }
    }
}
