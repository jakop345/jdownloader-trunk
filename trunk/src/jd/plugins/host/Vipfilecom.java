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

package jd.plugins.host;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class Vipfilecom extends PluginForHost {
    private static final String CODER = "JD-Team";

    public Vipfilecom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://vip-file.com/tmpl/terms.php";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        String downloadurl = downloadLink.getDownloadURL();
        this.setBrowserExclusive();

        br.getPage(downloadurl);
        if (!br.containsHTML("This file not found")) {
            String linkinfo[][] = new Regex(br, Pattern.compile("Size:.*?<b style=\"padding-left:5px;\">([0-9\\.]*) (.*?)</b>", Pattern.CASE_INSENSITIVE)).getMatches();

            if (linkinfo[0][1].matches("Gb")) {
                downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024 * 1024 * 1024));
            } else if (linkinfo[0][1].matches("Mb")) {
                downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024 * 1024));
            } else if (linkinfo[0][1].matches("Kb")) {
                downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024));
            }
            downloadLink.setName(new Regex(downloadurl, "http://[\\w\\.]*?vip-file\\.com/download/[a-zA-z0-9]+/(.*?)\\.html").getMatch(0));
            return true;
        } else {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        /* DownloadLink holen, 2x der Location folgen */
        String link = Encoding.htmlDecode(new Regex(br, Pattern.compile("<a href=\"(http://vip-file\\.com/download.*?)\">", Pattern.CASE_INSENSITIVE)).getMatch(0));
        if (link == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
        br.setFollowRedirects(false);
        br.getPage(link);
        if (br.getRedirectLocation() == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        br.getPage(br.getRedirectLocation());

        if (br.getRedirectLocation() == null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            return;
        }
        br.openDownload(downloadLink, br.getRedirectLocation()).startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        /* TODO: Wert prüfen */
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
