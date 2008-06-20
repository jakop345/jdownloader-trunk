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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Wiireloaded extends PluginForDecrypt {

    static private final String host = "wii-reloaded.ath.cx";

    private String version = "1.0.0.0";

    // http://wii-reloaded.ath.cx/protect/get.php?i=fkqXV249FN5el5waT
    static private final Pattern patternSupported = getSupportPattern("http://wii-reloaded.ath.cx/protect/get\\.php\\?i=[+]");

    private static final String PARAM_CALCCODE = "PARAM_CALCCODE";

    private static int CALCCODE = 0;

    private static String COOKIE = null;

    public Wiireloaded() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        CALCCODE=getProperties().getIntegerProperty(PARAM_CALCCODE);
        setConfigEntries();
            
        
    }

    private void setConfigEntries() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, getProperties(), PARAM_CALCCODE, JDLocale.L("gui.plugins.decrypt.wiireloaded.calcresult","Ergebnis der Rechenaufgabe"),-99999,99999));
        cfg.setDefaultValue(5);
        
        
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);

                progress.setRange(3);
                if (COOKIE == null) {
                    requestInfo = HTTP.getRequest(url);
                    COOKIE = requestInfo.getCookie();
                }
                requestInfo = HTTP.getRequest(url, COOKIE, url + "", false);
                progress.increase(1);
                while (requestInfo.containsHTML("captcha/captcha.php")) {
                    String adr = "http://wii-reloaded.ath.cx/protect/captcha/captcha.php";
                    File captchaFile = getLocalCaptchaFile(this, ".jpg");
                    HTTPConnection con = HTTP.getRequestWithoutHtmlCode(new URL(adr), COOKIE, null, true).getConnection();

                    boolean fileDownloaded = JDUtilities.download(captchaFile, con);
                    progress.addToMax(1);
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {

                    } else {
                        logger.info("captchafile: " + captchaFile);

                        String capTxt = Plugin.getCaptchaCode(captchaFile, this);
                        String postAdr = "http://wii-reloaded.ath.cx/protect/get.php?i=" + SimpleMatches.getSimpleMatch(requestInfo.getHtmlCode(), "<form method=\"post\" action=\"get.php?i=°\">", 0);

                        requestInfo = HTTP.postRequest(new URL(postAdr), COOKIE + "; " + con.getHeaderField("Set-Cookie"), url + "", null, "sicherheitscode=" + capTxt + "&submit=Weiter", false);
                    }
                    ArrayList<String> ids = SimpleMatches.getAllSimpleMatches(requestInfo.getHtmlCode(), "onClick=\"popup_dl(°)\"", 1);

                    progress.addToMax(ids.size());
                    for (int i = 0; i < ids.size(); i++) {
                        String u = "http://wii-reloaded.ath.cx/protect/hastesosiehtsaus.php?i=" + ids.get(i);
                        requestInfo = null;
                        int tmp=CALCCODE;
                        while (requestInfo == null || requestInfo.containsHTML("Bitte Ergebnis eingeben")) {
                            requestInfo = HTTP.postRequest(new URL(u), COOKIE + "; " + con.getHeaderField("Set-Cookie"), u, null, "scode=" + CALCCODE + "&senden=Download", false);
                            CALCCODE++;
                            if (CALCCODE > 200) {
                                CALCCODE = 1;
                                break;
                            }
                        }
                        CALCCODE--;
                        
                        if(tmp!=CALCCODE){
                        getProperties().setProperty(PARAM_CALCCODE, CALCCODE);
                        getProperties().save();
                        }
                        
                        if (requestInfo.getLocation() != null) {
                            decryptedLinks.add(this.createDownloadlink(requestInfo.getLocation()));
                            logger.finer(requestInfo.getLocation());
                        }
                        progress.increase(1);
                    }
                    // Letzten Teil der URL herausfiltern und postrequest
                    // durchführen
                    // String[] result = parameter.split("/");
                    // RequestInfo reqinfo = postRequest(url, "tiny=" +
                    // result[result.length-1] + "&submit=continue");

                    // Link herausfiltern
                    progress.increase(1);
                    // decryptedLinks.add(this.createDownloadlink((getBetween(reqinfo.getHtmlCode(),
                    // "name=\"ifram\" src=\"", "\" marginwidth"))));

                    // Decrypten abschliessen
                }

                step.setParameter(decryptedLinks);
                return step;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}
