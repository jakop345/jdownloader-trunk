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

package jd.utils;

import java.io.File;
import java.util.logging.Logger;

import jd.controlling.ProgressController;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.plugins.DownloadLink;
import jd.utils.locale.JDL;
import de.savemytube.flv.FLV;

public class JDMediaConvert {

    private static Logger logger = jd.controlling.JDLogger.getLogger();

    private static String TempExt = ".tmp$";

    public static boolean ConvertFile(DownloadLink downloadlink, ConversionMode InType, ConversionMode OutType) {
        logger.info("Convert " + downloadlink.getName() + " - " + InType.getText() + " - " + OutType.getText());
        if (InType.equals(OutType)) {
            logger.info("No Conversion needed, renaming...");
            File oldone = new File(downloadlink.getFileOutput());
            File newone = new File(downloadlink.getFileOutput().replaceAll(TempExt, OutType.getExtFirst()));
            downloadlink.setFinalFileName(downloadlink.getName().replaceAll(TempExt, OutType.getExtFirst()));
            oldone.renameTo(newone);
            return true;
        }

        ProgressController progress = new ProgressController(JDL.L("convert.progress.convertingto", "Konvertiere zu") + " " + OutType.toString(), 3, null);
        downloadlink.getLinkStatus().setStatusText(JDL.L("convert.progress.convertingto", "Konvertiere zu") + " " + OutType.toString());
        progress.increase(1);
        switch (InType) {
        case VIDEOFLV:
            // Inputformat FLV
            switch (OutType) {
            case AUDIOMP3:
                logger.info("Convert FLV to mp3...");
                new FLV(downloadlink.getFileOutput(), true, true);
                progress.increase(1);
                // FLV löschen
                if (!new File(downloadlink.getFileOutput()).delete()) {
                    new File(downloadlink.getFileOutput()).deleteOnExit();
                }
                // AVI löschen
                if (!new File(downloadlink.getFileOutput().replaceAll(TempExt, ".avi")).delete()) {
                    new File(downloadlink.getFileOutput().replaceAll(TempExt, ".avi")).deleteOnExit();
                }
                progress.doFinalize();
                return true;
            case AUDIOMP3_AND_VIDEOFLV:
                logger.info("Convert FLV to mp3 (keep FLV)...");
                new FLV(downloadlink.getFileOutput(), true, true);
                progress.increase(1);
                // AVI löschen
                if (!new File(downloadlink.getFileOutput().replaceAll(TempExt, ".avi")).delete()) {
                    new File(downloadlink.getFileOutput().replaceAll(TempExt, ".avi")).deleteOnExit();
                }
                // Rename tmp to flv
                new File(downloadlink.getFileOutput()).renameTo(new File(downloadlink.getFileOutput().replaceAll(".tmp", ConversionMode.VIDEOFLV.getExtFirst())));
                progress.doFinalize();
                return true;
            default:
                logger.warning("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
                downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unbekanntes Format"));
                progress.doFinalize();
                return false;
            }
        default:
            logger.warning("Don't know how to convert " + InType.getText() + " to " + OutType.getText());
            downloadlink.getLinkStatus().setErrorMessage(JDL.L("convert.progress.unknownintype", "Unbekanntes Format"));
            progress.doFinalize();
            return false;
        }
    }
}