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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "myzuka.ru" }, urls = { "http://(www\\.)?myzuka\\.ru/Album/\\d+" }, flags = { 0 })
public class MyzukaRuDecrypter extends PluginForDecrypt {

    public MyzukaRuDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink("directhttp://" + parameter);
            offline.setFinalFileName(new Regex(parameter, "https?://[^<>\"/]+/(.+)").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String[] info = br.getRegex("(<div id=\"playerDiv\\d+\".*?)class=\"player\\-controls\"").getColumn(0);
        if (info == null || info.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String fpName = br.getRegex("<h1 class=\"green\">([^<>\"]*?)</h1>").getMatch(0);
        if (fpName == null) {
            fpName = new Regex(br.getURL(), "myzuka\\.ru/Album/\\d+/(.+)").getMatch(0);
        }
        for (final String singleLink : info) {
            final String url = new Regex(singleLink, "href=\"(/Song/\\d+/[A-Za-z0-9\\-_]+)\"").getMatch(0);
            final String title = new Regex(singleLink, "href=\"/Song/\\d+/[A-Za-z0-9\\-_]+\">([^<>\"]*?)</a>").getMatch(0);
            final String artist = new Regex(singleLink, "href=\"/Artist/\\d+/[A-Za-z0-9\\-_]+\">([^<>\"]*?)</a>").getMatch(0);
            final String filesize = new Regex(singleLink, "(\\d{1,2}(,\\d{1,2})?) Мб").getMatch(0);
            if (url == null || title == null || artist == null || filesize == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            final DownloadLink fina = createDownloadlink("http://myzuka.ru" + Encoding.htmlDecode(url));
            fina.setName(Encoding.htmlDecode(artist) + " - " + Encoding.htmlDecode(title) + ".mp3");
            fina.setDownloadSize(SizeFormatter.getSize(filesize + "MB"));
            fina.setAvailable(true);
            decryptedLinks.add(fina);
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
