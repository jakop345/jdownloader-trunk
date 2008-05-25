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
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class RelinkUs extends PluginForDecrypt {

    final static String host             = "relink.us";

    private String      version          = "1.0.0.0";

    private Pattern     patternSupported = getSupportPattern("http://[*]relink\\.us/go\\.php\\?id=[0-9]+");

    public RelinkUs() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
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
        return "Relink.us-1.0.0.";
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

    @Override public PluginStep doStep(PluginStep step, String parameter) {
    	if(step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
    		try {
    			URL url = new URL(parameter);
    			RequestInfo reqinfo = getRequest(url);
    			
    			String title=getSimpleMatch(reqinfo, "<div class='ordner_head'>°</div>", 0);
    			
    			ArrayList<ArrayList<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), "action='°' method='post' target='_blank'");
    			progress.setRange( links.size());
    			FilePackage fp= new FilePackage();
    			fp.setName(title);
    			for(int i=0; i<links.size(); i++) {
    				reqinfo = postRequest(new URL("http://relink.us/" + links.get(i).get(0)), "submit=Open");
    				String link=getBetween(reqinfo.getHtmlCode(), "iframe name=\"pagetext\" height=\"100%\" frameborder=\"no\" width=\"100%\" src=\"", "\"");
    				
    				if(link.contains("yourlayer")){
    				    reqinfo=getRequest(new URL(link));
    				    link=getSimpleMatch(reqinfo.getHtmlCode(), "frameborder=\"0\" src=\"°\">", 0);
    				}
    				
    				
    				
    				
    				decryptedLinks.add(this.createDownloadlink(link));
    				fp.add(decryptedLinks.lastElement());
    			progress.increase(1);
    			}
    			
    			// Decrypt abschliessen
    			
    			step.setParameter(decryptedLinks);
    		}
    		catch(IOException e) {
    			 e.printStackTrace();
    		}
    	}
    	return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}