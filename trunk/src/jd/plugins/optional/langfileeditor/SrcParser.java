//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.optional.langfileeditor;

import java.io.File;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;
import jd.event.JDBroadcaster;
import jd.event.MessageEvent;
import jd.event.MessageListener;
import jd.nutils.Formatter;
import jd.nutils.JDHash;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class SrcParser {

    public static final int PARSE_NEW_FILE = 0;
    public static final int PARSE_NEW_ENTRY = 1;
    private File root;
    private JDBroadcaster<MessageListener, MessageEvent> broadcaster;

    public SrcParser(File resourceFile) {
        this.root = resourceFile;
        this.entries = new ArrayList<LngEntry>();

        this.broadcaster = new JDBroadcaster<MessageListener, MessageEvent>() {

            @Override
            protected void fireEvent(MessageListener listener, MessageEvent event) {
                listener.onMessage(event);
            }

        };

        pattern.add("jd\\.gui\\.swing\\.ShortCuts\\.(.+?)");
        pattern.add("sys\\.warning\\.dlcerror\\.(.+?)");
        pattern.add("!(.+?)");
    }

    public JDBroadcaster<MessageListener, MessageEvent> getBroadcaster() {
        return broadcaster;
    }

    public ArrayList<LngEntry> getEntries() {
        return entries;
    }

    private ArrayList<LngEntry> entries;
    private File currentFile;
    private String currentContent;
    private ArrayList<String> pattern = new ArrayList<String>();

    public ArrayList<String> getPattern() {
        return pattern;
    }

    public void setPattern(ArrayList<String> pattern) {
        this.pattern = pattern;
    }

    public void parse() {

        for (File f : getSourceFiles(root)) {
            parseFile(f);
        }

    }

    public static void main(String args[]) {

        SrcParser p = new SrcParser(new File("C:\\Users\\Coalado\\.jd_home\\tmp\\lfe\\src\\jd"));
        p.parse();
    }

    @SuppressWarnings("unchecked")
    /**
     * parses an java file and writes all JDL Matches to entries and pattern. this method uses a cache to be faster
     */
    private void parseFile(File file) {

        this.currentFile = file;
        broadcaster.fireEvent(new MessageEvent(this, PARSE_NEW_FILE, "Parse " + file.getAbsolutePath()));

        // find all lines containing JDL calls
        currentContent = JDIO.getLocalFile(file);
        File cacheEntries = JDUtilities.getResourceFile("tmp/lfe/cache/" + JDHash.getMD5(currentContent) + ".entries");
        File cachePattern = JDUtilities.getResourceFile("tmp/lfe/cache/" + JDHash.getMD5(currentContent) + ".pattern");
        ArrayList<LngEntry> fileEntries = new ArrayList<LngEntry>();
        ArrayList<String> filePattern = new ArrayList<String>();
        if (cacheEntries.exists() && cachePattern.exists()) {

            try {
                fileEntries = (ArrayList<LngEntry>) JDIO.loadObject(null, cacheEntries, false);

                filePattern = (ArrayList<String>) JDIO.loadObject(null, cachePattern, false);

                for (LngEntry entry : fileEntries) {
                    if (!entries.contains(entry)) {
                        System.out.println(" CACHE: " + entry);
                        entries.add(entry);
                    }
                }

                for (String patt : filePattern) {
                    if (!pattern.contains(patt)) {
                        System.out.println(" CACHE: " + patt);
                        pattern.add(patt);
                    }
                }
                return;
            } catch (Exception e) {

                cacheEntries.delete();
                cachePattern.delete();
                fileEntries = new ArrayList<LngEntry>();
                filePattern = new ArrayList<String>();
            }
        }

        prepareContent();
        currentContent = Pattern.compile("\\/\\*(.*?)\\*\\/", Pattern.DOTALL).matcher(currentContent).replaceAll("[[/*.....*/]]");
        currentContent = Pattern.compile("[^:]//(.*?)[\n|\r]", Pattern.DOTALL).matcher(currentContent).replaceAll("[[\\.....]]");

        currentContent = Pattern.compile("JDL\\s*?\\.\\s*?L", Pattern.DOTALL).matcher(currentContent).replaceAll("JDL.L");
        String[] matches = new Regex(currentContent, "([^;^{^}]*JDL\\.LF?\\s*?\\(.*?\\)[^;^{^}]*)").getColumn(0);

        for (String match : matches) {
            // splitting all calls.
            parseCodeLine(match, fileEntries, filePattern);

        }

        try {
            JDIO.saveObject(null, fileEntries, cacheEntries, null, null, false);
            JDIO.saveObject(null, filePattern, cachePattern, null, null, false);

        } catch (Exception e) {
            e.printStackTrace();
            cacheEntries.delete();
            cachePattern.delete();
        }
    }

    private void prepareContent() {

        String cl = this.currentFile.getAbsolutePath();
        cl = cl.replace("\\", "/");
        cl = cl.substring(cl.indexOf("/jd/") + 1).replace('/', '.');
        cl = cl.replace(".java", "");
        currentContent = currentContent.replace("this.getClass().getName()", "\"" + cl + "\"");
        currentContent = currentContent.replace("getClass().getName()", "\"" + cl + "\"");
        String simple = cl.substring(cl.lastIndexOf(".") + 1);
        currentContent = currentContent.replace("this.getClass().getSimpleName()", "\"" + simple + "\"");
        currentContent = currentContent.replace("getClass().getSimpleName()", "\"" + simple + "\"");
        currentContent = currentContent.replace("\"+\"", "");
        if (this.currentFile.getAbsolutePath().toLowerCase().contains("actioncontroll")) {
            System.out.println("I");
        }
        if (this.currentContent.contains("jd.gui.swing.jdgui.menu.actions;")) {
            String menukey = new Regex(currentContent, "super\\(\"(.*?)\",\\s*\".*?\"\\);").getMatch(0);
            if (menukey != null) {
                currentContent = currentContent.replaceFirst("super\\(\"(.*?)\",\\s*\".*?\"\\);", "[[...]]");
                currentContent += "\r\nJDL.L(\"gui.menu." + menukey + ".name\",\"" + menukey + "\");";
                currentContent += "\r\nJDL.L(\"gui.menu." + menukey + ".mnem\",\"-\");";
                currentContent += "\r\nJDL.L(\"gui.menu." + menukey + ".accel\",\"-\");";
            }
        }
        if (this.currentContent.contains("jd.gui.swing.jdgui.menu;")) {
            String menukey = new Regex(currentContent, "super\\(\"(.*?)\",\\s*\".*?\"\\);").getMatch(0);
            currentContent = currentContent.replaceFirst("super\\(\"(.*?)\",\\s*\".*?\"\\);", "[[...]]");
            currentContent += "\r\nJDL.L(\"" + menukey + "\",\"" + menukey + "\");";

        }
        if (this.currentContent.contains(" ThreadedAction")) {
            String[] keys = new Regex(currentContent, " ThreadedAction\\s*\\(\"(.*?)\"\\,\\s*\"(.*?)\"").getColumn(0);

            for (String k : keys) {
                currentContent += "\r\nJDL.L(\"" + "gui.menu." + k + ".name" + "\",\"" + "gui.menu." + k + ".name" + "\");";
                currentContent += "\r\nJDL.L(\"" + "gui.menu." + k + ".mnem" + "\",\"" + "gui.menu." + k + ".mnem" + "\");";
                currentContent += "\r\nJDL.L(\"" + "gui.menu." + k + ".accel" + "\",\"" + "-" + "\");";
            }

        }
        if (this.currentContent.contains(" ToolBarAction")) {
            // new ToolBarAction("action.opendlfolder",
            // "gui.images.package_opened") {
            String[] keys = new Regex(currentContent, " ToolBarAction\\s*\\(\"(.*?)\"\\,\\s*\"(.*?)\"").getColumn(0);

            for (String k : keys) {
                currentContent += "\r\nJDL.L(\"" + "gui.menu." + k + ".name" + "\",\"" + "gui.menu." + k + ".name" + "\");";
                currentContent += "\r\nJDL.L(\"" + "gui.menu." + k + ".mnem" + "\",\"" + "gui.menu." + k + ".mnem" + "\");";
                currentContent += "\r\nJDL.L(\"" + "gui.menu." + k + ".accel" + "\",\"" + "-" + "\");";
            }
        }
        if (this.currentContent.contains("extends PluginOptional")) {
            currentContent += "\r\nJDL.L(\"" + cl + "\",\"" + simple + "\");";

        }
        // JDL.L(this.getClass().getName()

    }

    /**
     * Returns all javafiles in dir. works recursive
     * 
     * @param dir
     * @return
     */
    private ArrayList<File> getSourceFiles(File dir) {
        ArrayList<File> files = new ArrayList<File>();

        if (dir != null) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    files.addAll(getSourceFiles(file));
                } else if (file.getName().toLowerCase().endsWith(".java")) {
                    files.add(file);
                }
            }
        }

        return files;
    }

    /**
     * Finds lngentries and patterns in this codeline. adds all to global list
     * and to locallists fileEntries and filePattern
     * 
     * @param match
     * @param fileEntries
     * @param filePattern
     */
    private void parseCodeLine(String match, ArrayList<LngEntry> fileEntries, ArrayList<String> filePattern) {
        String[] calls = match.split("JDL\\.");
        String pat_string = "\"(.*?)(?<!\\\\)\"";
        // if (match.contains("plugins.host.rapidshare.errors")) {
        // match = match;
        // }
        LngEntry entry;
        String m;
        main: for (String orgm : calls) {

            m = orgm;
            m = m.trim();
            if (m.startsWith("L")) {

                String[] strings = new Regex(m, pat_string).getColumn(0);
                m = m.replace("\r", "");
                m = m.replace("\n", "");

                m = m.replaceAll(pat_string, "%%%S%%%");

                m = m.replace(" ", "");
                orgm = m;
                // m = new Regex(m, "\\((.*?\\,.*?)[\\)\\,]").getMatch(0);
                try {

                    int com1 = m.indexOf(",");
                    int end = m.indexOf(")", com1 + 1);
                    int com2;
                    com2 = m.indexOf(",", com1 + 1);
                    if (com2 > 0 && com2 < end) end = com2;
                    if (end < 0) {
                        m = m.substring(m.indexOf("(") + 1).trim();
                    } else {
                        m = m.substring(m.indexOf("(") + 1, end).trim();
                    }

                } catch (Exception e) {
                    e.printStackTrace();

                }
                while (m.charAt(m.length() - 1) == ',')
                    m = m.substring(0, m.length() - 1);
                if (m == null || m.length() == 0) {
                    // JDLogger.getLogger().severe("unknown: " + orgm);
                    continue;
                }
                m = m.replace("%%%+%%%", "%%%%%%");
                String[] parameter = m.split(",");

                if (orgm.startsWith("LF ") || orgm.startsWith("LF(")) {

                    if (orgm.substring(2).trim().charAt(0) != '(') {

                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + m);
                        continue;
                    }
                    if (parameter.length != 2) {

                        JDLogger.getLogger().severe("Malformated translation pair (inner functions?) in " + currentFile + " : " + match);
                        continue;
                    }
                    int i = 0;
                    if (parameter[1].contains("+")) {
                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + match);
                        continue;
                    }

                    /*
                     * merge expressions
                     */
                    while (parameter[0].contains("+")) {
                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+(\\w+)\\+?)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {

                                    JDLogger.getLogger().severe("Malformated translation key in " + currentFile + " : " + match);
                                    break main;
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+?(\\w+)\\+)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("Malformated translation key in 2" + currentFile + " : " + match);
                                    break main;
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                    }
                    for (int x = 0; x < parameter.length; x++) {

                        while (parameter[x].contains("%%%S%%%")) {

                            parameter[x] = parameter[x].replaceFirst("%%%S%%%", Matcher.quoteReplacement(strings[i++]));
                        }

                    }
                    String error;
                    if ((error = new Regex(parameter[0], "([\\(\\)\\{\\}\\/\\\\\\$\\&\\+\\~\\#\\\"\\!\\?]+)").getMatch(0)) != null) {

                        int index = parameter[0].indexOf(error);
                        if (index >= 0) {
                            JDLogger.getLogger().warning("Unsupported chars (" + parameter[0].substring(0, index) + "<< |" + parameter[0].substring(index + 1) + ") in key:" + currentFile + " : " + parameter[0]);

                        } else {
                            JDLogger.getLogger().warning("Unsupported chars in key: " + currentFile + " : " + parameter[0]);

                        }
                        continue;
                    }
                    if (!parameter[0].contains(".")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                    }
                    if (parameter[0].contains("null")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                    }
                    entry = new LngEntry(parameter[0], parameter[1]);
                    if (!entries.contains(entry)) {
                        entries.add(entry);

                        System.out.println("LF  " + Formatter.fillInteger(entries.size(), 3, "0") + " " + entry);
                        broadcaster.fireEvent(new MessageEvent(this, PARSE_NEW_ENTRY, "LF  " + Formatter.fillInteger(entries.size(), 3, "0") + " " + entry));

                    }
                    if (!fileEntries.contains(entry)) {
                        fileEntries.add(entry);
                    }
                } else if (orgm.startsWith("L ") || orgm.startsWith("L(")) {

                    if (orgm.substring(1).trim().charAt(0) != '(') {

                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + m);
                        continue;
                    }
                    if (parameter.length != 2) {

                        JDLogger.getLogger().severe("Malformated translation pair (inner functions?) in " + currentFile + " : " + match);
                        continue;
                    }
                    int i = 0;
                    if (parameter[1].contains("+")) {
                        JDLogger.getLogger().severe("Malformated translation value in " + currentFile + " : " + match);
                        continue;
                    }

                    /*
                     * merge expressions
                     */

                    while (parameter[0].contains("+")) {
                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+([^%]+)\\+?)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    System.out.println(mm[0] + " - " + mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {
                                    parameter[0] = parameter[0].replace(mm[0], "*");
                                    JDLogger.getLogger().severe("Pattern match in " + currentFile + " : " + match);

                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                        try {
                            String[][] matches = new Regex(parameter[0], "(\\+?([^%]+)\\+)").getMatches();
                            for (String[] mm : matches) {
                                try {
                                    String value = getValueOf(mm[1]);
                                    parameter[0] = parameter[0].replace(mm[0], value);

                                } catch (Exception e) {
                                    JDLogger.getLogger().severe("Malformated translation key in 1" + currentFile + " : " + match);
                                    break main;
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            break;
                        }

                    }

                    for (int x = 0; x < parameter.length; x++) {

                        while (parameter[x].contains("%%%S%%%")) {

                            parameter[x] = parameter[x].replaceFirst("%%%S%%%", Matcher.quoteReplacement(strings[i++]));
                        }

                    }

                    String error;
                    if ((error = new Regex(parameter[0], "([\\(\\)\\{\\}\\/\\\\\\$\\&\\+\\~\\#\\\"\\!\\?]+)").getMatch(0)) != null) {

                        int index = parameter[0].indexOf(error);
                        if (index >= 0) {
                            JDLogger.getLogger().warning("Unsupported chars (" + parameter[0].substring(0, index) + "<< |" + parameter[0].substring(index + 1) + ") in key:" + currentFile + " : " + parameter[0]);

                        } else {
                            JDLogger.getLogger().warning("Unsupported chars in key: " + currentFile + " : " + parameter[0]);

                        }
                        continue;
                    }
                    if (!parameter[0].contains(".")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                        break;
                    }
                    if (parameter[0].contains("null")) {
                        JDLogger.getLogger().warning("Prob. Malformated translation key in " + currentFile + " : " + match);
                        break;
                    }

                    if (parameter[0].contains("*")) {
                        String patt = parameter[0].replace(".", "\\.").replace("*", "(.+?)");

                        if (!pattern.contains(patt)) pattern.add(patt);
                        if (!filePattern.contains(patt)) filePattern.add(patt);

                    } else {
                        entry = new LngEntry(parameter[0], parameter[1]);

                        if (!entries.contains(entry)) {
                            entries.add(entry);

                            System.out.println("L   " + Formatter.fillInteger(entries.size(), 3, "0") + " " + entry);
                            broadcaster.fireEvent(new MessageEvent(this, PARSE_NEW_ENTRY, "L   " + Formatter.fillInteger(entries.size(), 3, "0") + " " + entry));

                        }

                        if (!fileEntries.contains(entry)) {
                            fileEntries.add(entry);
                        }
                    }
                }

            }
        }
    }

    private String getValueOf(String variable) {
        String[] matches = new Regex(currentContent, variable + "\\s*=(.*?);").getColumn(0);
        String ret = matches[matches.length - 1].trim();
        while (ret.startsWith("\""))
            ret = ret.substring(1);
        while (ret.endsWith("\""))
            ret = ret.substring(0, ret.length() - 1);

        return ret;

    }

    public void parseDefault(File file) {
        String[] entries = Regex.getLines(JDIO.getLocalFile(file));

        for (String line : entries) {
            if (line.startsWith("#pattern: ")) {
                pattern.add(line.substring(10));
                continue;

            }
            if (line.startsWith("#")) continue;
            String key = line.substring(0, line.indexOf("=")).trim();
            String value = line.substring(line.indexOf("=") + 1).trim();
            this.entries.add(new LngEntry(key, value));
        }

    }
}
