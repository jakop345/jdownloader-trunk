//    jDownloader - Downloadmanager
//    Copyright (C) 2011  JD-Team support@jdownloader.org
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
//
package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "israbox.com" }, urls = { "http(s|)://[\\w\\.]*israbox\\.(com|net|org|info|me|download)/[0-9]+-.*?\\.html" }, flags = { 0 })
public class SrBoxCom extends PluginForDecrypt {

    public SrBoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        // Logger logDebug = JDLogger.getLogger();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (br.containsHTML("(An error has occurred|The article cannot be found)") || "https://www.israbox.download/".equals(br.getRedirectLocation()) || "http://www.israbox.me/".equals(br.getRedirectLocation()) || "http://www.israbox.info/".equals(br.getRedirectLocation()) || "http://www.israbox.org/".equals(br.getRedirectLocation()) || "http://www.israbox.net/".equals(br.getRedirectLocation()) || "http://www.israbox.com/".equals(br.getRedirectLocation())) {
            try {
                decryptedLinks.add(createOfflinelink(parameter));
            } catch (final Throwable t) {
                logger.info("Link offline: " + parameter);
            }
            return decryptedLinks;
        }
        String fpName = br.getRegex("<h1><a href[^>]+>(.*?)</a></h1>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<b>Download Fast:(.*?)</b>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("color=\"#ffffff\" size=\"1\">(.*?)free from rapidshare").getMatch(0);
                }
            }
        }
        // On this site, when an accent is set, the result is as extended ASCII
        // character which is bad in the name of the package
        fpName = Encoding.htmlDecode(fpName);
        fpName = RemoveCharacter(fpName);
        fpName = CapitalLetterForEachWords(fpName);

        // Array of image to download the cover (It can be usable if the user
        // want to create a subfolder with the name of the package because
        // the folder is immediately created because it will download the cover
        // in it
        String[] TabImage2 = br.getRegex("<img src=\"(http://[\\w\\.]*?lectro\\.ws)/*?/uploads/posts/(.*?)\"").getColumn(1);
        String[] TabImage1 = null;
        String[] TabNet = null;
        if (TabImage2.length == 0) {
            TabImage1 = br.getRegex("<img src=\"(http(s|)://[\\w\\.]*?israbox\\.(com|net|org|info|me|download))*?/uploads(.*?)\"").getColumn(2);
            TabNet = br.getRegex("<img src=\"(http(s|)://[\\w\\.]*?israbox\\.(com|net|org|info|me|download))*?/uploads(.*?)\"").getColumn(1);
        }

        int iPosition = parameter.indexOf(":");
        String strHttp = parameter.substring(0, iPosition);

        iPosition = strHttp.length() + 3;
        String strTemp = parameter.substring(iPosition, parameter.length() - iPosition);
        iPosition = strTemp.indexOf("/");
        strTemp = strTemp.substring(0, iPosition);
        iPosition = strTemp.lastIndexOf(".") + 1;
        String strNet = strTemp.substring(iPosition, strTemp.length());

        // Creation of the array of link that is supported by all plug-in
        String[] links = br.getRegex("<a href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            return null;
        }

        // Number of picture
        int iImage = TabImage1 == null ? 0 : TabImage1.length;
        if (TabImage1 != null) {
            for (String strImageLink : TabImage1) {
                if (strImageLink.contains("/thumbs/") || strImageLink.contains("/medium/")) {
                    iImage++;
                }
            }
        }
        iImage += TabImage2 == null ? 0 : TabImage2.length;
        if (TabImage2 != null) {
            for (String strImageLink : TabImage2) {
                if (strImageLink.contains("/thumbs/") || strImageLink.contains("/medium/")) {
                    iImage++;
                }
            }
        }

        // Some link can be crypted in this site, see if it is the case
        String[] linksCrypted = br.getRegex("\"(http(s|)://www\\.israbox\\.(com|net|org|info|me|download)/engine/go\\.php\\?url=.*?)\"").getColumn(0);

        progress.setRange(links.length + iImage + linksCrypted.length);
        // Added links
        for (String redirectlink : links) {
            decryptedLinks.add(createDownloadlink(redirectlink));
            progress.increase(1);
        }

        // Added crypted links
        for (String redirectlink : linksCrypted) {
            br.getPage(redirectlink);
            String finallink = br.getRedirectLocation();
            if (finallink != null) {
                decryptedLinks.add(createDownloadlink(finallink));
                progress.increase(1);
            }
        }

        // Added Image
        DownloadLink[] TabImageLink = new DownloadLink[iImage];
        int iImageFinal = 0;
        int iImageIndex = 0;
        if (TabImage1 != null) {
            int iImageNet = 0;
            for (String strImageLink : TabImage1) {
                if (!strImageLink.toLowerCase().contains("foto")) {
                    if (TabNet[iImageNet] != null) {
                        strNet = TabNet[iImageNet];
                    }
                    strImageLink = strHttp + "://www.israbox." + strNet + "/uploads/" + strImageLink;

                    if (strImageLink.contains("/thumbs/") || strImageLink.contains("/medium/")) {
                        DownloadLink DLLink = createDownloadlink(strImageLink, false);
                        if (DLLink != null) {
                            TabImageLink[iImageIndex++] = DLLink;
                            iImageFinal++;
                        }
                        strImageLink = strImageLink.replace("thumbs/", "");
                        strImageLink = strImageLink.replace("medium/", "");
                    }

                    DownloadLink DLLink = createDownloadlink(strImageLink, false);
                    if (DLLink != null) {
                        TabImageLink[iImageIndex++] = DLLink;
                        iImageFinal++;
                    }
                }
                iImageNet++;
            }
        }

        if (TabImage2 != null) {
            for (String strImageLink : TabImage2) {
                strImageLink = "http://www.lectro.ws/uploads/posts/" + strImageLink;

                if (strImageLink.contains("/thumbs/") || strImageLink.contains("/medium/")) {
                    DownloadLink DLLink = createDownloadlink(strImageLink, false);
                    if (DLLink != null) {
                        TabImageLink[iImageIndex++] = DLLink;
                        iImageFinal++;
                    }
                    strImageLink = strImageLink.replace("thumbs/", "");
                    strImageLink = strImageLink.replace("medium/", "");
                }

                DownloadLink DLLink = createDownloadlink(strImageLink, false);
                if (DLLink != null) {
                    TabImageLink[iImageIndex++] = DLLink;
                    iImageFinal++;

                }
            }
        }

        if (TabImageLink != null) {
            iImageIndex = 1;
            for (DownloadLink DLLink : TabImageLink) {
                if (DLLink != null) {
                    String strExtension = "";
                    int iIndex = DLLink.getPluginPatternMatcher().lastIndexOf('.');
                    if (iIndex > -1) {
                        strExtension = DLLink.getPluginPatternMatcher().substring(iIndex);
                    }

                    if (strExtension != "") {
                        if (fpName != null) {
                            String strName = fpName;

                            // Delete the end of the filename only if it represents the year in parenthesis
                            strName = ReplacePattern(strName, "\\([0-9]{4}\\)$").trim();
                            if (iImageFinal > 1) {
                                strName += "_" + Integer.toString(iImageIndex);
                                iImageIndex++;
                            }
                            DLLink.setFinalFileName(strName + strExtension);
                        }
                    }
                    decryptedLinks.add(DLLink);
                }
            }
            progress.increase(1);
        }

        for (int i = decryptedLinks.size() - 1; i >= 0; i--) {
            if (decryptedLinks.get(i) == null) {
                decryptedLinks.remove(i);
            }
        }

        // Add all link in a package
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    @Override
    protected DownloadLink createDownloadlink(String link) {
        return createDownloadlink(link, true);
    }

    protected DownloadLink createDownloadlink(String link, Boolean bVerify) {
        if (!link.startsWith("http")) {
            return null;
        }

        if (link.contains("?media=")) {
            return null;
        }

        if (link.startsWith("http://www.israbox.com/uploads/img/") || link.startsWith("http://www.israbox.net/uploads/img/") || link.startsWith("http://www.israbox.org/uploads/img/") || link.startsWith("http://www.israbox.info/uploads/img/") || link.startsWith("http://www.israbox.me/uploads/img/") || link.startsWith("https://www.israbox.download/uploads/img/")) {
            return null;
        }

        if (link.toLowerCase().endsWith("_04c.png") || link.toLowerCase().endsWith("_kap-download.jpg") || link.toLowerCase().endsWith("_download-button.png") || link.toLowerCase().endsWith("_download.png")) {
            return null;
        }

        if (link.toLowerCase().contains("1418317446_53af83b0c0ff48717413b401bdc95723") || link.toLowerCase().contains("qm9ccs3xn4c9le2paels")) {
            return null;
        }

        if (link.toLowerCase().contains("discogs") || link.toLowerCase().contains("tracklist") || link.toLowerCase().contains("all-music") || link.toLowerCase().contains("junodownload")) {
            return null;
        }

        if (link.contains("signup?")) {
            return null;
        }

        if (link.startsWith("http://ax-d.pixfuture.net")) {
            return null;
        }

        if (link.startsWith("http://www.facebook") || link.startsWith("https://twitter.com") || link.startsWith("https://plus.google.com")) {
            return null;
        }

        if (bVerify && (link.startsWith("http://www.israbox.com") || link.startsWith("http://www.israbox.net") || link.startsWith("http://www.israbox.org") || link.startsWith("http://www.israbox.info") || link.startsWith("http://www.israbox.me") || link.startsWith("https://www.israbox.download"))) {
            return null;
        }

        return super.createDownloadlink(link);
    }

    /**
     * Allows to remove some character to have a nice name
     *
     * @param strName
     *            The name of the package
     * @return the name of the package normalized.
     */
    private String RemoveCharacter(String strName) {
        strName = strName.replace("", "-");
        strName = strName.replace("", "'");
        strName = strName.replace("–", "-");
        strName = strName.replace(":", ",");
        strName = strName.replace("/", "&");
        strName = strName.replace("\\", "&");
        strName = strName.replace("’", "'");

        strName = strName.replace("&nbsp;", " ");
        if (strName.startsWith("VA - ")) {
            strName = strName.replace("VA - ", "");
        }
        strName = strName.replace("Various Artists - ", "");
        strName = strName.replace("Vinyl", "");

        // Remove the exact words mp3, flac, lossless and ape
        strName = ReplacePattern(strName, "(?<!\\p{L})mp3(?!\\p{L})|(?<!\\p{L})ape(?!\\p{L})|(?<!\\p{L})flac(?!\\p{L})|(?<!\\p{L})lossless(?!\\p{L})|(?<!\\p{L})lossles(?!\\p{L})|(?<!\\p{L})alac(?!\\p{L})");

        // Remove the box set information
        strName = ReplacePattern(strName, "[0-9]+ *albums");
        strName = ReplacePattern(strName, "- [0-9]+ *CD Box Set");
        strName = ReplacePattern(strName, "Box Set");

        // Remove the HDTracks information (Take care of the case)
        strName = ReplacePattern(strName, "HDTracks");

        // Remove the vinyl digitizing information
        strName = ReplacePattern(strName, "Vinyl Digitizing");

        // Remove the number of CD
        strName = ReplacePattern(strName, "[0-9]+ *CD");

        // Replace Vol ou Vol. with 0
        strName = ReplacePattern(strName, "Vol(\\. |\\.| )", "0");

        // Replace the [ ] with nothing because the string in this bracket has been removed
        strName = ReplacePattern(strName, "\\[ *\\]");

        // Replace the ( ) with nothing because the string in this parenthesis has been removed
        strName = ReplacePattern(strName, "^\\[.*\\] ");

        // Replace the ( ) with nothing because the string in this parenthesis has been removed
        strName = ReplacePattern(strName, "\\( *\\)");

        // Replace several space characters in one
        strName = ReplacePattern(strName, "\\s{2,}", " ");

        // Remove the encoding information
        strName = ReplacePattern(strName, "[0-9]+ Kbps itunes");
        strName = ReplacePattern(strName, "[0-9]+ Kbps *&");
        strName = ReplacePattern(strName, "[0-9]+ Kbps\\.");
        strName = ReplacePattern(strName, "[0-9]+ Kbps");
        strName = ReplacePattern(strName, "[0-9]+Kbps");
        strName = ReplacePattern(strName, "320 *&");
        strName = ReplacePattern(strName, "& 320");
        strName = ReplacePattern(strName, "\\s{2,}");

        strName = strName.trim();
        if (strName.endsWith("320")) {
            strName = strName.substring(0, strName.length() - 3);
        }

        // Add a dash to collection
        int iPositionDash = -1;
        String strPattern = "Collection \\([0-9]{4}[-][0-9]{4}\\)";

        Pattern pattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(strName);
        if (matcher.find()) {
            iPositionDash = strName.indexOf("Collection");
        }

        // Add a dash to discography
        strPattern = "Discography \\([0-9]{4}[-][0-9]{4}\\)";
        pattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(strName);
        if (matcher.matches()) {
            iPositionDash = strName.indexOf("Discography");
        }

        if (iPositionDash != -1 && (iPositionDash > 2 && strName.substring(iPositionDash - 2, iPositionDash) != "- ")) {
            strName = strName.substring(0, iPositionDash).trim() + " - " + strName.substring(iPositionDash, strName.length()).trim();
        }

        // Replace brackets with parenthesis
        strName = strName.replace("[", "(");
        strName = strName.replace("]", ")");

        // Add a space between two parenthesis
        strName = strName.replace(")(", ") (");

        // remove trailing ,
        strName = strName.replaceFirst(",$", "");

        strName = strName.trim();
        if (strName.endsWith("&") || strName.endsWith("+")) {
            strName = strName.substring(0, strName.length() - 1);
        }
        if (strName.endsWith("(&)")) {
            strName = strName.substring(0, strName.length() - 3);
        }
        if (strName.endsWith("( & )")) {
            strName = strName.substring(0, strName.length() - 5);
        }

        return strName.trim();
    }

    /**
     * Allows to put a capital letter on each words of the title
     *
     * @param strText
     *            The text that we want to be capitalize
     * @return the text with a capital letter on each words.
     */
    private String CapitalLetterForEachWords(String strText) {
        if (strText == null || strText == "") {
            return "";
        }

        String strReturn = "";
        try {
            String strTemp = "";
            String strFirstChar = "";

            List<String> CapitalFirstCharacterException = new ArrayList<String>();
            CapitalFirstCharacterException.add("(");
            CapitalFirstCharacterException.add("[");

            List<String> CapitalCharacterInException = new ArrayList<String>();
            CapitalCharacterInException.add("-");
            CapitalCharacterInException.add(".");
            // CapitalCharacterInException.add("'");
            CapitalCharacterInException.add("\"");
            CapitalCharacterInException.add("/");
            CapitalCharacterInException.add("\\");
            CapitalCharacterInException.add("(");
            CapitalCharacterInException.add("[");

            String[] strAllWord = strText.split(" ");
            for (String strWord : strAllWord) {
                if (strWord.length() > 0) {
                    strFirstChar = strWord.substring(0, 1);
                    String strRemain = strWord.substring(1, strWord.length());
                    String strCharacter = strFirstChar;
                    if (CapitalFirstCharacterException.contains(strCharacter)) {
                        if (strWord.length() > 1) {
                            strCharacter += strWord.substring(1, 2).toUpperCase();
                            strRemain = strWord.substring(2, strWord.length());
                        } else {
                            strCharacter = strWord;
                        }
                    } else {
                        strCharacter = strFirstChar.toUpperCase();
                    }

                    String strCharacterIn = "";
                    strTemp = "";
                    for (String strInException : CapitalCharacterInException) {
                        strTemp = "";
                        if (strRemain.contains(strInException)) {
                            if (strInException == ".") {
                                strInException = "\\" + strInException;
                            }
                            String[] AllCharacter = strRemain.split(strInException);
                            for (int iIndex = 0; iIndex < AllCharacter.length; iIndex++) {
                                String strAllCarac = AllCharacter[iIndex];
                                if (iIndex > 0 && strAllCarac.length() > 0) {
                                    strFirstChar = strAllCarac.substring(0, 1);
                                    strRemain = strAllCarac.substring(1, strAllCarac.length());
                                    strCharacterIn = strFirstChar;
                                    if (CapitalFirstCharacterException.contains(strCharacterIn)) {
                                        if (strAllCarac.length() > 1) {
                                            strCharacterIn += strAllCarac.substring(1, 1).toUpperCase();
                                            strRemain = strAllCarac.substring(2, strAllCarac.length());
                                        } else {
                                            strCharacterIn = strAllCarac;
                                        }
                                    } else {
                                        strCharacterIn = strFirstChar.toUpperCase();
                                    }

                                    strTemp += strCharacterIn + strRemain;
                                    boolean bSpaceAtTheEnd = strTemp.endsWith(" ");
                                    strTemp = strTemp.trim();
                                    strTemp += strInException;
                                    if (bSpaceAtTheEnd) {
                                        strTemp += " ";
                                    }
                                } else {
                                    strTemp += strAllCarac + strInException;
                                }
                            }
                            if (strTemp.endsWith(strInException)) {
                                strTemp = strTemp.substring(0, strTemp.length() - 1);
                            }
                            strRemain = strTemp;
                        }
                    }
                    strReturn += strCharacter + strRemain + " ";
                } else {
                    strReturn += " ";
                }
            }
        } catch (Exception e) {
            strReturn = strText;
        }
        return strReturn.trim();
    }

    /**
     * Allows to replace text using a regular expression pattern
     *
     * @param strText
     *            Text to replace
     * @param strPattern
     *            Pattern to use for replacing
     * @return Text replaced
     */
    private String ReplacePattern(String strText, String strPattern) {
        return ReplacePattern(strText, strPattern, "");
    }

    /**
     * Allows to replace text using a regular expression pattern
     *
     * @param strText
     *            Text to replace
     * @param strPattern
     *            Pattern to use for replacing
     * @param strReplace
     *            Text use to replace
     * @return Text replaced
     */
    private String ReplacePattern(String strText, String strPattern, String strReplace) {
        Pattern pattern = Pattern.compile(strPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(strText);
        return matcher.replaceAll(strReplace);
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}