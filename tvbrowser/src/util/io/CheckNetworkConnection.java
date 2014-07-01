/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (martin@tvbrowser.org)
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
package util.io;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Window;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import tvbrowser.core.Settings;
import tvbrowser.ui.mainframe.MainFrame;
import util.ui.UiUtilities;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Checks the Network and creates a Waiting-Dialog if necessary
 * @since 2.2
 */
class CheckNetworkConnection {
  private static final util.ui.Localizer mLocalizer = util.ui.Localizer.getLocalizerFor(CheckNetworkConnection.class);

  private boolean mCheckRunning = true;

  private boolean mResult = false;

  private JDialog mWaitingDialog;

  private final static String[] CHECK_URLS = { 
    "https://duckduckgo.com/",
    "http://www.google.com/",
    "http://www.yahoo.com/",
    "http://www.bing.com/"
  };

  /**
   * Check the Network
   * 
   * @return true, if the connection is working
   */
  public boolean checkConnection() {
    // if checking is disabled, always assume existing connection
    if (!Settings.propInternetConnectionCheck.getBoolean()) {
      return true;
    }
    
    mResult = false;
    
    return checkConnectionInternal(getCheckUrls());      
  }

  /**
   * Checks if a internet connection to a specific Server can be established
   * 
   * @param url check this Server
   * @return true, if a connection can be established
   */
  public boolean checkConnection(final URL url) {
    mResult = false;
    
    return checkConnectionInternal(new URL[] {url});
  }
    

  /**
   * Checks if a internet connection to a specific Server can be established
   * 
   * @param url check this Server
   * @return true, if a connection can be established
   */
  private boolean checkConnectionInternal(final URL[] urls) {
    // Start Check in second Thread
    new Thread(new Runnable() {
      public void run() {
        mCheckRunning = true;
        
        for(URL url : urls) {
          if(!mResult && url != null) {
            try {
              URLConnection test = url.openConnection();
              
              if(test instanceof HttpsURLConnection) {
                HttpsURLConnection connection = (HttpsURLConnection)test;
                mResult = mResult || (connection.getResponseCode() == HttpsURLConnection.HTTP_OK)
                    || (connection.getResponseCode() == HttpsURLConnection.HTTP_SEE_OTHER)
                    || (connection.getResponseCode() == HttpsURLConnection.HTTP_ACCEPTED)
                    || (connection.getResponseCode() == HttpsURLConnection.HTTP_CREATED);
              }
              else {
                HttpURLConnection connection = (HttpURLConnection) test;
                mResult = mResult || (connection.getResponseCode() == HttpURLConnection.HTTP_OK)
                    || (connection.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER)
                    || (connection.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED)
                    || (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED);  
              }
            } catch (IOException e) {}
          }
        }
        
        mCheckRunning = false;
      };
    }, "Check network connection").start();
    
    int num = 0;
    // Wait till second Thread is finished or Settings.propNetworkCheckTimeout is reached
    int timeout = Settings.propNetworkCheckTimeout.getInt()/100;
    while ((mCheckRunning) && (num < timeout)) {
      num++;
      if (num == 7) {
        // Show the Dialog after 700 MS
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            showDialog();
          };
        });
      }
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }
    
    hideDialog();
    return mResult;
  }
  
  
  private void hideDialog() {
    mCheckRunning = false;
    if (mWaitingDialog != null) {
      if(MainFrame.getInstance().isVisible() && UiUtilities.getLastModalChildOf(MainFrame.getInstance()).equals(mWaitingDialog)) {
        mWaitingDialog.dispose();
      } else {
        new Thread("Hide network connection dialog") {
          public void run() {
            setPriority(Thread.MIN_PRIORITY);
            
            while(!MainFrame.getInstance().isVisible() && !UiUtilities.getLastModalChildOf(MainFrame.getInstance()).equals(mWaitingDialog)) {
              try {
                sleep(500);
              }catch(Exception e) {}
            }
            
            mWaitingDialog.dispose();
          }
        }.start();
      }
    }
  }

  private void showDialog() {
    try {
      if (MainFrame.isStarting()) {
        return;
      }
      if(!MainFrame.getInstance().isVisible() || MainFrame.getInstance().getExtendedState() == Frame.ICONIFIED) {
        return;
      }
    }catch(Exception e) {}
    
    if ((mCheckRunning) && (mWaitingDialog == null)) {
      Window comp = UiUtilities.getLastModalChildOf(MainFrame.getInstance());
      if (comp instanceof Dialog) {
        mWaitingDialog = new JDialog((Dialog) comp, false);
      } else {
        mWaitingDialog = new JDialog((Frame) comp, false);
      }
      mWaitingDialog.setUndecorated(true);
      mWaitingDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));

      JPanel panel = (JPanel) mWaitingDialog.getContentPane();
      panel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

      panel.setLayout(new FormLayout("3dlu, pref, 3dlu", "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu"));
      CellConstraints cc = new CellConstraints();

      JLabel header = new JLabel(mLocalizer.msg("header", "Header"));
      header.setFont(header.getFont().deriveFont(Font.BOLD));

      panel.add(header, cc.xy(2, 2));

      panel.add(
          new JLabel(mLocalizer.msg("pleaseWait", "Checking the internet connection... This may take up to {0} seconds.", Settings.propNetworkCheckTimeout.getInt()/1000)), cc
              .xy(2, 4));

//      JProgressBar bar = new JProgressBar();
//      bar.setIndeterminate(true);
//      panel.add(bar, cc.xy(2, 6));

      mWaitingDialog.pack();
      if(mCheckRunning) {
        UiUtilities.centerAndShow(mWaitingDialog);
      }
      mWaitingDialog.setVisible(mCheckRunning && MainFrame.getInstance().isVisible() && MainFrame.getInstance().getExtendedState() != Frame.ICONIFIED);
    }
  }

  public static String[] getUrls() {
    return CHECK_URLS;
  }
  
  private static URL[] getCheckUrls() {
    URL[] check = new URL[CHECK_URLS.length];
    
    for(int i = 0; i < CHECK_URLS.length; i++) {
      try {
        check[i] = new URL(CHECK_URLS[i]);
      } catch (MalformedURLException e) {}
    }
    
    return check;
  }
}