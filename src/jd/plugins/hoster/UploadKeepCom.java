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

package jd.plugins.hoster;

import java.io.File;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.pluginUtils.Recaptcha;

//uploadkeep.com by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadkeep.com" }, urls = { "http://[\\w\\.]*?uploadkeep\\.com/.+" }, flags = { 0 })
public class UploadKeepCom extends PluginForHost {

    public UploadKeepCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadkeep.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.uploadkeep.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("fname\" value=\"(.*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.remove("method_premium");
        br.submitForm(form);
        
        if (br.containsHTML("reached the download-limit")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 120 * 60 * 1001l); }
        if (br.containsHTML("You have to wait")) {
            if (br.containsHTML("minute")) {
                int minute = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute., (\\d+) seconds until your next download").getMatch(0));
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) minute., (\\d+) seconds until your next download").getMatch(1));
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (minute * 60 + sec) * 1001l);
            } else {
                int sec = Integer.parseInt(br.getRegex("You have to wait (\\d+) seconds until your next download").getMatch(1));
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, sec * 1001l);
            }
        }
        String passCode = null;
        Recaptcha rc = new Recaptcha(br);
        rc.parse();
        rc.load();
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        String c = getCaptchaCode(cf, link);
        if (br.containsHTML("<br><b>Passwort:</b>")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            rc.getForm().put("password", passCode);
        }
        rc.setCode(c);
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            if (br.containsHTML("Wrong password") || br.containsHTML("Wrong captcha")) {
                logger.warning("Wrong password or captcha");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
