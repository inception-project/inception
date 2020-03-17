package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import io.bit3.jsass.Options;

/**
 * Returns just a plain {@link io.bit3.jsass.Options} without any additional configuration.
 */
public class SimpleSassCompilerOptionsFactory
    implements SassCompilerOptionsFactory
{

    @Override
    public Options newOptions()
    {
        return new Options();
    }

}
