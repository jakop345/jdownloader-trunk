package org.jdownloader.extensions.eventscripter.sandboxobjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JsonKeyValueStorage;
import org.appwork.storage.Storable;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.api.linkcollector.v2.CrawledLinkAPIStorableV2;
import org.jdownloader.api.linkcollector.v2.CrawledLinkQueryStorable;
import org.jdownloader.api.linkcollector.v2.LinkCollectorAPIImplV2;
import org.jdownloader.extensions.eventscripter.ScriptAPI;
import org.jdownloader.extensions.extraction.Archive;
import org.jdownloader.extensions.extraction.bindings.crawledlink.CrawledLinkFactory;
import org.jdownloader.extensions.extraction.contextmenu.downloadlist.ArchiveValidator;

import jd.controlling.linkcrawler.CrawledLink;

@ScriptAPI(description = "The context download list link")
public class CrawledLinkSandbox {

    private final CrawledLink                                              link;
    private final CrawledLinkAPIStorableV2                                 storable;

    private final static WeakHashMap<CrawledLink, HashMap<String, Object>> SESSIONPROPERTIES = new WeakHashMap<CrawledLink, HashMap<String, Object>>();

    public CrawledLinkSandbox(CrawledLink link) {
        this.link = link;
        storable = LinkCollectorAPIImplV2.toStorable(CrawledLinkQueryStorable.FULL, link);

    }

    public String getAvailableState() {
        return storable.getAvailability() + "";
    }

    public CrawledLinkSandbox() {
        link = null;
        storable = new CrawledLinkAPIStorableV2();
    }

    public Object getProperty(String key) {
        if (link != null) {
            return link.getDownloadLink().getProperty(key);
        }
        return null;
    }

    public Object getSessionProperty(final String key) {
        final CrawledLink link = this.link;
        if (link != null) {
            synchronized (SESSIONPROPERTIES) {
                final HashMap<String, Object> properties = SESSIONPROPERTIES.get(link);
                if (properties != null) {
                    return properties.get(key);
                }
            }
        }
        return null;
    }

    public void setSessionProperty(final String key, final Object value) {
        if (link != null) {
            if (value != null) {
                if (!canStore(value)) {
                    throw new WTFException("Type " + value.getClass().getSimpleName() + " is not supported");
                }
            }
            synchronized (SESSIONPROPERTIES) {
                HashMap<String, Object> properties = SESSIONPROPERTIES.get(link);
                if (properties == null) {
                    properties = new HashMap<String, Object>();
                    SESSIONPROPERTIES.put(link, properties);
                }
                properties.put(key, value);
            }
        }
    }

    public String getUUID() {
        if (link != null) {
            return link.getUniqueID().toString();
        }
        return null;
    }

    public void setProperty(String key, Object value) {
        if (link != null) {
            if (value != null) {
                if (!canStore(value)) {
                    throw new WTFException("Type " + value.getClass().getSimpleName() + " is not supported");
                }
            }
            link.getDownloadLink().setProperty(key, value);
        }
    }

    private boolean canStore(final Object value) {
        return value == null || Clazz.isPrimitive(value.getClass()) || JsonKeyValueStorage.isWrapperType(value.getClass()) || value instanceof Storable;
    }

    public ArchiveSandbox getArchive() {
        if (link == null || ArchiveValidator.EXTENSION == null) {
            return null;
        }
        final Archive archive = ArchiveValidator.EXTENSION.getArchiveByFactory(new CrawledLinkFactory(link));
        if (archive != null) {
            return new ArchiveSandbox(archive);
        }
        final ArrayList<Object> list = new ArrayList<Object>();
        list.add(link);
        final List<Archive> archives = ArchiveValidator.getArchivesFromPackageChildren(list);
        return (archives == null || archives.size() == 0) ? null : new ArchiveSandbox(archives.get(0));
    }

    public String getComment() {
        if (link != null) {
            return link.getComment();
        }
        return null;
    }

    public void setEnabled(boolean b) {
        if (link != null) {
            link.setEnabled(b);
        }
    }

    public String getDownloadPath() {
        if (link == null) {
            switch (CrossSystem.getOSFamily()) {
            case WINDOWS:
                return "c:\\I am a dummy folder\\Test.txt";
            default:
                return "/mnt/Text.txt";
            }
        }
        return link.getDownloadLink().getFileOutput();
    }

    @ScriptAPI(description = "Sets a new filename", parameters = { "new Name" })
    public void setName(String name) {
        if (link != null) {
            link.setName(name);

        }
    }

    public String getUrl() {
        if (link != null) {
            return link.getURL();
        }
        return null;
    }

    public long getBytesTotal() {
        if (link != null) {
            return link.getSize();
        }
        return -1;
    }

    public String getName() {
        if (link == null) {
            return "Test.txt";
        }
        return link.getName();
    }

    public CrawledPackageSandbox getPackage() {
        if (link == null) {
            return new CrawledPackageSandbox();
        }
        return new CrawledPackageSandbox(link.getParentNode());
    }

    public String getHost() {
        return storable.getHost();
    }

    public boolean isEnabled() {
        return storable.isEnabled();
    }

    @Override
    public String toString() {
        return "CrawledLink Instance: " + getName();
    }

}