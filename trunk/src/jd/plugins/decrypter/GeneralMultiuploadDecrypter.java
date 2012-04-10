//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nextdown.net", "bitsor.com", "flameupload.com", "mirrorafile.com", "lougyl.com", "neo-share.com", "qooy.com", "share2many.com", "uploader.ro", "uploadmirrors.com", "indirdur.net", "megaupper.com", "shrta.com", "1filesharing.com", "7ups.net", "mirrorfusion.com", "digzip.com", "needmirror.com" }, urls = { "http://(www\\.)?nextdown\\.net/files/[0-9A-Z]{8}", "http://(www\\.)?bitsor\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?flameupload\\.(com|co)/(download|files)/[0-9A-Z]{8}", "http://[\\w\\.]*?mirrorafile\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?lougyl\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?neo\\-share\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?qooy\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?share2many\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?uploader\\.ro/files/[0-9A-Z]{8}",
        "http://[\\w\\.]*?uploadmirrors\\.(com|org)/download/[0-9A-Z]{8}", "http://[\\w\\.]*?indirdur\\.net/files/[0-9A-Z]{8}", "http://[\\w\\.]*?megaupper\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?shrta\\.com/files/[0-9A-Z]{8}", "http://[\\w\\.]*?1filesharing\\.com/(mirror|download)/[0-9A-Z]{8}", "http://[\\w\\.]*?7ups\\.net/files/[0-9A-Z]{8}", "http://[\\w\\.]*?mirrorfusion\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?digzip\\.com/files/[0-9A-Z]{8}", "http://(www\\.)?needmirror\\.com/files/[0-9A-Z]{8}" }, flags = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 })
public class GeneralMultiuploadDecrypter extends PluginForDecrypt {

    public GeneralMultiuploadDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    // This decrypter should handle nearly all sites using the qooy.com script!
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String parameter = param.toString();
        // Only uploadmirrors.com has those "/download/" links so we need to
        // correct them
        parameter = parameter.replaceAll("(/dl/|/mirror/|/download/)", "/files/").replace("flameupload.co/", "flameupload.com/");
        // Tohse links need a "/" at the end to be valid
        if (!param.getCryptedUrl().endsWith("/")) param.setCryptedUrl(param.getCryptedUrl().toString() + "/");
        String host = new Regex(parameter, "(.+)/(files|dl)").getMatch(0);
        String id = new Regex(parameter, "files/([0-9A-Z]+)").getMatch(0);
        // This should never happen but in case a dev changes the plugin without
        // much testing he ll see the error later!
        if (host == null || id == null) {
            logger.warning("A critical error happened! Please inform the support. : " + param.toString());
            return null;
        }
        parameter = host + "/status.php?uid=" + id;
        br.getPage(parameter);
        /* Error handling */
        if (!br.containsHTML("<img src=") && !br.containsHTML("<td class=\"host\">")) {
            logger.info("The following link should be offline: " + param.toString());
            throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        }
        br.setFollowRedirects(false);
        String[] redirectLinks = br.getRegex("(/(redirect|rd)/[0-9A-Z]+/[a-z0-9]+)").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("><a href=(.*?)target=").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) return null;
        progress.setRange(redirectLinks.length);
        String nicelookinghost = host.replaceAll("(www\\.|http://|/)", "");
        logger.info("Found " + redirectLinks.length + " " + nicelookinghost + " links to decrypt...");
        for (String singlelink : redirectLinks) {
            Browser brc = br.cloneBrowser();
            String dllink = null;
            // Handling for links that need to be regexed or that need to be get
            // by redirect
            if (singlelink.contains("/redirect/") || singlelink.contains("/rd/")) {
                brc.getPage(host.replace("www.", "") + singlelink);
                if (parameter.contains("flameupload.com")) {
                    dllink = brc.getRegex(">Download Link:<br><a href=\"([^\"]+)").getMatch(0);
                } else {
                    dllink = brc.getRedirectLocation();
                    if (dllink == null) {
                        dllink = brc.getRegex("<frame name=\"main\" src=\"(.*?)\">").getMatch(0);
                        // For 1filesharing.com links
                        if (dllink == null) {
                            dllink = brc.getRegex("<iframe id=\"download\" src=\"(.*?)\"").getMatch(0);
                            // For needmirror.com links
                            if (dllink == null) {
                                dllink = brc.getRegex(">Please <a href=\"([^\"\\']+)\"").getMatch(0);
                            }
                        }
                    }
                }
            } else {
                // Handling for already regexed final-links
                dllink = singlelink;
            }
            progress.increase(1);
            if (dllink == null || dllink.equals("")) {
                // Continue away, randomised pages can cause failures.
                logger.warning("Possible plugin error: " + param.toString());
                logger.warning("Continuing...");
                continue;
            }
            if (dllink.contains("flameupload")) {
                logger.info("Recursion? " + param.toString() + "->" + dllink);
            }
            decryptedLinks.add(createDownloadlink(dllink));
        }
        logger.warning("Task Complete! : " + param.toString());
        return decryptedLinks;
    }
}
