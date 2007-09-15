package jd.captcha;



import java.io.File;

import jd.JDUtilities;
import jd.captcha.utils.UTILITIES;



/**
 * JAC Tester

 * 
 * @author coalado
 */
public class JACTest {
    /**
     * @param args
     */
    public static void main(String args[]){
  
        JACTest main = new JACTest();
        main.go();
    }
    private void go(){
      String methodsPath=UTILITIES.getFullPath(new String[] { JDUtilities.getJDHomeDirectory().getAbsolutePath(), "jd", "captcha", "methods"});
      String hoster="rapidshare.com";

       JAntiCaptcha jac= new JAntiCaptcha(methodsPath,hoster);
     //sharegullicom47210807182105.gif
      jac.setShowDebugGui(true);
//  jac.exportDB();
// jac.importDB();
     LetterComperator.CREATEINTERSECTIONLETTER=true;
      jac.displayLibrary();
     jac.getJas().set("preScanFilter", 100);
//       jac.trainCaptcha(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"+"securedin1730080724541.jpg"), 4);
     jac.showPreparedCaptcha(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"+"8.jpg"));
      
     //UTILITIES.getLogger().info(JAntiCaptcha.getCaptchaCode(UTILITIES.loadImage(new File(JDUtilities.getJDHomeDirectory().getAbsolutePath()+"/jd/captcha/methods"+"/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")), null, "rapidshare.com"));
     //jac.removeBadLetters();
      //jac.addLetterMap();
      //jac.saveMTHFile();

      
   
    }
}