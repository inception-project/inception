package de.tudarmstadt.ukp.inception.io.jsoncas.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("format.json-cas-legacy")
public class LegacyUimaJsonCasFormatProperties
{
    private boolean omitDefaultValues = false;

    public void setOmitDefaultValues(boolean aOmitDefaultValues)
    {
        omitDefaultValues = aOmitDefaultValues;
    }

    public boolean isOmitDefaultValues()
    {
        return omitDefaultValues;
    }
}
