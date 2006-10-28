/*
 * CapturePlugin by Andreas Hessel (Vidrec@gmx.de), Bodo Tasche
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * CVS information:
 *  $RCSfile$
 *   $Source$
 *     $Date$
 *   $Author$
 * $Revision$
 */
package captureplugin.drivers.defaultdriver;

import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import sun.misc.BASE64Encoder;
import util.exc.ErrorHandler;
import util.io.StreamReaderThread;
import util.paramhandler.ParamParser;
import util.ui.Localizer;
import util.ui.UiUtilities;

import captureplugin.utils.CaptureUtilities;

/**
 * This Class contains excutes the Application/URL
 */
public class CaptureExecute {

    /** Translator */
    private static final Localizer mLocalizer = Localizer.getLocalizerFor(CaptureExecute.class);

    /** Data for Export */
    private DeviceConfig mData = new DeviceConfig();

    /** Dialog for Settings if Settings not complete */
    private DefaultKonfigurator mDialog;

    /** ParentFrame */
    private Component mParent;
    
    /** Success ? */
    private boolean mError = true;

    /**
     * Creates the Execute
     * 
     * @param frame Frame
     * @param plugin Plugin
     * @param data Data
     * @param dialog Dialgo
     * @param useReturnValue use the Return-Value 
     */
    public CaptureExecute(Component frame, DeviceConfig data) {
        mParent = frame;
        mData = data;
        if (frame instanceof JDialog) {
            mDialog = new DefaultKonfigurator((JDialog)frame, data);
        } else {
            mDialog = new DefaultKonfigurator((JFrame)frame, data);
        }
    }
    
    /**
     * Add a Program
     * @param programTime Program to add
     * @return Success?
     */
    public boolean addProgram(ProgramTime programTime) {
        if (mData.getParameterFormatAdd().trim().length() == 0){
            JOptionPane.showMessageDialog(mParent, mLocalizer.msg("NoParamsAdd", "Please specify Parameters for adding of the Program!"),
                    mLocalizer.msg("CapturePlugin", "Capture Plugin"), JOptionPane.OK_OPTION);
            mDialog.show(DefaultKonfigurator.TAB_PARAMETER);
            return false;
        }

        if (mData.getParameterFormatRem().trim().length() == 0){
          JOptionPane.showMessageDialog(mParent, mLocalizer.msg("NoParamsRemove", "Please specify Parameters for removing of the Program!"),
                  mLocalizer.msg("CapturePlugin", "Capture Plugin"), JOptionPane.OK_OPTION);
          mDialog.show(DefaultKonfigurator.TAB_PARAMETER);
          return false;
      }        
        
        return execute(programTime, mData.getParameterFormatAdd());
    }
    
    /**
     * Remove a Program
     * @param programTime Program to remove
     * @return Success?
     */
    public boolean removeProgram(ProgramTime programTime) {
        if ((mData.getParameterFormatAdd().trim().length() == 0) || ((mData.getParameterFormatRem().trim().length() == 0))){
            JOptionPane.showMessageDialog(mParent, mLocalizer.msg("NoParams", "Please specify Parameters for the Program!"),
                    mLocalizer.msg("CapturePlugin", "Capture Plugin"), JOptionPane.OK_OPTION);
            mDialog.show(DefaultKonfigurator.TAB_PARAMETER);
            return false;
        }
        
        return execute(programTime, mData.getParameterFormatRem());
    }
    
    /**
     * Executes the Program in mData and uses program
     * 
     * @param program Program to use for Command-Line
     */
    public boolean execute(ProgramTime programTime, String param) {
        try {
            String output = "";
            String channelNumber;

            if (!checkParams()) {
                return false;
            }
            
            ParamParser parser = new ParamParser(new CaptureParamLibrary(mData, programTime));
            
            String params = parser.analyse(param, programTime.getProgram());

            if (parser.hasErrors()) {
              JOptionPane.showMessageDialog(mParent, parser.getErrorString(), Localizer.getLocalization(Localizer.I18N_ERROR), JOptionPane.ERROR_MESSAGE);
              return false;
            }
            
            if (mData.getUseWebUrl()) {
                output = executeUrl(params);
            } else {
                output = executeApplication(params);
            }
                      
            params = CaptureUtilities.replaceIgnoreCase(params, mData.getPassword(), "***");
            output = CaptureUtilities.replaceIgnoreCase(output, mData.getPassword(), "***");
         
            if (!mData.useReturnValue()) {
                mError = false;
            }
            
            if (mError) {
                ResultDialog dialog;
                
                if (mParent instanceof JDialog) {
                    dialog = new ResultDialog((JDialog) mParent, params, output, true);
                } else {
                    dialog = new ResultDialog((JFrame) mParent, params, output, true);
                }
                UiUtilities.centerAndShow(dialog);
                return false;
            } 
            
            if (!mData.getDialogOnlyOnError() || (mData.getDialogOnlyOnError() && mError)) {
                ResultDialog dialog;

                if (mParent instanceof JDialog) {
                    dialog = new ResultDialog((JDialog) mParent, params, output, false);
                } else {
                    dialog = new ResultDialog((JFrame) mParent, params, output, false);
                }

                
                UiUtilities.centerAndShow(dialog);
            }

        } catch (Exception e) {
            ErrorHandler.handle( mLocalizer.msg("ErrorExecute", "Error while excecuting."), e);
            return false;
        }
        
        return true;
    }
    
    /** 
     * Checks the Parameters
     * @return true if OK
     */
    private boolean checkParams() {
        if (!mData.getUseWebUrl() && (mData.getProgramPath().trim().length() == 0)) {
            JOptionPane.showMessageDialog(mParent, mLocalizer.msg("NoProgram", "Please specify Application to use!"), mLocalizer
                    .msg("CapturePlugin", "Capture Plugin"), JOptionPane.OK_OPTION);
            mDialog.show(DefaultKonfigurator.TAB_PATH);
            return false;
        }
        if (mData.getUseWebUrl() && (mData.getWebUrl().trim().length() == 0)) {
            JOptionPane.showMessageDialog(mParent, mLocalizer.msg("NoUrl", "Please specify URL to use!"), mLocalizer
                    .msg("CapturePlugin", "Capture Plugin"), JOptionPane.OK_OPTION);
            mDialog.show(DefaultKonfigurator.TAB_PATH);
            return false;
        }
        return true;
    }
    
    /**
     * Starts an Application
     * @param params Params for the Application
     * @return Output of Application
     * @throws Exception
     */
    private String executeApplication(String params) throws Exception{
        
        Process p = null;
        try {
            String path = mData.getProgramPath();            
            path = path.substring(0,path.lastIndexOf(File.separator) + 1);
            
            if(path == null || path.length() < 1 || !(new File(path).isDirectory()))
              path = System.getProperty("user.dir");
            
            p = Runtime.getRuntime().exec((mData.getProgramPath() + " " + params).split(" "), null, new File(path));
        } catch (Exception e) {
            ErrorHandler.handle(mLocalizer.msg("ProblemAtStart", "Problems while starting Application."), e);
            return null;
        }

        String output = "";
        int time = 0;
        
        StreamReaderThread out = new StreamReaderThread(p.getInputStream(),true);
        StreamReaderThread error = new StreamReaderThread(p.getErrorStream(),false);
        out.start();
        error.start();
        
        // wait until the process has exited, max MaxTimouts
        
        if (mData.getTimeOut() > 0 ){
            while (time < mData.getTimeOut() * 1000) {
                Thread.sleep(100);
                time += 100;
                try {
                    p.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                }
            }
        } else {
            while (true) {
                Thread.sleep(100);
                try {
                    p.exitValue();
                    break;
                } catch (IllegalThreadStateException e) {
                }
            }
        }
        
        while (time < mData.getTimeOut() * 1000) {
            Thread.sleep(100);
            time += 100;
            try {
                p.exitValue();
                break;
            } catch (IllegalThreadStateException e) {
            }
        }

        // get the process output
        
        if(!out.isAlive())
          output = out.getOutput();

        if (p.exitValue() != 0) {
            mError = true;
        } else {
            mError = false;
        }
        
        return output;
    }
    
    
    /**
     * Executes the URL
     * @param params Params for the URL
     * @return
     */
    private String executeUrl(String params) throws Exception{
        StringBuffer result = new StringBuffer();
        
        URL url = new URL (mData.getWebUrl() + "?" +params);
        
        URLConnection uc = url.openConnection();
        

        BASE64Encoder enc = new BASE64Encoder();
        
        String userpassword = mData.getUserName() + ":" + mData.getPassword();
        String encoded = new sun.misc.BASE64Encoder().encode (userpassword.getBytes());

        uc.setRequestProperty  ("Authorization", "Basic " + encoded);

        if (uc instanceof HttpURLConnection) {
            if (((HttpURLConnection)uc).getResponseCode() != HttpURLConnection.HTTP_OK) {
                InputStream content = (InputStream) ((HttpURLConnection)uc).getErrorStream();
                BufferedReader in =  new BufferedReader (new InputStreamReader (content));
                String line;
                while ((line = in.readLine()) != null) {
                  result.append(line);
                }
                mError = true;
                return result.toString();
            }
        }
        
        InputStream content = (InputStream)uc.getInputStream();
        BufferedReader in =  new BufferedReader (new InputStreamReader (content));
        String line;
        while ((line = in.readLine()) != null) {
          result.append(line);
        }
        
        mError = false;
        return result.toString();
    }
    

}