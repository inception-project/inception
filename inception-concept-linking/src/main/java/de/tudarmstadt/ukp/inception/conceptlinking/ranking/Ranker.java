package de.tudarmstadt.ukp.inception.conceptlinking.ranking;

import java.util.List;
import java.util.Set;

import org.apache.uima.cas.CAS;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;

public interface Ranker {

    List<KBHandle> rank(String aQuery, String aMention, Set<KBHandle> aCandidates, CAS aCas, int aBeginOffset);
}
