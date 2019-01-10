package de.tudarmstadt.ukp.clarin.webanno.support.bootstrap.select;

import java.util.Arrays;

import de.agilecoders.wicket.jquery.AbstractConfig;
import de.agilecoders.wicket.jquery.IKey;

/**
 * Bootstrap select config
 *
 * @author Alexey Volkov
 * @since 02.11.14
 */
public class BootstrapSelectConfig extends AbstractConfig {

    private static final IKey<Boolean> LiveSearch = newKey("liveSearch", false);
    private static final IKey<String> LiveSearchStyle = newKey("liveSearchStyle", "contains");
    private static final IKey<Boolean> Multiple = newKey("multiple", false);
    private static final IKey<Integer> MaxOptions = newKey("maxOptions", null);
    private static final IKey<Object[]> MaxOptionsText = newKey("maxOptionsText", new Object[] {
            "Limit reached ({n} {var} max)",
            "Group limit reached ({n} {var} max)",
            new Object[] { "items", "item" }
    });

    private static final IKey<String> SelectedTitle = newKey("selectedTitle", null);

    private static final IKey<String> NoneSelectedText = newKey("noneSelectedText", null);
    private static final IKey<String> NoneResultsText = newKey("noneResultsText", null);
    private static final IKey<String> CountSelectedText = newKey("countSelectedText", null);

    private static final IKey<Boolean> ActionsBox = newKey("actionsBox", false);
    private static final IKey<String> SelectAllText = newKey("selectAllText", null);
    private static final IKey<String> DeselectAllText = newKey("deselectAllText", null);

    private static final long serialVersionUID = -1083532309683379273L;

    /**
     * @param liveSearch is live search
     * @return current instance
     */
    public BootstrapSelectConfig withLiveSearch(Boolean liveSearch) {
        put(LiveSearch, liveSearch);
        return this;
    }

    /**
     * @param liveSearchStyle search style (contains or startsWith/begins)
     * @return current instance
     */
    public BootstrapSelectConfig withLiveSearchStyle(String liveSearchStyle) {
        put(LiveSearchStyle, liveSearchStyle);
        return this;
    }

    /**
     * @param multiple is multiple
     * @return current instance
     */

    public BootstrapSelectConfig withMultiple(Boolean multiple) {
        put(Multiple, multiple);
        return this;
    }

    /**
     * @param maxOptions maxOptions
     * @return current instance
     */
    public BootstrapSelectConfig withMaxOptions(Integer maxOptions) {
        put(MaxOptions, maxOptions);
        return this;
    }

    /**
     * @param maxOptionsText maxOptionsText
     * @param groupText group text
     * @param singleItem item title
     * @param multiItem items title
     * @return current instance
     */
    public BootstrapSelectConfig withMaxOptionsText(String maxOptionsText, String groupText,
            String singleItem, String multiItem) {
        Object[] clone = get(MaxOptionsText).clone();
        clone[0] = maxOptionsText;
        clone[1] = groupText;
        Object[] labels = Arrays.copyOf((Object[]) clone[2], 2);
        labels[0] = multiItem;
        labels[1] = singleItem;
        clone[2] = labels;
        put(MaxOptionsText, clone);
        return this;
    }

    /**
     * @param noResultText no result text
     * @return current instance
     */
    public BootstrapSelectConfig withNoResultText(String noResultText) {
        put(NoneResultsText, noResultText);
        return this;
    }

    /**
     * @param title title
     * @return current instance
     */
    public BootstrapSelectConfig withSelectedTitle(String title) {
        put(SelectedTitle, title);
        return this;
    }

    /**
     * @param text text
     * @return current instance
     */
    public BootstrapSelectConfig withNoneSelectedText(String text) {
        put(NoneSelectedText, text);
        return this;
    }

    /**
     * @param text text
     * @return current instance
     */
    public BootstrapSelectConfig withCountSelectedText(String text) {
        put(CountSelectedText, text);
        return this;
    }
    
    /**
     * Shows or hides actions box with select all / deselect all buttons. 
     * Applicable only for {@code BootstrapMultiSelect}.
     * 
     * @param actionsBox is Actions box
     * @return current instance
     */
    public BootstrapSelectConfig withActionsBox(Boolean actionsBox) {
        put(ActionsBox, actionsBox);
        return this;
    }
    

    /**
     * Sets text for select all button in case actions box is turned on. 
     * Applicable only for {@code BootstrapMultiSelect}.
     * 
     * @see BootstrapSelectConfig#withActionsBox(Boolean)
     * 
     * @param selectAllText text displayed on selectAll button
     * @return current instance
     */
    public BootstrapSelectConfig withSelectAllText(String selectAllText) {
        put(SelectAllText, selectAllText);
        return this;
    }
    
    /**
     * Sets text for deselect all button in case actions box is turned on. 
     * Applicable only for {@code BootstrapMultiSelect}.
     * 
     * @see BootstrapSelectConfig#withActionsBox(Boolean)
     * 
     * @param deselectAllText text displayed on deselectAll button
     * @return current instance
     */
    public BootstrapSelectConfig withDeselectAllText(String deselectAllText) {
        put(DeselectAllText, deselectAllText);
        return this;
    }

    /**
     * @return selected title
     */
    public String getSelectedTitle() {
        return get(SelectedTitle);
    }

    /**
     * @return is live search
     */
    public Boolean getLiveSearch() {
        return get(LiveSearch);
    }

    /**
     * @return is multiple
     */
    public Boolean getMultiple() {
        return get(Multiple);
    }

    /**
     * @return max options
     */
    public Integer getMaxOptions() {
        return get(MaxOptions);
    }

    /**
     * @return no result text
     */
    public String getNoResultText() {
        return get(NoneResultsText);
    }

    /**
     * @return no selected text
     */
    public String getNoneSelectedText() {
        return get(NoneSelectedText);
    }

    /**
     * @return count selected text
     */
    public String getCountSelectedText() {
        return get(CountSelectedText);
    }

    /**
     * @return actions box
     */
    public boolean getActionsBox() {
        return get(ActionsBox);
    }

    /**
     * @return select all text
     */
    public String getSelectAllText() {
        return get(SelectAllText);
    }

    /**
     * @return select all text
     */
    public String getDeselectAllText() {
        return get(DeselectAllText);
    }

    /**
     * @return max options text
     */
    public String getMaxOPtionsText() {
        return (String) get(MaxOptionsText)[0];
    }
}
