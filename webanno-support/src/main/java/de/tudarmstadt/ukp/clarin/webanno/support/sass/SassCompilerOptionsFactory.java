package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import io.bit3.jsass.Options;

/**
 * Factory that creates a {@link io.bit3.jsass.Options}. This enables the users to set application
 * specific configurations to the {@link io.bit3.jsass.Compiler}.
 */
public interface SassCompilerOptionsFactory
{
    Options newOptions();
}
