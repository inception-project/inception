package de.tudarmstadt.ukp.inception.kb;

public class SPARQLQueryStore
{
    
    public static String aLimit = "1000";
    
    public static String PROPERTYLIST_QUERY = String.join("\n"
            , InferencerVariableStore.RDF_PREFIX
            , InferencerVariableStore.OWL_PREFIX
            , "SELECT DISTINCT ?s ?l WHERE {"
            , "  { ?s ?pTYPE ?oPROPERTY .}"
            , "  UNION "
            , "  { ?s a ?prop" 
            , "    VALUES ?prop { rdf:Property owl:ObjectProperty owl:DatatypeProperty owl:AnnotationProperty} }"
            , "  OPTIONAL {"
            , "    ?s ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT 10000");
    
    public static String PROPERTYLIST_WIKIDATA_QUERY = String.join("\n"
            , "SELECT DISTINCT ?s ?l WHERE {"
            , " ?s ?pTYPE ?oPROPERTY ."
            , "  OPTIONAL {"
            , "    ?s ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT 10000");
    
    
    public static String PROPERTYLIST_DOMAIN_DEPENDENT = String.join("\n"
            , InferencerVariableStore.RDF_PREFIX
            , InferencerVariableStore.OWL_PREFIX
            , "SELECT DISTINCT ?s ?l WHERE {"
            , "  ?s rdfs:domain/(owl:unionOf/rdf:rest*/rdf:first)* ?aDomain "
            , "  OPTIONAL {"
            , "    ?s ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT 10000");
    
    public static String PROPERTY_SPECIFIC_RANGE = String.join("\n"
            , InferencerVariableStore.RDF_PREFIX
            , InferencerVariableStore.OWL_PREFIX
            , "SELECT DISTINCT ?s ?l WHERE {"
            , "  ?aProperty rdfs:range/(owl:unionOf/rdf:rest*/rdf:first)* ?s "
            , "  OPTIONAL {"
            , "    ?aProperty ?pLABEL ?l ."
            , "    FILTER(LANG(?l) = \"\" || LANGMATCHES(LANG(?l), \"en\"))"
            , "  }"
            , "}"
            , "LIMIT 10000");
    
    
}
