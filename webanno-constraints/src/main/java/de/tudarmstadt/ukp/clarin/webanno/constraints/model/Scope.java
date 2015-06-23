package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import java.util.List;

public class Scope
{

    private final String scopeName;
    private final List<Rule> rules;

    /**
     * @param scopeName
     * @param rules
     */
    public Scope(String scopeName, List<Rule> rules)
    {
        this.scopeName = scopeName;
        this.rules = rules;
    }

    public String getScopeName()
    {
        return scopeName;
    }

    public List<Rule> getRules()
    {
        return rules;
    }

    @Override
    public String toString()
    {
        return "Scope [" + scopeName + "]\nRules\n" + rules.toString() + "]";
    }

}
