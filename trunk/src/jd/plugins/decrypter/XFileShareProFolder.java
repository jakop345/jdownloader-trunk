//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.SiteType.SiteTemplate;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "XFileShareProFolder" }, urls = { "https?://(www\\.)?(subyshare\\.com|brupload\\.net|linestorage\\.com|terafile\\.co|(exclusivefaile\\.com|exclusiveloader\\.com)|sharesix\\.com|ex-load\\.com|hulkload\\.com|sharingmaster\\.com|mediafire\\.bz|anafile\\.com|koofile\\.com|kingfiles\\.net|bestreams\\.net|vodlocker\\.com|powvideo\\.net|vidspot\\.net|upshared\\.com|filewe\\.com|videopremium\\.(net|tv)|lunaticfiles\\.com|expressleech\\.com|youwatch\\.org|enjoybox\\.in|(fileplaneta\\.com|fileplanet\\.com\\.ua)|filebulk\\.com|streamratio\\.com|vshare\\.eu|vidplay\\.net|filepurpose\\.com|livecloudz\\.com|treefiles\\.com|up\\.media1fire\\.com|salefiles\\.com|ortofiles\\.com|verzend\\.be|interfile\\.net|goldbytez\\.com|sanshare\\.com|restfile\\.(ws|ca|co|com)|storagely\\.com|mightyupload\\.com|(4upfiles\\.com|4up\\.(im|me))|thefile\\.me|free\\-uploading\\.com|wizzfile\\.com|rapidfileshare\\.net|rd\\-fs\\.com|fireget\\.com|allbox4\\.com|ishareupload\\.com|project\\-free\\-upload\\.com|gorillavid\\.in|your\\-filehosting\\.com|mp3the\\.net|mooshare\\.biz|xenubox\\.com|mixshared\\.com|longfiles\\.com|helluploads\\.com|novafile\\.com|orangefiles\\.me|ufile\\.eu|qtyfiles\\.com|free\\-uploading\\.com|free\\-uploading\\.com|uppit\\.com|nosupload\\.com|uploadbaz\\.com|simpleshare\\.org|ryushare\\.com|lafiles\\.com|downloadani\\.me|movdivx\\.com|filenuke\\.com|vidbux\\.com|filesabc\\.com|edoc\\.com|filesabc\\.com|faststore\\.org)/(users/[a-z0-9_]+/[^\\?\r\n]+|folder/\\d+/[^\\?\r\n]+)|https?://(?:www\\.)?fileparadox\\.com/f/[a-z0-9]+|http?://(www\\.)?musickapoz\\.se/users/[a-z0-9]+|https?://(?:www\\.)?imgmega\\.com/g/[a-z0-9]+" }, flags = { 0 })
@SuppressWarnings("deprecation")
public class XFileShareProFolder extends PluginForDecrypt {

    // DEV NOTES
    // other: keep last /.+ for fpName. Not needed otherwise.
    // other: group sister sites or aliased domains together, for easy
    // maintenance.
    // TODO: remove old xfileshare folder plugins after next major update.

    private String HOST      = null;
    private String parameter = null;

    /**
     * @author raztoki
     * */
    public XFileShareProFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HashSet<String> dupe = new HashSet<String>();
        parameter = param.toString();
        HOST = new Regex(parameter, "https?://(www\\.)?([^:/]+)").getMatch(1);
        if (HOST == null) {
            logger.warning("Failure finding HOST : " + parameter);
            return null;
        }
        // define custom browser headers and language settings.
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.9");
        br.setCookie("http://" + HOST, "lang", "english");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (br.containsHTML("No such user exist")) {
            logger.warning("Incorrect URL or Invalid user : " + parameter);
            return decryptedLinks;
        }
        // name isn't needed, other than than text output for fpName.
        String fpName = new Regex(parameter, "(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)[^/]+/(.+)").getMatch(1); // name
        if (fpName == null) {
            fpName = new Regex(parameter, "(folder/\\d+/|f/[a-z0-9]+/|go/[a-z0-9]+/)(.+)").getMatch(1); // id
            if (fpName == null) {
                fpName = new Regex(parameter, "users/[a-z0-9_]+/[^/]+/(.+)").getMatch(0); // name
                if (fpName == null) {
                    fpName = new Regex(parameter, "users/[a-z0-9_]+/(.+)").getMatch(0); // id
                    if (fpName == null && parameter.matches(".+(fileparadox\\.com|imgmega\\.com).+")) {
                        fpName = br.getRegex("<H1>(.*?)</H1>").getMatch(0);
                    }
                }
            }
        }
        dupe.add(parameter);
        parsePage(dupe, decryptedLinks);
        parseNextPage(dupe, decryptedLinks);

        if (fpName != null) {
            fpName = "Folder - " + Encoding.htmlDecode(fpName);
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(HashSet<String> dupe, ArrayList<DownloadLink> ret) throws PluginException {
        final String[] links = br.getRegex("href=\"(https?://(www\\.)?" + HOST + "/[a-z0-9]{12})(\"|/)").getColumn(0);
        if (links != null && links.length > 0) {
            for (String dl : links) {
                if (dupe.add(dl)) {
                    ret.add(createDownloadlink(dl));
                }
            }
        }
        String folders[] = br.getRegex("folder.?\\.gif.*?<a href=\"(.+?" + HOST + "[^\"]+users/[^\"]+)").getColumn(0);
        if (folders != null && folders.length > 0) {
            for (String dl : folders) {
                if (dupe.add(dl)) {
                    ret.add(createDownloadlink(dl));
                }
            }
        }
    }

    private boolean parseNextPage(HashSet<String> dupe, ArrayList<DownloadLink> ret) throws IOException, PluginException {
        // not sure if this is the same for normal folders, but the following
        // picks up users/username/*
        String nextPage = br.getRegex("<div class=\"paging\">[^\r\n]+<a href='([^']+&amp;page=\\d+)'>Next").getMatch(0);
        if (nextPage != null) {
            nextPage = HTMLEntities.unhtmlentities(nextPage);
            if (dupe.add(nextPage)) {
                br.getPage(parameter + nextPage);
                parsePage(dupe, ret);
                parseNextPage(dupe, ret);
                return true;
            }
        }
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.SibSoft_XFileShare;
    }

    public String[] siteSupportedNames() {
        return new String[] { "subyshare.com", "brupload.net", "linestorage.com", "terafile.co", "exclusivefaile.com", "exclusiveloader.com", "sharesix.com", "ex-load.com", "hulkload.com", "sharingmaster.com", "mediafire.bz", "anafile.com", "koofile.com", "kingfiles.net", "bestreams.net", "vodlocker.com", "powvideo.net", "vidspot.net", "upshared.com", "filewe.com", "videopremium.net", "videopremium.tv", "lunaticfiles.com", "expressleech.com", "youwatch.org", "enjoybox.in", "fileplaneta.com", "fileplanet.com.ua", "filebulk.com", "streamratio.com", "vshare.eu", "vidplay.net", "filepurpose.com", "livecloudz.com", "treefiles.com", "up.media1fire.com", "salefiles.com", "ortofiles.com", "verzend.be", "interfile.net", "goldbytez.com", "sanshare.com", "restfile.ws", "restfile.ca", "restfile.co", "restfile.com", "storagely.com", "mightyupload.com", "4upfiles.com", "4up.im", "4up.me",
                "thefile.me", "free-uploading.com", "wizzfile.com", "rapidfileshare.net", "rd-fs.com", "fireget.com", "allbox4.com", "ishareupload.com", "project-free-upload.com", "gorillavid.in", "your-filehosting.com", "mp3the.net", "mooshare.biz", "xenubox.com", "mixshared.com", "longfiles.com", "helluploads.com", "novafile.com", "orangefiles.me", "ufile.eu", "qtyfiles.com", "free-uploading.com", "free-uploading.com", "uppit.com", "nosupload.com", "uploadbaz.com", "simpleshare.org", "ryushare.com", "lafiles.com", "downloadani.me", "movdivx.com", "filenuke.com", "vidbux.com", "filesabc.com", "edoc.com", "filesabc.com", "faststore.org", "fileparadox.com", "imgmega.com" };
    }
}