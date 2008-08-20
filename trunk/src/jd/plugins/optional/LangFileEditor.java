//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.optional;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.gui.skins.simple.Link.JLinkButton;
import jd.gui.skins.simple.components.JDFileChooser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Editor for jDownloader language files. Gets JDLocale.L() and JDLocale.LF()
 * entries from source and compares them to the keypairs in the language file.
 * 
 * @author eXecuTe|Greeny
 */

public class LangFileEditor extends PluginOptional {

    private SubConfiguration subConfig = JDUtilities.getSubConfig("ADDONS_LANGFILEEDITOR");
    private static final String PROPERTY_FOLDER = "PROPERTY_FOLDER";
    private static final String PROPERTY_FILE = "PROPERTY_FILE";
    private static final String PROPERTY_COLORIZE_MISSING = "PROPERTY_COLORIZE_MISSING";
    private static final String PROPERTY_COLORIZE_OLD = "PROPERTY_COLORIZE_OLD";
    private static final String PROPERTY_MISSING_COLOR = "PROPERTY_MISSING_COLOR";
    private static final String PROPERTY_OLD_COLOR = "PROPERTY_OLD_COLOR";

    private JFrame frame;
    private File sourceFolder, languageFile;
    private JTextField txtFolder, txtFile;
    private JLabel lblEntriesCount;
    private JButton btnBrowseFile, btnBrowseFolder;
    private JMenu mnuFile, mnuKey, mnuEntries, mnuColorize;
    private JMenuItem mnuBrowseFile, mnuBrowseFolder, mnuDownloadSource, mnuReload, mnuSave, mnuSaveAs, mnuClose;
    private JMenuItem mnuAdd, mnuAdopt, mnuAdoptMissing, mnuClear, mnuDelete, mnuEdit, mnuTranslate, mnuTranslateMissing;
    private JMenuItem mnuPickMissingColor, mnuPickOldColor, mnuSelectMissing, mnuSelectOld, mnuShowDupes, mnuSort;
    private JRadioButtonMenuItem mnuColorizeMissing, mnuColorizeOld;
    private JTable table;
    private MyTableModel tableModel;

    private Vector<String[]> data = new Vector<String[]>();
    private Vector<String> oldEntries = new Vector<String>();
    private Vector<String[]> dupes = new Vector<String[]>();
    private String lngKey = null;

    public static void main(String args[]) {
        new LangFileEditor().showGui();
    }

    private void showGui() {

        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setTitle(JDLocale.L("plugins.optional.langfileeditor.title", "jDownloader - Language File Editor"));
        frame.setMinimumSize(new Dimension(800, 500));
        frame.setPreferredSize(new Dimension(1200, 700));
        frame.setName("LANGFILEEDIT");

        tableModel = new MyTableModel();
        table = new JTable(tableModel);
        table.setDefaultRenderer(String.class, new MyTableCellRenderer());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);

        JPanel main = new JPanel(new BorderLayout(5, 5));
        main.setBorder(new EmptyBorder(10, 10, 10, 10));
        frame.setContentPane(main);

        JPanel top = new JPanel(new BorderLayout(5, 5));
        JPanel top1 = new JPanel(new BorderLayout(5, 5));
        JPanel top2 = new JPanel(new BorderLayout(5, 5));

        top.add(top1, BorderLayout.PAGE_START);
        top.add(top2, BorderLayout.PAGE_END);

        top1.add(new JLabel(JDLocale.L("plugins.optional.langfileeditor.sourceFolder", "Source Folder: ")), BorderLayout.LINE_START);
        sourceFolder = (File) subConfig.getProperty(PROPERTY_FOLDER);
        top1.add(txtFolder = new JTextField((sourceFolder != null) ? sourceFolder.getAbsolutePath() : JDLocale.L("plugins.optional.langfileeditor.sourceFolder.select", "<Please select the Source Folder!>")), BorderLayout.CENTER);
        top1.add(btnBrowseFolder = new JButton(JDLocale.L("plugins.optional.langfileeditor.browse", "Browse")), BorderLayout.EAST);
        txtFolder.setEditable(false);
        btnBrowseFolder.addActionListener(this);
        frame.getRootPane().setDefaultButton(btnBrowseFolder);

        top2.add(new JLabel(JDLocale.L("plugins.optional.langfileeditor.languageFile", "Language File: ")), BorderLayout.LINE_START);
        languageFile = (File) subConfig.getProperty(PROPERTY_FILE);
        top2.add(txtFile = new JTextField((languageFile != null) ? languageFile.getAbsolutePath() : JDLocale.L("plugins.optional.langfileeditor.languageFile.select", "<Please select a Language File!>")), BorderLayout.CENTER);
        top2.add(btnBrowseFile = new JButton(JDLocale.L("plugins.optional.langfileeditor.browse", "Browse")), BorderLayout.EAST);
        txtFile.setEditable(false);
        btnBrowseFile.addActionListener(this);

        main.add(top, BorderLayout.PAGE_START);
        main.add(new JScrollPane(table), BorderLayout.CENTER);
        main.add(lblEntriesCount = new JLabel(JDLocale.L("plugins.optional.langfileeditor.entriesCount", "Entries Count:")), BorderLayout.PAGE_END);

        buildMenu();

        frame.setResizable(true);
        frame.pack();
        frame.setVisible(true);

        if (sourceFolder != null || languageFile != null) initList();
    }

    private void initList() {

        this.initLocaleData();
        mnuEntries.setEnabled(true);
        mnuKey.setEnabled(true);
        mnuReload.setEnabled(true);
        mnuSave.setEnabled(true);
        mnuSaveAs.setEnabled(true);

    }

    private void setInfoLabels() {

        int numSource = 0, numFile = 0, numMissing = 0, numOld = 0;

        for (String[] entry : data) {

            if (entry[1] != "" && entry[2] != "") {
                numSource++;
                numFile++;
            } else if (entry[1].equals("")) {
                numFile++;
                numOld++;
            } else if (entry[2].equals("")) {
                numSource++;
                numMissing++;
            }

        }

        lblEntriesCount.setText(JDLocale.LF("plugins.optional.langfileeditor.entriesCount.extended", "Entries Count:     [Sourcecode] %s     [Language File] %s     [Missing] %s     [Not found / no Default] %s     [Probably old] %s     [Probably dupes] %s", numSource, numFile, numMissing, numOld, oldEntries.size(), dupes.size()));

    }

    private void buildMenu() {
        // File Menü
        mnuFile = new JMenu(JDLocale.L("plugins.optional.langfileeditor.file", "File"));

        mnuFile.add(mnuBrowseFolder = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.browseSourceFolder", "Browse Source Folder")));
        mnuFile.add(mnuBrowseFile = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.browseLanguageFile", "Browse Language File")));
        mnuFile.addSeparator();
        mnuFile.add(mnuReload = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.reload", "Reload")));
        mnuFile.addSeparator();
        mnuFile.add(mnuSave = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.save", "Save")));
        mnuFile.add(mnuSaveAs = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.saveAs", "Save As")));
        mnuFile.addSeparator();
        mnuFile.add(mnuDownloadSource = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.download", "Download SourceCode")));
        mnuFile.addSeparator();
        mnuFile.add(mnuClose = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.close", "Close")));

        mnuBrowseFolder.addActionListener(this);
        mnuBrowseFile.addActionListener(this);
        mnuReload.addActionListener(this);
        mnuReload.setEnabled(false);
        mnuSave.addActionListener(this);
        mnuSave.setEnabled(false);
        mnuSaveAs.addActionListener(this);
        mnuSaveAs.setEnabled(false);
        mnuDownloadSource.addActionListener(this);
        mnuClose.addActionListener(this);

        // Key Menü
        mnuKey = new JMenu(JDLocale.L("plugins.optional.langfileeditor.key", "Key"));
        mnuKey.setEnabled(false);

        mnuKey.add(mnuAdd = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.addKey", "Add Key")));
        mnuKey.add(mnuDelete = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.deleteSelectedKeys", "Delete Selected Key(s)")));
        mnuKey.addSeparator();
        mnuKey.add(mnuEdit = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.editSelectedValues", "Edit Selected Value(s)")));
        mnuKey.add(mnuClear = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.clearAllValues", "Clear All Values")));
        mnuKey.addSeparator();
        mnuKey.add(mnuAdopt = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.adoptDefaults", "Adopt Default(s)")));
        mnuKey.add(mnuAdoptMissing = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.adoptDefaults.missing", "Adopt Defaults of Missing Entries")));
        mnuKey.add(mnuTranslate = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.translate", "Translate with Google")));
        mnuKey.add(mnuTranslateMissing = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.translate.missing", "Translate Missing Entries with Google")));

        mnuAdd.addActionListener(this);
        mnuDelete.addActionListener(this);
        mnuEdit.addActionListener(this);
        mnuClear.addActionListener(this);
        mnuAdopt.addActionListener(this);
        mnuAdoptMissing.addActionListener(this);
        mnuTranslate.addActionListener(this);
        mnuTranslateMissing.addActionListener(this);

        // Entries Menü
        mnuEntries = new JMenu(JDLocale.L("plugins.optional.langfileeditor.entries", "Entries"));
        mnuEntries.setEnabled(false);

        mnuEntries.add(mnuColorize = new JMenu(JDLocale.L("plugins.optional.langfileeditor.colorize", "Colorize")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuSelectMissing = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.selectMissing", "Select Missing Entries")));
        mnuEntries.add(mnuSelectOld = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.selectOld", "Select Old Entries")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuShowDupes = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.showDupes", "Show Dupes")));
        mnuEntries.addSeparator();
        mnuEntries.add(mnuSort = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.sortEntries", "Sort Entries")));

        mnuSelectMissing.setEnabled(!subConfig.getBooleanProperty(PROPERTY_COLORIZE_MISSING, false));
        mnuSelectOld.setEnabled(!subConfig.getBooleanProperty(PROPERTY_COLORIZE_OLD, false));

        mnuSelectMissing.addActionListener(this);
        mnuSelectOld.addActionListener(this);
        mnuShowDupes.addActionListener(this);
        mnuSort.addActionListener(this);

        // Colorize Menü
        mnuColorize.add(mnuColorizeMissing = new JRadioButtonMenuItem(JDLocale.L("plugins.optional.langfileeditor.colorizeMissing", "Colorize Missing Entries")));
        mnuColorize.add(mnuPickMissingColor = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.pickMissingColor", "Pick Color for Missing Entries")));
        mnuColorize.addSeparator();
        mnuColorize.add(mnuColorizeOld = new JRadioButtonMenuItem(JDLocale.L("plugins.optional.langfileeditor.colorizeOld", "Colorize Old Entries")));
        mnuColorize.add(mnuPickOldColor = new JMenuItem(JDLocale.L("plugins.optional.langfileeditor.pickOldColor", "Pick Color for Old Entries")));

        mnuColorizeMissing.setSelected(subConfig.getBooleanProperty(PROPERTY_COLORIZE_MISSING, false));
        mnuColorizeOld.setSelected(subConfig.getBooleanProperty(PROPERTY_COLORIZE_OLD, false));

        mnuColorizeMissing.addActionListener(this);
        mnuPickMissingColor.addActionListener(this);
        mnuColorizeOld.addActionListener(this);
        mnuPickOldColor.addActionListener(this);

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(mnuFile);
        menuBar.add(mnuKey);
        menuBar.add(mnuEntries);
        frame.setJMenuBar(menuBar);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() instanceof MenuItem && ((MenuItem) e.getSource()).getActionID() == 0) {

            if (frame == null || !frame.isVisible()) {
                showGui();
            } else {
                frame.toFront();
            }

        } else if (e.getSource() == btnBrowseFolder || e.getSource() == mnuBrowseFolder) {

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_SRC");

            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile().isDirectory()) {
                sourceFolder = chooser.getSelectedFile();
                txtFolder.setText(sourceFolder.getAbsolutePath());
                initList();

                subConfig.setProperty(PROPERTY_FOLDER, sourceFolder);
                subConfig.save();
                frame.getRootPane().setDefaultButton(btnBrowseFile);
            }

        } else if (e.getSource() == btnBrowseFile || e.getSource() == mnuBrowseFile) {

            JDFileChooser chooser = new JDFileChooser("LANGFILEEDITOR_LNG");
            chooser.setFileFilter(new LngFileFilter());

            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                languageFile = chooser.getSelectedFile();
                txtFile.setText(languageFile.getAbsolutePath());
                initList();

                subConfig.setProperty(PROPERTY_FILE, languageFile);
                subConfig.save();
                frame.getRootPane().setDefaultButton(btnBrowseFolder);
            }

        } else if (e.getSource() == mnuSave) {

            saveLanguageFile(languageFile);

        } else if (e.getSource() == mnuSaveAs) {

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogType(JFileChooser.SAVE_DIALOG);
            chooser.setDialogTitle(JDLocale.L("plugins.optional.langfileeditor.saveAs", "Save As"));
            chooser.setFileFilter(new LngFileFilter());
            chooser.setCurrentDirectory(languageFile.getParentFile());

            if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) saveLanguageFile(chooser.getSelectedFile());

        } else if (e.getSource() == mnuEdit) {

            int[] rows = table.getSelectedRows();

            for (int i = 0; i < rows.length; i++) {
                EditDialog dialog = new EditDialog(frame, data.get(rows[i]));
                if (dialog.value != null) tableModel.setValueAt(dialog.value, rows[i], 2);
            }

        } else if (e.getSource() == mnuDelete) {

            int[] rows = table.getSelectedRows();

            for (int i = rows.length - 1; i >= 0; --i) {
                int cur = rows[i];
                String temp = data.remove(cur)[0];
                oldEntries.remove(temp);
                for (int j = dupes.size() - 1; j >= 0; --j) {
                    if (dupes.get(j)[1].equals(temp) || dupes.get(j)[2].equals(temp)) dupes.remove(j);
                }
                tableModel.fireTableRowsDeleted(cur, cur);
            }

            setInfoLabels();

        } else if (e.getSource() == mnuAdd) {

            AddDialog dialog = new AddDialog(frame);

            if (dialog.key != null && dialog.value != null && !dialog.key.equals("") && !dialog.value.equals("")) {
                data.add(new String[] { dialog.key, "", dialog.value });
            }

            setInfoLabels();
            tableModel.fireTableRowsInserted(data.size() - 1, data.size() - 1);

        } else if (e.getSource() == mnuAdoptMissing) {

            for (int i = 0; i < data.size(); i++) {

                if (data.get(i)[2].equals("")) {
                    String def = data.get(i)[1];
                    if (!def.equals("")) tableModel.setValueAt(def, i, 2);
                }

            }

        } else if (e.getSource() == mnuReload) {

            initList();

        } else if (e.getSource() == mnuAdopt) {

            for (int i : table.getSelectedRows()) {
                String def = tableModel.getValueAt(i, 1);
                if (!def.equals("")) tableModel.setValueAt(def, i, 2);
            }

        } else if (e.getSource() == mnuClear) {

            for (int i = 0; i < table.getRowCount(); i++) {
                tableModel.setValueAt("", i, 2);
            }

        } else if (e.getSource() == mnuSelectMissing) {

            table.clearSelection();
            for (int i = 0; i < table.getRowCount(); i++) {
                if (tableModel.getValueAt(i, 2).equals("")) table.getSelectionModel().addSelectionInterval(i, i);
            }

        } else if (e.getSource() == mnuSelectOld) {

            table.clearSelection();
            for (int i = 0; i < table.getRowCount(); i++) {
                if (oldEntries.contains(table.getValueAt(i, 0))) table.getSelectionModel().addSelectionInterval(i, i);
            }

        } else if (e.getSource() == mnuColorizeMissing) {

            subConfig.setProperty(PROPERTY_COLORIZE_MISSING, mnuColorizeMissing.isSelected());
            subConfig.save();
            mnuSelectMissing.setEnabled(!mnuColorizeMissing.isSelected());
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuColorizeOld) {

            subConfig.setProperty(PROPERTY_COLORIZE_OLD, mnuColorizeOld.isSelected());
            subConfig.save();
            mnuSelectOld.setEnabled(!mnuColorizeOld.isSelected());
            tableModel.fireTableDataChanged();

        } else if (e.getSource() == mnuPickMissingColor) {

            Color newColor = JColorChooser.showDialog(frame, JDLocale.L("plugins.optional.langfileeditor.pickMissingColor", "Pick Color for Missing Entries"), (Color) subConfig.getProperty(PROPERTY_MISSING_COLOR, Color.RED));
            if (newColor != null) {
                subConfig.setProperty(PROPERTY_MISSING_COLOR, newColor);
                subConfig.save();
                tableModel.fireTableDataChanged();
            }

        } else if (e.getSource() == mnuPickOldColor) {

            Color newColor = JColorChooser.showDialog(frame, JDLocale.L("plugins.optional.langfileeditor.pickOldColor", "Pick Color for Old Entries"), (Color) subConfig.getProperty(PROPERTY_OLD_COLOR, Color.ORANGE));
            if (newColor != null) {
                subConfig.setProperty(PROPERTY_OLD_COLOR, newColor);
                subConfig.save();
                tableModel.fireTableDataChanged();
            }

        } else if (e.getSource() == mnuShowDupes) {

            new DupeDialog(frame, dupes);

        } else if (e.getSource() == mnuTranslateMissing) {

            if (lngKey == null) {
                String result = JOptionPane.showInputDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.message", "Please insert the Language Key to provide a correct translation of Google:"), JDLocale.L("plugins.optional.langfileeditor.translate.title", "Insert Language Key"), JOptionPane.QUESTION_MESSAGE);
                if (result == null) return;
                if (!isSupportedLanguageKey(result)) {
                    JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.error.message", "The Language Key, you have entered, is not supported by Google!"), JDLocale.L("plugins.optional.langfileeditor.translate.error.title", "Wrong Language Key!"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                lngKey = result;
            }

            for (int i = 0; i < data.size(); i++) {

                if (data.get(i)[2].equals("")) {

                    String def = data.get(i)[1];

                    if (!def.equals("")) {
                        logger.finer("Working on " + data.get(i)[0] + ":");
                        String result = JDLocale.translate(lngKey, def);
                        logger.finer("Default: \"" + def + "\" == Google: \"" + result + "\"");
                        tableModel.setValueAt(result, i, 2);
                    }

                }

            }

        } else if (e.getSource() == mnuTranslate) {

            if (lngKey == null) {
                String result = JOptionPane.showInputDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.message", "Please insert the Language Key to provide a correct translation of Google:"), JDLocale.L("plugins.optional.langfileeditor.translate.title", "Insert Language Key"), JOptionPane.QUESTION_MESSAGE);
                if (result == null) return;
                if (!isSupportedLanguageKey(result)) {
                    JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.translate.error.message", "The Language Key, you have entered, is not supported by Google!"), JDLocale.L("plugins.optional.langfileeditor.translate.error.title", "Wrong Language Key!"), JOptionPane.ERROR_MESSAGE);
                    return;
                }
                lngKey = result;
            }

            for (int i : table.getSelectedRows()) {

                String def = tableModel.getValueAt(i, 1);

                if (!def.equals("")) {
                    logger.finer("Working on " + data.get(i)[0] + ":");
                    String result = JDLocale.translate(lngKey, def);
                    logger.finer("Default: \"" + def + "\" == Google: \"" + result + "\"");
                    tableModel.setValueAt(result, i, 2);
                }

            }

        } else if (e.getSource() == mnuClose) {

            frame.setVisible(false);
            frame.dispose();

        } else if (e.getSource() == mnuSort) {

            Collections.sort(data, new StringArrayComparator());
            tableModel.fireTableRowsUpdated(0, data.size() - 1);

        } else if (e.getSource() == mnuDownloadSource) {

            try {
                JLinkButton.openURL("http://jdownloader.org/download");
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }

        }

    }

    private boolean isSupportedLanguageKey(String lngKey) {
        String[] lngKeys = new String[] { "da", "de", "fi", "fr", "el", "hi", "it", "ja", "ko", "hr", "nl", "no", "pl", "pt", "ro", "ru", "sv", "es", "cs", "en", "ar" };

        for (String element : lngKeys) {
            if (element.equals(lngKey)) return true;
        }
        return false;
    }

    private void saveLanguageFile(File file) {
        StringBuilder sb = new StringBuilder();

        for (String[] entry : data) {
            if (entry[2] != "") sb.append(entry[0] + " = " + entry[2] + "\n");
        }

        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            out.write(sb.toString());
            out.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, JDLocale.LF("plugins.optional.langfileeditor.save.error.message", "An error occured while writing the LanguageFile:\n%s", e.getMessage()), JDLocale.L("plugins.optional.langfileeditor.save.error.title", "Error!"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(frame, JDLocale.L("plugins.optional.langfileeditor.save.success.message", "LanguageFile saved successfully!"), JDLocale.L("plugins.optional.langfileeditor.save.success.title", "Save successful!"), JOptionPane.INFORMATION_MESSAGE);
    }

    private void initLocaleData() {

        String value;
        String[] temp;
        Vector<String[]> dupeHelp = new Vector<String[]>();
        data.clear();
        oldEntries.clear();
        dupes.clear();
        lngKey = null;

        Vector<String[]> sourceEntries = (sourceFolder == null) ? new Vector<String[]>() : getSourceEntries(sourceFolder);
        Vector<String[]> fileEntries = (languageFile == null) ? new Vector<String[]>() : getLanguageFileEntries(languageFile);

        for (String[] entry : sourceEntries) {

            temp = new String[] { entry[0], entry[1], getValue(fileEntries, entry[0]) };
            if (temp[2] == null) temp[2] = "";

            data.add(temp);
            if (temp[2] != "") {
                value = getValue(dupeHelp, temp[2]);
                if (value != null) dupes.add(new String[] { temp[2], temp[0], value });
                dupeHelp.add(new String[] { temp[2], temp[0] });
            }

        }

        for (String[] entry : fileEntries) {

            if (getValue(data, entry[0]) == null) {

                temp = new String[] { entry[0], "", entry[1] };

                data.add(temp);
                oldEntries.add(temp[0]);
                if (temp[2] != "") {
                    value = getValue(dupeHelp, temp[2]);
                    if (value != null) dupes.add(new String[] { temp[2], temp[0], value });
                    dupeHelp.add(new String[] { temp[2], temp[0] });
                }

            }

        }

        Collections.sort(data, new StringArrayComparator());
        tableModel.fireTableRowsInserted(0, data.size() - 1);
        setInfoLabels();

    }

    private String getValue(Vector<String[]> vector, String key) {

        String result = null;

        for (String[] entry : vector) {

            if (entry[0].equals(key)) {
                result = entry[1];
                break;
            }

        }

        return result;

    }

    private Vector<String[]> getLanguageFileEntries(File file) {

        Vector<String[]> entries = new Vector<String[]>();
        Vector<String> keys = new Vector<String>();

        String[][] matches = new Regex(Pattern.compile("(.*?)[\\s]*?=[\\s]*?(.*?)[\\r]?\\n").matcher(JDUtilities.getLocalFile(file))).getMatches();

        for (String[] match : matches) {

            match[0] = match[0].trim();
            match[1] = match[1].trim() + ((match[1].endsWith(" ")) ? " " : "");
            if (!keys.contains(match[0]) && !match[0].equals("") && !match[1].equals("")) {

                keys.add(Encoding.UTF8Decode(match[0]));
                entries.add(new String[] { Encoding.UTF8Decode(match[0]), Encoding.UTF8Decode(match[1]) });

            }

        }

        return entries;

    }

    private Vector<String[]> getSourceEntries(File dir) {

        Vector<String[]> entries = new Vector<String[]>();
        Vector<String> keys = new Vector<String>();

        for (String file : getSourceFiles(dir)) {

            String[][] matches = new Regex(Pattern.compile("JDLocale[\\s]*?\\.L[F]?[\\s]*?\\([\\s]*?\"(.*?)\"[\\s]*?,[\\s]*?\"(.*?)\"[\\s]*?[,\\)]").matcher(file)).getMatches();

            for (String[] match : matches) {

                match[0] = match[0].trim();
                match[1] = match[1].trim() + ((match[1].endsWith(" ")) ? " " : "");
                if (!keys.contains(match[0])) {

                    keys.add(Encoding.UTF8Decode(match[0]));
                    entries.add(new String[] { Encoding.UTF8Decode(match[0]), Encoding.UTF8Decode(match[1]) });

                }
            }
        }

        return entries;

    }

    private Vector<String> getSourceFiles(File directory) {

        Vector<String> fileContents = new Vector<String>();

        for (File entry : directory.listFiles()) {

            if (entry.isDirectory()) {

                fileContents.addAll(getSourceFiles(entry));

            } else if (entry.isFile()) {

                if (JDUtilities.getFileExtension(entry).equals("java")) {
                    fileContents.add(JDUtilities.getLocalFile(entry));
                }

            }

        }

        return fileContents;

    }

    private class MyTableCellRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String[] r = data.get(row);

            if (table.isRowSelected(row)) {
                c.setBackground(Color.LIGHT_GRAY);
            } else if (subConfig.getBooleanProperty(PROPERTY_COLORIZE_MISSING, false) && r[2].equals("")) {
                c.setBackground((Color) subConfig.getProperty(PROPERTY_MISSING_COLOR, Color.RED));
            } else if (subConfig.getBooleanProperty(PROPERTY_COLORIZE_OLD, false) && oldEntries.contains(r[0])) {
                c.setBackground((Color) subConfig.getProperty(PROPERTY_OLD_COLOR, Color.ORANGE));
            } else {
                c.setBackground(Color.WHITE);
            }

            return c;
        }

    }

    private class MyTableModel extends AbstractTableModel {

        private static final long serialVersionUID = -5434313385327397539L;
        private String[] columnNames = { JDLocale.L("plugins.optional.langfileeditor.key", "Key"), JDLocale.L("plugins.optional.langfileeditor.sourceValue", "Source Value"), JDLocale.L("plugins.optional.langfileeditor.languageFileValue", "Language File Value") };

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.size();
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String getValueAt(int row, int col) {
            return data.get(row)[col];
        }

        public Class<?> getColumnClass(int c) {
            return String.class;
        }

        public boolean isCellEditable(int row, int col) {
            return (col == 2);
        }

        public void setValueAt(Object value, int row, int col) {
            data.get(row)[col] = (String) value;
            this.fireTableRowsUpdated(row, row);
            setInfoLabels();
        }

    }

    private class StringArrayComparator implements Comparator<String[]> {

        public int compare(String[] s1, String[] s2) {

            return s1[0].compareToIgnoreCase(s2[0]);

        }

    }

    private class LngFileFilter extends FileFilter {

        public boolean accept(File f) {

            return (f.isDirectory() || f.getName().toLowerCase().endsWith(".lng"));

        }

        public String getDescription() {

            return "LanguageFiles (*.lng)";

        }

    }

    private class DupeDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        public DupeDialog(JFrame owner, Vector<String[]> dupes) {

            super();

            JPanel panel = new JPanel();
            setContentPane(panel);

            setModal(true);
            setLayout(new GridBagLayout());
            setTitle(JDLocale.L("plugins.optional.langfileeditor.duplicatedEntries", "Duplicated Entries"));

            MyDupeTableModel tableModel = new MyDupeTableModel();
            JTable table = new JTable(tableModel);
            tableModel.setData(dupes);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(900, 350));

            JButton btnClose = new JButton(JDLocale.L("plugins.optional.langfileeditor.close", "Close"));
            btnClose.addActionListener(this);
            getRootPane().setDefaultButton(btnClose);

            JDUtilities.addToGridBag(this, scroll, 1, 1, 1, 1, 1, 1, new Insets(5, 5, 5, 5), GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            JDUtilities.addToGridBag(this, btnClose, 1, 2, 1, 1, 1, 1, new Insets(5, 5, 5, 5), GridBagConstraints.NONE, GridBagConstraints.CENTER);

            int n = 10;
            panel.setBorder(new EmptyBorder(n, n, n, n));

            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            dispose();

        }

        class MyDupeTableModel extends AbstractTableModel {

            private static final long serialVersionUID = -5434313385327397539L;
            private String[] columnNames = { JDLocale.L("plugins.optional.langfileeditor.string", "String"), JDLocale.L("plugins.optional.langfileeditor.firstKey", "First Key"), JDLocale.L("plugins.optional.langfileeditor.secondKey", "Second Key") };
            private Vector<String[]> tableData = new Vector<String[]>();

            public int getColumnCount() {
                return columnNames.length;
            }

            public int getRowCount() {
                return tableData.size();
            }

            public String getColumnName(int col) {
                return columnNames[col];
            }

            public String getValueAt(int row, int col) {
                return tableData.get(row)[col];
            }

            public Class<?> getColumnClass(int c) {
                return String.class;
            }

            public boolean isCellEditable(int row, int col) {
                return false;
            }

            public void setData(Vector<String[]> newData) {
                tableData = newData;
                this.fireTableRowsInserted(0, tableData.size() - 1);
            }

        }

    }

    private class EditDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        private JButton btnOK = new JButton(JDLocale.L("plugins.optional.langfileeditor.ok", "OK"));
        private JButton btnCancel = new JButton(JDLocale.L("plugins.optional.langfileeditor.cancel", "Cancel"));
        private JButton btnAdopt = new JButton(JDLocale.L("plugins.optional.langfileeditor.adoptDefaultValue", "Adopt Default Value"));
        private JFrame owner;
        private JTextArea taSourceValue = new JTextArea(5, 20);
        private JTextArea taFileValue = new JTextArea(5, 20);

        public String value;

        public EditDialog(JFrame owner, String[] entry) {

            super(owner);
            this.owner = owner;

            setModal(true);
            setLayout(new BorderLayout(5, 5));
            setTitle(JDLocale.L("plugins.optional.langfileeditor.editValue", "Edit Value"));
            getRootPane().setDefaultButton(btnOK);

            JLabel lblKey = new JLabel("");

            btnOK.addActionListener(this);
            btnCancel.addActionListener(this);
            btnAdopt.addActionListener(this);

            lblKey.setText("Key: " + entry[0]);
            taSourceValue.setText(entry[1]);
            taFileValue.setText(entry[2]);
            taSourceValue.setEditable(false);

            JPanel main = new JPanel(new BorderLayout(5, 5));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));
            JPanel fields = new JPanel(new BorderLayout(5, 5));
            JPanel buttons1 = new JPanel(new BorderLayout(5, 5));
            JPanel buttons2 = new JPanel(new FlowLayout());

            main.add(lblKey, BorderLayout.PAGE_START);
            main.add(fields, BorderLayout.CENTER);
            main.add(buttons1, BorderLayout.PAGE_END);

            fields.add(new JScrollPane(taSourceValue), BorderLayout.PAGE_START);
            fields.add(new JScrollPane(taFileValue), BorderLayout.PAGE_END);

            buttons1.add(buttons2, BorderLayout.LINE_END);
            buttons2.add(btnAdopt);
            buttons2.add(btnOK);
            buttons2.add(btnCancel);

            setContentPane(main);
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnOK) {

                value = taFileValue.getText();
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnCancel) {

                value = null;
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnAdopt) {

                taFileValue.setText(taSourceValue.getText());

            }

        }

    }

    private class AddDialog extends JDialog implements ActionListener {

        private static final long serialVersionUID = 1L;

        private JButton btnOK = new JButton(JDLocale.L("plugins.optional.langfileeditor.ok", "OK"));
        private JButton btnCancel = new JButton(JDLocale.L("plugins.optional.langfileeditor.cancel", "Cancel"));
        private JFrame owner;
        private JTextField tfKey = new JTextField(20);
        private JTextArea taValue = new JTextArea(5, 20);

        public String value;
        public String key;

        public AddDialog(JFrame owner) {

            super(owner);
            this.owner = owner;

            setModal(true);
            setLayout(new BorderLayout(5, 5));
            setTitle("Edit Value");
            getRootPane().setDefaultButton(btnOK);

            btnOK.addActionListener(this);
            btnCancel.addActionListener(this);

            JLabel lblKey = new JLabel("Key:");

            JPanel main = new JPanel(new BorderLayout(5, 5));
            main.setBorder(new EmptyBorder(10, 10, 10, 10));
            JPanel keyPanel = new JPanel(new BorderLayout(5, 5));
            JPanel buttons1 = new JPanel(new BorderLayout(5, 5));
            JPanel buttons2 = new JPanel(new FlowLayout());

            main.add(keyPanel, BorderLayout.PAGE_START);
            main.add(new JScrollPane(taValue), BorderLayout.CENTER);
            main.add(buttons1, BorderLayout.PAGE_END);

            keyPanel.add(lblKey, BorderLayout.LINE_START);
            keyPanel.add(tfKey, BorderLayout.CENTER);

            buttons1.add(buttons2, BorderLayout.LINE_END);
            buttons2.add(btnOK);
            buttons2.add(btnCancel);

            setContentPane(main);
            pack();
            setLocation(JDUtilities.getCenterOfComponent(owner, this));
            setVisible(true);

        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == btnOK) {

                value = taValue.getText().trim();
                key = tfKey.getText().trim();
                dispose();
                owner.setVisible(true);

            } else if (e.getSource() == btnCancel) {

                value = null;
                dispose();
                owner.setVisible(true);

            }

        }

    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public boolean initAddon() {
        return true;
    }

    @Override
    public void onExit() {
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        menu.add(new MenuItem(MenuItem.NORMAL, JDLocale.L("plugins.optional.langfileeditor.show_gui", "Show GUI"), 0).setActionListener(this));

        return menu;
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.langfileeditor.name", "JDLangFileEditor");
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    public static int getAddonInterfaceVersion() {
        return 1;
    }

}
