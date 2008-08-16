//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
//along with this program.  If not, see <http://gnu.org/licenses/>.

package jd.plugins.decrypt;

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class Collectr extends PluginForDecrypt {

    static private String host = "collectr.net";

    static private final Pattern patternAb18 = Pattern.compile("Hast du das 18 Lebensjahr bereits abgeschlossen\\?.*");
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?collectr\\.net/out/[0-9]*[/]{0,1}[\\d]*", Pattern.CASE_INSENSITIVE);

    public Collectr() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {

        Browser.clearCookies(host);

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.getPage(parameter);

        Form[] forms = br.getForms();

        if (Regex.matches(br.toString(), patternAb18)) {
            forms[0].put("o18", "o18=true");
            br.submitForm(forms[0]);
        }

        String links[] = br.getRegex("<iframe id=\"displayPage\" src=\"(.*?)\" name=\"displayPage\"").getColumn(0);
        progress.setRange(links.length);

        for (String element : links) {
            decryptedLinks.add(createDownloadlink(element));
            progress.increase(1);
        }

        return decryptedLinks;
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
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}