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

package jd.plugins.optional.jdunrar;

import java.util.ArrayList;

public class ArchivFile {

    private String filepath;

    private int percent;

    private long size = 0;

    private boolean isProtected = false;

    private ArrayList<String> volumes;

    public ArchivFile(String name) {
        this.filepath = name;
        volumes = new ArrayList<String>();
    }

    public void addVolume(String vol) {
        if (vol == null) return;
        if (volumes.indexOf(vol) >= 0) return;
        this.volumes.add(vol);

    }

    public String getFilepath() {
        return filepath;
    }

    public int getPercent() {
        return percent;
    }

    public long getSize() {
        return size;
    }

    public ArrayList<String> getVolumes() {
        return volumes;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public void setProtected(boolean b) {
        this.isProtected = b;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String toString() {
        return this.filepath;
    }

}
