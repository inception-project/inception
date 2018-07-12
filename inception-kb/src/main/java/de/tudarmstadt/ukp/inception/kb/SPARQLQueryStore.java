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
    
    // Add queries if needed for different KBs 
    
    
}
