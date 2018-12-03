/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.ui.kb;

import java.util.Comparator;
import java.util.function.Function;

import de.tudarmstadt.ukp.inception.kb.graph.KBHandle;
import de.tudarmstadt.ukp.inception.ui.kb.stmt.StatementGroupBean;

/**
 * Comparator which sorts specifiable "important" {@link KBHandle}s to the front. As a secondary
 * criterion, {@link KBHandle}s are sorted in lexical order by their UI label.
 */
public class ImportantStatementComparator implements Comparator<StatementGroupBean> {

    private Function<StatementGroupBean, Boolean> important;

    public ImportantStatementComparator(Function<StatementGroupBean, Boolean> important) {
        this.important = important;
    }

    @Override
    public int compare(StatementGroupBean h1, StatementGroupBean h2) {
        int h1Importance = important.apply(h1) ? 0 : 1;
        int h2Importance = important.apply(h2) ? 0 : 1;

        if (h1Importance == h2Importance) {
            return h1.getProperty().getUiLabel().compareToIgnoreCase(h2.getProperty().getUiLabel());
        } else {
            return Integer.compare(h1Importance, h2Importance);
        }
    }

    public Function<StatementGroupBean, Boolean> getImportant()
    {
        return important;
    }

    public void setImportant(Function<StatementGroupBean, Boolean> aImportant)
    {
        this.important = aImportant;
    }
}
