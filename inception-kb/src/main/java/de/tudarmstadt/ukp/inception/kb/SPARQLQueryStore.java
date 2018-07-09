package de.tudarmstadt.ukp.inception.kb;

public class SPARQLQueryStore
{
    
    public static String aLimit = "1000";
    public static String PROPERTYLIST_RDF_QUERY = String.join("\n"
            , "SELECT DISTINCT ?s ?l WHERE {"
            , "  { ?s ?pTYPE ?oPROPERTY .}"
            , "  UNION "
            , "  { ?s a ?prop" 
            , "    FILTER regex(str(?prop), \"Property$\") }"
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
    
    
}
