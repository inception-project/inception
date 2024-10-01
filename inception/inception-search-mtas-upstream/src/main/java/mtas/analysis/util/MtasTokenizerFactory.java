package mtas.analysis.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import org.apache.lucene.util.ResourceLoader;
import org.apache.lucene.util.ResourceLoaderAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mtas.analysis.MtasTokenizer;

/**
 * A factory for creating MtasTokenizer objects.
 */
public class MtasTokenizerFactory
    extends TokenizerFactory
    implements ResourceLoaderAware
{

    /** SPI name */
    public static final String NAME = "mtas";

    /** The Constant log. */
    private static final Logger log = LoggerFactory.getLogger(MtasTokenizerFactory.class);

    /** The Constant ARGUMENT_CONFIGFILE. */
    public static final String ARGUMENT_CONFIGFILE = "configFile";

    /** The Constant ARGUMENT_CONFIG. */
    public static final String ARGUMENT_CONFIG = "config";

    /** The Constant ARGUMENT_ANALYZER. */
    public static final String ARGUMENT_PARSER = "parser";

    /** The Constant ARGUMENT_PARSER_ARGS. */
    public static final String ARGUMENT_PARSER_ARGS = "parserArgs";

    /** The Constant ARGUMENT_DEFAULT. */
    public static final String ARGUMENT_DEFAULT = "default";

    /** The config argument. */
    private String configArgument;

    /** The default argument. */
    private String defaultArgument;

    /** The config file argument. */
    private String configFileArgument;

    /** The analyzer argument. */
    private String analyzerArgument;

    /** The parser arguments. */
    private String analyzerArgumentParserArgs;

    /** The configs. */
    private HashMap<String, MtasConfiguration> configs = null;

    /** The config. */
    private MtasConfiguration config = null;

    /**
     * Instantiates a new mtas tokenizer factory.
     *
     * @param args
     *            the args
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasTokenizerFactory(Map<String, String> args) throws IOException
    {
        this(args, null);
    }

    /** Default ctor for compatibility with SPI */
    public MtasTokenizerFactory()
    {
        throw defaultCtorException();
    }

    /**
     * Instantiates a new mtas tokenizer factory.
     *
     * @param args
     *            the args
     * @param resourceLoader
     *            the resource loader
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasTokenizerFactory(Map<String, String> args, ResourceLoader resourceLoader)
        throws IOException
    {
        super(args);
        configFileArgument = get(args, ARGUMENT_CONFIGFILE);
        configArgument = get(args, ARGUMENT_CONFIG);
        analyzerArgument = get(args, ARGUMENT_PARSER);
        analyzerArgumentParserArgs = get(args, ARGUMENT_PARSER_ARGS);
        defaultArgument = get(args, ARGUMENT_DEFAULT);
        int numberOfArgs = 0;
        numberOfArgs = (configFileArgument == null) ? numberOfArgs : numberOfArgs + 1;
        numberOfArgs = (configArgument == null) ? numberOfArgs : numberOfArgs + 1;
        numberOfArgs = (analyzerArgument == null) ? numberOfArgs : numberOfArgs + 1;

        if (numberOfArgs > 1) {
            throw new IOException(this.getClass().getName() + " can't have multiple of "
                    + ARGUMENT_CONFIGFILE + ", " + ARGUMENT_CONFIG + " AND " + ARGUMENT_PARSER);
        }
        else if (configArgument == null && defaultArgument != null) {
            throw new IOException(this.getClass().getName() + " can't have " + ARGUMENT_DEFAULT
                    + " without " + ARGUMENT_CONFIG);
        }
        else if (numberOfArgs == 0) {
            throw new IOException(this.getClass().getName() + " should have " + ARGUMENT_CONFIGFILE
                    + " or " + ARGUMENT_CONFIG + " or " + ARGUMENT_PARSER);
        }
        init(resourceLoader);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.analysis.util.TokenizerFactory#create(org.apache.lucene.
     * util.AttributeFactory)
     */
    @Override
    public MtasTokenizer create(AttributeFactory factory)
    {
        MtasTokenizer tokenizer = null;
        try {
            tokenizer = create(factory, null);
        }
        catch (IOException e) {
            log.error("Error", e);
        }
        return tokenizer;
    }

    public MtasTokenizer create(String configuration) throws IOException
    {
        return create(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, configuration);
    }

    /**
     * Creates the.
     *
     * @param configuration
     *            the configuration
     * @return the mtas tokenizer
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasTokenizer create(String configuration, String defaultConfiguration)
        throws IOException
    {
        return create(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, configuration,
                defaultConfiguration);
    }

    public MtasTokenizer create(AttributeFactory factory, String configuration) throws IOException
    {
        return create(factory, configuration, null);
    }

    /**
     * Creates the.
     *
     * @param factory
     *            the factory
     * @param configuration
     *            the configuration
     * @return the mtas tokenizer
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public MtasTokenizer create(AttributeFactory factory, String configuration,
            String defaultConfiguration)
        throws IOException
    {
        if (defaultConfiguration == null) {
            defaultConfiguration = defaultArgument;
        }
        if (configs != null && configs.size() > 0) {
            if (configuration == null && defaultConfiguration == null) {
                throw new IOException("no (default)configuration");
            }
            else if (configuration == null) {
                if (configs.get(defaultConfiguration) != null) {
                    return new MtasTokenizer(factory, configs.get(defaultConfiguration));
                }
                else {
                    throw new IOException(
                            "default configuration " + defaultConfiguration + " not available");
                }
            }
            else {
                MtasConfiguration config = configs.get(configuration);
                if (config == null) {
                    if (defaultConfiguration != null) {
                        if (configs.get(defaultConfiguration) != null) {
                            return new MtasTokenizer(factory, configs.get(defaultConfiguration));
                        }
                        else {
                            throw new IOException("configuration " + configuration
                                    + " not found and default configuration " + defaultConfiguration
                                    + " not available");
                        }
                    }
                    else {
                        throw new IOException("configuration " + configuration
                                + " not available and no default configuration");
                    }
                }
                else {
                    return new MtasTokenizer(factory, config);
                }
            }
        }
        else if (config != null) {
            return new MtasTokenizer(factory, config);
        }
        else {
            throw new IOException("no configuration");
        }
    }

    /**
     * Inits the.
     *
     * @param resourceLoader
     *            the resource loader
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    private void init(ResourceLoader resourceLoader) throws IOException
    {
        if (config == null && configs == null) {
            if (resourceLoader == null) {
                return;
            }
            else if (configFileArgument == null && configArgument == null
                    && analyzerArgument == null) {
                throw new IOException("no configuration");
            }
            else {
                if (configFileArgument != null) {
                    try {
                        config = MtasConfiguration
                                .readConfiguration(resourceLoader.openResource(configFileArgument));
                    }
                    catch (IOException e) {
                        throw new IOException(
                                "Problem loading configuration from " + configFileArgument, e);
                    }
                }
                if (configArgument != null) {
                    try {
                        configs = MtasConfiguration.readMtasTokenizerConfigurations(resourceLoader,
                                configArgument);
                    }
                    catch (IOException e) {
                        throw new IOException(
                                "Problem loading configurations from " + configArgument, e);
                    }
                }
                if (analyzerArgument != null) {
                    configs = null;
                    config = new MtasConfiguration();
                    MtasConfiguration subConfig = new MtasConfiguration();
                    subConfig.name = "parser";
                    subConfig.attributes.put("name", analyzerArgument);
                    subConfig.attributes.put(ARGUMENT_PARSER_ARGS, analyzerArgumentParserArgs);
                    config.children.add(subConfig);
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.analysis.util.ResourceLoaderAware#inform(org.apache.
     * lucene.analysis.util.ResourceLoader)
     */
    @Override
    public void inform(ResourceLoader loader) throws IOException
    {
        init(loader);
    }

}
