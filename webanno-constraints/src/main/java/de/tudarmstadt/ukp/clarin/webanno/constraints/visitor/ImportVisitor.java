package de.tudarmstadt.ukp.clarin.webanno.constraints.visitor;

import java.util.Map;

import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.syntaxtree.ImportDeclaration;
import de.tudarmstadt.ukp.clarin.webanno.constraints.grammar.visitor.GJVoidDepthFirst;

public class ImportVisitor
    extends GJVoidDepthFirst<Map<String, String>>
{
    @Override
    public void visit(ImportDeclaration aN, Map<String, String> aArgu)
    {
        super.visit(aN, aArgu);

        aArgu.put(aN.f3.tokenImage, aN.f1.tokenImage);
    }
}
