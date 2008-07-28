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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

public class Wiireloaded extends PluginForDecrypt {

    static private final String host = "wii-reloaded.ath.cx";

    private String version = "1.0.0.0";

    // http://wii-reloaded.ath.cx/protect/get.php?i=fkqXV249FN5el5waT
    static private final Pattern patternSupported = Pattern.compile("http://wii-reloaded\\.ath\\.cx/protect/get\\.php\\?i=.+", Pattern.CASE_INSENSITIVE);

    public Wiireloaded() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        //currentStep = steps.firstElement();
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
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        Vector<String> link_passwds = new Vector<String>();
        link_passwds.add("wii-reloaded.info");
        //if (step.getStep() == PluginStep.STEP_DECRYPT) {
            ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
            //step.setParameter(decryptedLinks);
            Browser br = new Browser();
            br.setFollowRedirects(false);
            progress.setRange(3);
            br.getPage(parameter);
            String page = br.getPage(parameter);

            progress.increase(1);
            int max = 10;
            while (page.contains("captcha/captcha.php") || page.contains("Sicherheitscode war falsch")) {
                if (max-- <= 0) {
                    logger.severe("Captcha Code has been wrong many times. abort.");
                    return step;

                }
                String adr = "http://wii-reloaded.ath.cx/protect/captcha/captcha.php";
                File captchaFile = getLocalCaptchaFile(this, ".jpg");
                boolean fileDownloaded = JDUtilities.download(captchaFile, br.openGetConnection(adr));
                progress.addToMax(1);
                if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {

                } else {
                    logger.info("captchafile: " + captchaFile);
                    String capTxt = Plugin.getCaptchaCode(captchaFile, this);
                    br.getPage(parameter);
                    Form[] forms = br.getForms();
                    Form post = forms[0];
                    logger.info("set " + post.setVariable(0, capTxt) + " = " + capTxt);
                    page = br.submitForm(post);
                }
            }
            String[][] ids = new Regex(page, "onClick=\"popup_dl\\((.*?)\\)\"").getMatches();
            progress.addToMax(ids.length);
            for (int i = 0; i < ids.length; i++) {
                String u = "http://wii-reloaded.ath.cx/protect/hastesosiehtsaus.php?i=" + ids[i][0];
                String calc_page = br.getPage(u);                
                String rechnung[][] = new Regex(calc_page,Pattern.compile("\\((\\w+) (\\+|\\-) (\\w+) = \\?\\)",Pattern.CASE_INSENSITIVE)).getMatches();
                Integer calc_result ;
                if (rechnung[0][1].contains("+")){
                    calc_result = RomanToInt(rechnung[0][0])+RomanToInt(rechnung[0][2]);
                }else{
                    calc_result = RomanToInt(rechnung[0][0])-RomanToInt(rechnung[0][2]);
                }
                Form form = br.getForms()[0];
                form.setVariable(0, calc_result.toString());
                br.submitForm(form);
                if (br.getRedirectLocation() != null) {
                    DownloadLink link = this.createDownloadlink(br.getRedirectLocation());
                    link.setSourcePluginPasswords(link_passwds);
                    decryptedLinks.add(link);
                }
                progress.increase(1);
            }

            progress.increase(1);

        }

        return step;

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    public int RomanToInt(String roman) {
        char[] chars = roman.toCharArray();
        char lastChar = ' ';
        int value = 0;

        for (int i = chars.length - 1; i >= 0; i--) {
            switch (chars[i]) {
            case 'I':
                if (lastChar == 'X' || lastChar == 'V')
                    value -= 1;
                else
                    value += 1;
                break;
            case 'V':
                value += 5;
                break;
            case 'X':
                if (lastChar == 'C' || lastChar == 'L')
                    value -= 10;
                else
                    value += 10;
                break;
            case 'L':
                value += 50;
                break;
            case 'C':
                if (lastChar == 'M' || lastChar == 'D')
                    value -= 100;
                else
                    value += 100;
                break;
            case 'D':
                value += 500;
                break;
            case 'M':
                value += 1000;
                break;
            }
            lastChar = chars[i];
        }
        return value;
    }

}
