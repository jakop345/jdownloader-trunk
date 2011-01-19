package jd.plugins.optional.folderwatch;

import java.awt.event.ActionEvent;

import javax.swing.JScrollPane;

import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.InfoPanel;
import jd.gui.swing.jdgui.views.ViewToolbar;
import jd.nutils.JDFlags;
import jd.plugins.optional.folderwatch.data.History;
import jd.plugins.optional.folderwatch.data.HistoryEntry;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class FolderWatchPanel extends SwitchPanel {

    private static final long           serialVersionUID = -4451556977039313203L;

    private static final String         JDL_PREFIX       = "plugins.optional.folderwatch.gui.";

    private final JDFolderWatch         fwInstance       = (JDFolderWatch) JDUtilities.getOptionalPlugin("folderwatch").getPlugin();

    private static FolderWatchTable     table;
    private static FolderWatchInfoPanel infoPanel;
    private static SubConfiguration     config;

    private static FolderWatchPanel     INSTANCE;

    private FolderWatchPanel() {
    }

    public static synchronized FolderWatchPanel getInstance() {
        if (INSTANCE == null) INSTANCE = new FolderWatchPanel();
        return INSTANCE;
    }

    public FolderWatchPanel(SubConfiguration config) {
        FolderWatchPanel.table = new FolderWatchTable();
        FolderWatchPanel.config = config;

        initActions();
        initGUI();
    }

    private void initGUI() {
        this.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[][grow,fill]"));
        this.add(new ViewToolbar("action.folderwatch.clear", "action.folderwatch.reimport"));
        this.add(new JScrollPane(table), "grow");
    }

    private void initActions() {
        new ThreadedAction("action.folderwatch.clear", "gui.images.clear") {
            private static final long serialVersionUID = 3349495273700955040L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {
                        if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN, JDL.L("action.folderwatch.clear.message", "Are you sure you want to clear the history?")), UserIO.RETURN_OK)) {
                            History.clear();
                            config.setProperty(FolderWatchConstants.PROPERTY_HISTORY, null);
                            config.save();
                            refresh();
                        }

                        return null;
                    }
                }.start();
            }
        };

        new ThreadedAction("action.folderwatch.reimport", "gui.images.add") {
            private static final long serialVersionUID = 9034432457172125570L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                new GuiRunnable<Object>() {
                    @Override
                    public Object runSave() {

                        if (table.getSelectedRowCount() > 0) {
                            int[] rows = table.getSelectedRows();

                            for (int row : rows) {
                                HistoryEntry container = (HistoryEntry) table.getValueAt(row, 2);
                                fwInstance.importContainer(container.getAbsolutePath());
                            }
                        }
                        return null;
                    }
                }.start();
            }
        };
    }

    @Override
    protected void onHide() {
    }

    @Override
    protected void onShow() {
        refresh();
    }

    public FolderWatchInfoPanel getInfoPanel() {
        if (infoPanel == null) {
            infoPanel = new FolderWatchInfoPanel("gui.images.addons.unrar");
        }

        return infoPanel;
    }

    public void refresh() {
        table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();

        getInfoPanel().update();
    }

    public class FolderWatchInfoPanel extends InfoPanel {

        private static final long serialVersionUID = -4944779193095436056L;

        public FolderWatchInfoPanel(String iconKey) {
            super(iconKey);

            addInfoEntry(JDL.L(JDL_PREFIX + "filestatus", "File status"), "", 0, 0);
        }

        public void update() {
            new GuiRunnable<Object>() {
                @Override
                public Object runSave() {
                    HistoryEntry container = (HistoryEntry) table.getValueAt(table.getSelectedRow(), 3);

                    String info = "";

                    if (container != null) {
                        container = History.updateEntry(container);

                        if (container.isExisting()) {
                            info = JDL.L(JDL_PREFIX + "filestatus.exists", "File exists");
                        } else {
                            info = JDL.L(JDL_PREFIX + "filestatus.notexists", "File does not exist");
                        }
                    }

                    updateInfo(JDL.L(JDL_PREFIX + "filestatus", "File status"), info);

                    return null;
                }
            }.start();
        }
    }

}
