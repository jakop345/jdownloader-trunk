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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class OwLy extends PluginForDecrypt {

    public OwLy(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        System.out.println("Seite::::::::::::::\n"+br.getPage(parameter));
        String declink = br.getRegex("<iframe frameborder=\"0\"  src=\"(.*?)\"").getMatch(0);
        decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(declink)));
        return decryptedLinks;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision: 6294 $");
    }
}