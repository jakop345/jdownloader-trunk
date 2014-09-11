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

package jd.controlling;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

/**
 * Im JDController wird das ganze App gesteuert. Events werden deligiert.
 * 
 * @author JD-Team/astaldo
 */
public class JDController {

    public static JDController getInstance() {
        return INSTANCE;
    }

    /**
     * Der Download Watchdog verwaltet die Downloads
     */

    private static JDController INSTANCE = new JDController();

    @Deprecated
    public ArrayList<DownloadLink> getContainerLinks(File file) {
        final LinkCrawler lc;
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            final LinkCrawlerThread thread = (LinkCrawlerThread) (Thread.currentThread());
            Object owner = thread.getCurrentOwner();
            final CrawledLink source;
            if (owner instanceof PluginForDecrypt) {
                source = ((PluginForDecrypt) owner).getCurrentLink();
            } else {
                source = null;
            }
            final LinkCrawler parent = thread.getCurrentLinkCrawler();
            lc = new LinkCrawler(false, false) {

                @Override
                protected CrawledLink crawledLinkFactorybyURL(String url) {
                    final CrawledLink ret = new CrawledLink(url);
                    if (source != null) {
                        ret.setSourceLink(source);
                    }
                    return ret;
                }

                @Override
                public int getCrawlerGeneration(boolean thisGeneration) {
                    if (!thisGeneration && parent != null) {
                        return Math.max(crawlerGeneration.get(), parent.getCrawlerGeneration(false));
                    }
                    return crawlerGeneration.get();
                }

                @Override
                public List<LazyCrawlerPlugin> getCrawlerPlugins() {
                    if (parent != null) {
                        return parent.getCrawlerPlugins();
                    }
                    return super.getCrawlerPlugins();
                }

                @Override
                protected boolean distributeCrawledLink(CrawledLink crawledLink) {
                    return crawledLink != null && crawledLink.getSourceUrls() == null;
                }

            };
        } else {
            lc = new LinkCrawler();
        }
        lc.crawl("file://" + file.getAbsolutePath());
        lc.waitForCrawling();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>(lc.getCrawledLinks().size());
        for (final CrawledLink link : lc.getCrawledLinks()) {
            DownloadLink dl = link.getDownloadLink();
            if (dl == null) {
                final String url = link.getURL();
                if (url != null) {
                    dl = new DownloadLink(null, null, null, url, true);
                }
            }
            if (dl != null) {
                ret.add(dl);
            }
        }
        return ret;
    }
}