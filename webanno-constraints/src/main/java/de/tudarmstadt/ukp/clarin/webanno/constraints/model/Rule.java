package de.tudarmstadt.ukp.clarin.webanno.constraints.model;

import java.util.List;

public class Rule
{
    private final List<Condition> conditions;
    private final List<Restriction> restrictions;

    public Rule(List<Condition> aConditions, List<Restriction> aRestrictions)
    {
        conditions = aConditions;
        restrictions = aRestrictions;
    }

    public List<Condition> getConditions()
    {
        return conditions;
    }

    public List<Restriction> getRestrictions()
    {
        return restrictions;
    }

    @Override
    public String toString()
    {
        return "Rule [conditions=" + conditions + ", restrictions=" + restrictions + "]";
    }
}
