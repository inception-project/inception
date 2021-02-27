package de.tudarmstadt.ukp.inception.editor;

import de.tudarmstadt.ukp.clarin.webanno.brat.resource.JQueryJsonResourceReference;
import org.apache.wicket.request.resource.JavaScriptResourceReference;

public class JQueryAnnotationEditorReference
        extends JavaScriptResourceReference
    {
        private static final long serialVersionUID = 1L;

        private static final JQueryAnnotationEditorReference INSTANCE = new JQueryAnnotationEditorReference();

        /**
         * Gets the instance of the resource reference
         *
         * @return the single instance of the resource reference
         */
        public static JQueryAnnotationEditorReference get()
        {
            return INSTANCE;
        }

        /**
         * Private constructor
         */
    private JQueryAnnotationEditorReference()
        {
            super(JQueryJsonResourceReference.class, "AnnotationEditor.js");
        }
}
