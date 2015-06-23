package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/***
 * Class containing objects after parsing and creating objects based on rules file
 * 
 * @author aakash
 *
 */
public class ParsedConstraints
{

    private final Map<String, String> imports;
    private final List<Scope> scopes;
    private Map<String, Scope> scopeMap = null;

    /**
     * @param imports
     * @param scopes
     */
    public ParsedConstraints(Map<String, String> imports, List<Scope> scopes)
    {
        this.imports = imports;
        this.scopes = scopes;
    }

    @Override
    public String toString()
    {
        return "Imports\n[" + printImports() + "]\n" + scopes.toString() + "]\n]";
    }

    private String printImports()
    {

        StringBuffer output = new StringBuffer();
        for (Entry<String, String> e : imports.entrySet()) {
            output.append(e.getKey());
            output.append(" is short for ");
            output.append(e.getValue());
            output.append(System.lineSeparator());
        }
        return output.toString();
    }

    public Map<String, String> getImports()
    {
        return imports;
    }

    public String getShortName(String aLongName)
    {
        for (Entry<String, String> e : imports.entrySet()) {
            if (e.getValue().equals(aLongName)) {
                return e.getKey();
            }
        }
        return null;
    }

    public List<Scope> getScopes()
    {
        return scopes;
    }

    public Scope getScopeByName(String scopeName)
    {

        if (scopeMap == null) { // initialize map if not set already
            scopeMap = new HashMap<String, Scope>();
            for (Scope scope : scopes) {
                scopeMap.put(scope.getScopeName(), scope);
            }
        }
        return scopeMap.get(scopeName);
    }

}
