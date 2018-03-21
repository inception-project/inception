/*
 * Copyright 2018
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
package de.tudarmstadt.ukp.inception.ui.kb.stmt.editor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.tudarmstadt.ukp.inception.kb.model.KnowledgeBase;

/**
 * Aggregates several {@link DatatypeSupport}s.<br>
 * Prefers editors/presenters for specific XML Schema datatypes over generic
 * {@link InstanceDatatypeSupport}s. As a last resort, {@link FallbackEditorPresenter} is used.
 */
public class MetaDatatypeSupport implements DatatypeSupport {
    
    private static final long serialVersionUID = -6786456814387451088L;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDatatypeSupport.class);

    private final List<DatatypeSupport> supports;
    
    public MetaDatatypeSupport(KnowledgeBase kb) {
        supports = new ArrayList<>();
        supports.add(new XmlSchemaDatatypeSupport());
        supports.add(new ClassDatatypeSupport(kb.getClassIri()));
    }
    
    /**
     * @return {@code true}
     */
    @Override
    public boolean isSupported(IRI datatype) {
        return true;
    }
    
    /**
     * @return {@code true}
     */
    @Override
    public boolean isValid(IRI datatype, Value value) {
        return true;
    }

    @Override
    public ValueEditor<?> createEditor(IRI datatype, String id, IModel<Value> model) {
        return consultSupports(datatype, model, (sup) -> sup.createEditor(datatype, id, model),
                () -> new InvalidStatementValueEditorPresenter(id),
                () -> new FallbackEditorPresenter(id, model, true));
    }

    @Override
    public WebMarkupContainer createPresenter(IRI datatype, String id, IModel<Value> model) {
        // TODO check another time what W3 recommend for uninterpretable statements
        return consultSupports(datatype, model, (sup) -> sup.createPresenter(datatype, id, model),
                () -> new InvalidStatementValueEditorPresenter(id),
                () -> new FallbackEditorPresenter(id, model, false));
    }

    private <T> T consultSupports(IRI datatype, IModel<Value> model,
            Function<DatatypeSupport, T> supplyIfValid, Supplier<T> supplyIfInvalid,
            Supplier<T> supplyFallback) {
        if (datatype == null) {
            LOGGER.warn("Null datatype for " + model.getObject().toString());
            return supplyFallback.get();
        }
        for (DatatypeSupport sup : supports) {
            if (sup.isSupported(datatype)) {
                if (sup.isValid(datatype, model.getObject())) {
                    return supplyIfValid.apply(sup);
                }
                return supplyIfInvalid.get();
            }
        }
        LOGGER.warn("Unsupported datatype " + datatype.toString() + " for " + model.getObject().toString());
        return supplyFallback.get();
    }
}
