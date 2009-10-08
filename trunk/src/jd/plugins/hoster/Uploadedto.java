//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploaded.to" }, urls = { "(http://[\\w\\.]*?uploaded\\.to/.*?(file/|\\?id=|&id=)[\\w]+/?)|(http://[\\w\\.]*?ul\\.to/[\\w\\-]+/.+)|(http://[\\w\\.]*?ul\\.to/[\\w\\-]+/?)" }, flags = { 2 })
public class Uploadedto extends PluginForHost {

    public Uploadedto(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uploaded.to/ref?id=70683&r");
        setMaxConnections(20);
        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER,
        // getPluginConfig(), "PREMIUMCHUNKS",
        // JDLocale.L("plugins.hoster.uploadedto.chunks",
        // "Premium connections # (>1 causes higher traffic)"), 1,
        // 20).setDefaultValue(1).setStep(1));
    }

    /**
     * Korrigiert den Downloadlink in ein einheitliches Format
     * 
     * @param link
     */
    @Override
    public void correctDownloadLink(DownloadLink link) {
        String url = link.getDownloadURL();
        url = url.replace("ul.to/", "uploaded.to/file/");
        url = url.replace("/?id=", "/file/");
        url = url.replace("?id=", "file/");
        url = url.replaceFirst("/\\?.*?&id=", "/file/");
        String[] parts = url.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++) {
            newLink += parts[t] + "/";
        }

        link.setUrlDownload(newLink);
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 800;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.getPage("http://uploaded.to/login");

        Form login = br.getForm(0);
        login.put("email", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(login);
        if (br.getCookie("http://uploaded.to", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.containsHTML("Login failed!")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    private boolean isPremium() {
        String accounttype = br.getMatch("Accounttype:</span> <span class=.*?>(.*?)</span>");
        if (accounttype != null && accounttype.equalsIgnoreCase("free")) { return false; }
        return true;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!isPremium()) {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            ai.setAccountBalance((long) (Double.parseDouble(balance) * 100));
            ai.setPremiumPoints(Long.parseLong(points));
            ai.setValidUntil(System.currentTimeMillis() + (356 * 24 * 60 * 60 * 1000l));
            ai.setStatus("Accounttyp: Collectorsaccount");
        } else {
            String balance = br.getMatch("Your bank balance is:</span> <span class=.*?>(.*?)</span>");
            String opencredits = br.getMatch("Open credits:</span> <span class=.*?>(.*?)</span>");
            String points = br.getMatch("Your point account is:</span>.*?<span class=.*?>(\\d*?)</span>");
            String traffic = br.getMatch("Traffic left: </span><span class=.*?>(.*?)</span> ");
            String expire = br.getMatch("Valid until: </span> <span class=.*?>(.*?)</span>");
            ai.setValidUntil(Regex.getMilliSeconds(expire, "dd-MM-yyyy hh:mm", null));
            if (opencredits != null) {
                double b = (Double.parseDouble(balance) + Double.parseDouble(opencredits)) * 100;
                ai.setAccountBalance((long) b);
            } else {
                ai.setAccountBalance((long) (Double.parseDouble(balance) * 100));
            }
            ai.setTrafficLeft(Regex.getSize(traffic));
            ai.setTrafficMax(50 * 1024 * 1024 * 1024l);
            ai.setPremiumPoints(Long.parseLong(points));
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        requestFileInformation(downloadLink);
        login(account);
        if (!isPremium()) {
            logger.severe("Entered a Free-account");
            linkStatus.setStatus(LinkStatus.ERROR_PREMIUM);
            linkStatus.setErrorMessage(JDL.L("plugins.hoster.uploadedto.errors.notpremium", "This is free account"));
            linkStatus.setValue(PluginException.VALUE_ID_PREMIUM_DISABLE);
            return;
        }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        checkPassword(downloadLink);
        String error = new Regex(br.getRedirectLocation(), "http://uploaded.to/\\?view=(.*)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getRedirectLocation(), "\\?view=(.*?)&id\\_a").getMatch(0);
        }
        if (error != null) {
            if (error.equalsIgnoreCase("error_traffic")) {
                linkStatus.setErrorMessage(JDL.L("plugins.hoster.uploadedto.errorso.premiumtrafficreached", "Traffic limit reached"));
                linkStatus.addStatus(LinkStatus.ERROR_PREMIUM);
                linkStatus.setValue(PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                return;

            }
            String message = JDL.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage("ServerError: " + message);
            return;

        }

        if (br.getRedirectLocation() == null) {
            logger.info("InDirect Downloads active");
            Form form = br.getFormBySubmitvalue("Download");
            URLConnectionAdapter con = br.openFormConnection(form);
            if (br.getRedirectLocation() == null) {
                con.disconnect();
                logger.severe("Endlink not found");
                linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFEKT);
                linkStatus.setErrorMessage(JDL.L("plugins.hoster.uploadedto.errors.indirectlinkerror", "Indirect link error"));
                return;
            }
        } else {
            logger.info("Direct Downloads active");
        }

        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 0);

        dl.setFileSizeVerified(true);
        if (dl.getConnection().getLongContentLength() == 0 || !dl.getConnection().isContentDisposition()) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(5 * 60 * 1000l);
            return;
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://uploaded.to/agb";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String id = new Regex(downloadLink.getDownloadURL(), "uploaded.to/file/(.*?)/").getMatch(0);

        br.getPage("http://uploaded.to/api/file?id=" + id);
        String[] lines = Regex.getLines(br + "");
        try {
            String fileName = lines[0].trim();

            long fileSize = Long.parseLong(lines[1].trim());
            downloadLink.setFinalFileName(fileName);
            downloadLink.setDownloadSize(fileSize);
            downloadLink.setSha1Hash(lines[2].trim());
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("error_fileremoved")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    private void checkPassword(DownloadLink downloadLink) throws Exception {
        Form form = br.getForm(0);
        String passCode = null;
        if (form != null && form.hasInputFieldByName("file_key")) {
            logger.info("pw protected link");
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            form.put("file_key", passCode);
            br.setFollowRedirects(false);
            br.submitForm(form);
            form = br.getForm(0);
            if (form != null && form.hasInputFieldByName("file_key")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                downloadLink.setProperty("pass", passCode);
            }
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        requestFileInformation(downloadLink);
        br.setCookie("http://uploaded.to/", "lang", "de");
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        checkPassword(downloadLink);
        if (br.containsHTML("ist aufgebraucht")) {
            long wait = Regex.getMilliSeconds(br.getRegex("\\(Oder warten Sie (.*?)\\!\\)").getMatch(0));
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            logger.info("Traffic Limit reached....");
            linkStatus.setValue(wait);
            return;
        }
        String error = new Regex(br.getURL(), "http://uploaded.to/\\?view=(.*?)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getURL(), "\\?view=(.*?)&id\\_a").getMatch(0);
        }
        if (error != null) {
            String message = JDL.L("plugins.errors.uploadedto." + error, error.replaceAll("_", " "));
            logger.severe("Fatal error 1");
            linkStatus.addStatus(LinkStatus.ERROR_FATAL);
            linkStatus.setErrorMessage(message);
            return;

        }

        br.setFollowRedirects(false);

        Form form = br.getFormbyProperty("name", "download_form");
        if (form == null && br.containsHTML("Versuch es sp")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.uploadedto.errors.serverproblem", "Server problem"), 10 * 60 * 1000l);
        if (form != null) {
            form.put("download_submit", "Download");
            // sleep(10000l, downloadLink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, false, 1);
        } else {
            String dlLink = br.getRedirectLocation();
            if (dlLink == null) {

                logger.severe("Fatal error 1\r\n" + br);
                throw new PluginException(LinkStatus.ERROR_FATAL);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlLink, false, 1);
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Sie laden bereits eine Datei herunter")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.fakeContentRangeHeader(false);
        dl.setFileSizeVerified(true);
        if (dl.getConnection().getLongContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(10 * 60 * 1000l);
            return;
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
