package org.jdownloader.gui.views.components;

import javax.swing.Icon;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public enum LinktablesSearchCategory implements SearchCatInterface, LabelInterface {

    FILENAME(_GUI._.searchcategory_filename(), "text", _GUI._.searchcategory_filename_help()),

    HOSTER(_GUI._.searchcategory_hoster(), "browse", _GUI._.searchcategory_hoster_help()),

    PACKAGE(_GUI._.searchcategory_package(), "package_open", _GUI._.searchcategory_package_help()),

    COMMENT(_GUI._.searchcategory_comment(), IconKey.ICON_LIST, _GUI._.searchcategory_comment_help()),

    STATUS(_GUI._.searchcategory_status(), IconKey.ICON_START, _GUI._.searchcategory_status_help()), ;

    private String label;
    private String iconKey;
    private String helptext;

    private LinktablesSearchCategory(String searchcategory_filename, String iconKey, String helptext) {
        label = searchcategory_filename;
        this.iconKey = iconKey;
        this.helptext = helptext;
    }

    public String getLabel() {
        return label;
    }

    public Icon getIcon() {
        return NewTheme.I().getIcon(iconKey, 18);
    }

    public String getHelpText() {
        return helptext;
    }

}
