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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "canna.to" }, urls = { "http://[uu\\.canna\\.to|85\\.17\\.36\\.224]+/cpuser/links\\.php\\?action=[cp_]*?popup&kat_id=[\\d]+&fileid=[\\d]+" }, flags = { 0 })
public class CnnT extends PluginForDecrypt {

    public CnnT(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static Object LOCK = new Object();

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final String fid = new Regex(parameter, "fileid=(\\d+)").getMatch(0);
        boolean valid = false;
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML(">Versuche es in wenigen Minuten nochmals")) {
            logger.info("Site overloaded at the moment: " + parameter);
            return decryptedLinks;
        }
        if (br.containsHTML("Es existiert kein Eintrag zu dieser ID") || this.br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        synchronized (LOCK) {
            for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                final Form captchaForm = br.getFormbyProperty("name", "download_form");
                captchaForm.setAction("/cpuser/links.php?action=load&fileid=" + fid);
                final String captchaUrl = br.getRegex("\"(securimage_show\\.php\\?sid=[a-z0-9]+)\"").getMatch(0);
                if (captchaUrl == null || captchaForm == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                final String captchaCode = getCaptchaCode("http://uu.canna.to/cpuser/" + captchaUrl, param);
                captchaForm.put("cp_captcha", captchaCode);
                br.submitForm(captchaForm);
                if (br.containsHTML("Der Sicherheitscode ist falsch!")) {
                    /* Falscher Captcha, Seite neu laden */
                    br.getPage(parameter);
                } else {
                    valid = true;
                    String finallink = br.getRegex("URL=(.*?)\"").getMatch(0);
                    if (finallink != null) {
                        decryptedLinks.add(createDownloadlink(finallink));
                    }
                    String links[] = br.getRegex("<a target=\"_blank\" href=\"(.*?)\">").getColumn(0);
                    if (links != null && links.length != 0) {
                        for (String link : links) {
                            decryptedLinks.add(createDownloadlink(link));
                        }
                    }
                    break;
                }
            }
        }
        if (valid == false) {
            logger.info("Captcha for the following link was entered wrong for more than 5 times: " + parameter);
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}