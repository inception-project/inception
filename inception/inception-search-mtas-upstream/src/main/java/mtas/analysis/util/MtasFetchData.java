package mtas.analysis.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

import org.apache.lucene.util.ResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class MtasFetchData.
 */
public class MtasFetchData
{

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(MtasFetchData.class);

    /** The reader. */
    Reader reader;

    /** The resource loader. */
    ResourceLoader resourceLoader;

    /**
     * Instantiates a new mtas fetch data.
     *
     * @param input
     *            the input
     * @param resourceLoader
     *            the resource loader
     */
    public MtasFetchData(Reader input, ResourceLoader resourceLoader)
    {
        reader = input;
        this.resourceLoader = resourceLoader;
    }

    /**
     * Gets the string.
     *
     * @return the string
     * @throws MtasParserException
     *             the mtas parser exception
     */
    private String getString() throws MtasParserException
    {
        BufferedReader bufferedReader = new BufferedReader(reader, 2048);
        try {
            char[] arr = new char[8 * 1024];
            StringBuilder buffer = new StringBuilder();
            int numCharsRead;
            while ((numCharsRead = bufferedReader.read(arr, 0, arr.length)) != -1) {
                buffer.append(arr, 0, numCharsRead);
            }
            bufferedReader.close();
            return (buffer.toString());
        }
        catch (IOException e) {
            log.debug("Error", e);
            throw new MtasParserException("couldn't read text");
        }
    }

    /**
     * Gets the url.
     *
     * @param prefix
     *            the prefix
     * @param postfix
     *            the postfix
     * @return the url
     * @throws MtasParserException
     *             the mtas parser exception
     */
    public Reader getUrl(String prefix, String postfix) throws MtasParserException
    {
        String url = getString();
        if ((url != null) && !url.equals("")) {
            if (prefix != null) {
                url = prefix + url;
            }
            if (postfix != null) {
                url = url + postfix;
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                BufferedReader in = null;
                try {
                    URLConnection connection = new URL(url).openConnection();
                    connection.setRequestProperty("Accept-Encoding", "gzip");
                    connection.setReadTimeout(10000);
                    if (connection.getHeaderField("Content-Encoding") != null
                            && connection.getHeaderField("Content-Encoding").equals("gzip")) {
                        in = new BufferedReader(new InputStreamReader(
                                new GZIPInputStream(connection.getInputStream()),
                                StandardCharsets.UTF_8));
                    }
                    else {
                        in = new BufferedReader(new InputStreamReader(connection.getInputStream(),
                                StandardCharsets.UTF_8));
                    }
                    return in;
                }
                catch (IOException ex) {
                    log.debug("Error", ex);
                    throw new MtasParserException("couldn't get " + url);
                }
            }
            else {
                throw new MtasParserException("no valid url: " + url);
            }
        }
        else {
            throw new MtasParserException("no valid url: " + url);
        }
    }

    /**
     * Gets the file.
     *
     * @param prefix
     *            the prefix
     * @param postfix
     *            the postfix
     * @return the file
     * @throws MtasParserException
     *             the mtas parser exception
     */
    public Reader getFile(String prefix, String postfix) throws MtasParserException
    {
        String file = getString();
        if ((file != null) && !file.equals("")) {
            if (prefix != null) {
                file = prefix + file;
            }
            if (postfix != null) {
                file = file + postfix;
            }
            Path path = (new File(file)).toPath();
            // first, try resourceLoader
            if (resourceLoader != null) {
                try {
                    return new InputStreamReader(
                            new GZIPInputStream(resourceLoader.openResource(file)),
                            StandardCharsets.UTF_8);
                }
                catch (IOException e1) {
                    log.debug("Error", e1);
                    try {
                        return new InputStreamReader(resourceLoader.openResource(file),
                                StandardCharsets.UTF_8);
                    }
                    catch (IOException e2) {
                        log.debug("Error", e2);
                        // throw new MtasParserException(e2.getMessage());
                    }
                }
            }
            if (Files.isReadable(path)) {
                try {
                    return new InputStreamReader(new GZIPInputStream(new FileInputStream(file)),
                            StandardCharsets.UTF_8);
                }
                catch (IOException e1) {
                    log.debug("Error", e1);
                    try {
                        String text = new String(Files.readAllBytes(Paths.get(file)),
                                StandardCharsets.UTF_8);
                        return new StringReader(text);
                    }
                    catch (IOException e2) {
                        log.debug("Error", e2);
                        throw new MtasParserException(e2.getMessage());
                    }
                }
            }
            else {
                throw new MtasParserException(
                        "file '" + file + "' does not exists or not readable");
            }
        }
        else {
            throw new MtasParserException("no valid file: " + file);
        }
    }

    /**
     * Gets the default.
     *
     * @return the default
     */
    public Reader getDefault()
    {
        return reader;
    }

}
