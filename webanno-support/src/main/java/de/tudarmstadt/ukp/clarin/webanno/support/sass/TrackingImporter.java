package de.tudarmstadt.ukp.clarin.webanno.support.sass;

import java.util.ArrayList;
import java.util.Collection;

import io.bit3.jsass.importer.Import;
import io.bit3.jsass.importer.Importer;

/**
 * Importer that tracks nested imports in Sass resources.
 */
class TrackingImporter
    implements Importer
{

    /**
     * Collection where nested imports are stored.
     */
    private final Collection<SassSource> importedSources;

    /**
     * The scope class used with SassResourceReference.
     */
    private final String scopeClass;

    /**
     * The importer that actually imports Sass sources.
     */
    private final Importer delegate;

    /**
     *
     * @param scopeClass
     *            The scope class used with SassResourceReference.
     * @param delegate
     *            The importer that actually imports Sass sources.
     */
    public TrackingImporter(String scopeClass, Importer delegate)
    {
        this.scopeClass = scopeClass;
        this.delegate = delegate;
        this.importedSources = new ArrayList<>();
    }

    /**
     * Returns collection of Sass source that were imported during last compilation.
     *
     * @return collection of Sass sources imported during last compilation
     */
    public Collection<SassSource> getImportedSources()
    {
        return importedSources;
    }

    @Override
    public Collection<Import> apply(String url, Import previous)
    {
        importedSources.clear();

        Collection<Import> imports = delegate.apply(url, previous);

        if (imports != null && !imports.isEmpty()) {
            imports.stream().findFirst().map(Import::getImportUri)
                    .ifPresent(importUri -> this.importedSources
                            .add(new SassSource(importUri.toString(), scopeClass)));
        }

        return imports;
    }
}
