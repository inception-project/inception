package de.tudarmstadt.ukp.inception.kb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("inception.knowledge-base")
public class KnowledgeBaseProperties
{
    private int sparqlQueryResultLimit = 1000;

    public int getSparqlQueryResultLimit()
    {
        return sparqlQueryResultLimit;
    }

    public void setSparqlQueryResultLimit(int aSparqlQueryResultLimit)
    {
        sparqlQueryResultLimit = aSparqlQueryResultLimit;
    }
}
