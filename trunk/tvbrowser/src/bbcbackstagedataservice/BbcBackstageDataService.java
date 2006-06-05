/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (martin_oat@yahoo.de)
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
 *     $Date: 2006-06-03 00:23:19 +0200 (Sa, 03 Jun 2006) $
 *   $Author: ds10 $
 * $Revision: 2452 $
 */
package bbcbackstagedataservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Properties;
import java.util.TimeZone;

import org.apache.commons.compress.tar.TarEntry;
import org.apache.commons.compress.tar.TarInputStream;

import tvdataservice.MutableChannelDayProgram;
import tvdataservice.SettingsPanel;
import tvdataservice.TvDataUpdateManager;
import util.exc.TvBrowserException;
import util.io.IOUtilities;
import bbc.rd.tvanytime.TVAnytimeException;
import bbc.rd.tvanytime.serviceInformation.ServiceInformation;
import bbc.rd.tvanytime.xml.NonFatalXMLException;
import bbc.rd.tvanytime.xml.SAXXMLParser;
import devplugin.AbstractTvDataService;
import devplugin.Channel;
import devplugin.ChannelGroup;
import devplugin.Date;
import devplugin.PluginInfo;
import devplugin.ProgressMonitor;
import devplugin.Version;

/**
 * This Dataservice collects Data from http://backstage.bbc.co.uk/feeds/tvradio/
 *
 * @author bodum
 */
public class BbcBackstageDataService extends AbstractTvDataService {
  /** Base-URL */
  private static final String BASEURL = "http://backstage.bbc.co.uk/feeds/tvradio/";

  /**
   * Logger
   */
  private static java.util.logging.Logger mLog = java.util.logging.Logger.getLogger(BbcBackstageDataService.class
      .getName());

  /**
   * Channelgroup
   */
  private ChannelGroup mBbcChannelGroup = new BbcChannelGroup("BBC Backstage", "bbcbackstage",
      "Data from BBC Backstage", "BBC Backstage");

  /**
   * List of Channels
   */
  private ArrayList<Channel> mChannels = new ArrayList<Channel>();

  /**
   * Working-Directory
   */
  private File mWorkingDir;

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#checkForAvailableChannelGroups(devplugin.ProgressMonitor)
   */
  public ChannelGroup[] checkForAvailableChannelGroups(ProgressMonitor monitor) throws TvBrowserException {
    return new ChannelGroup[] { mBbcChannelGroup };
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#checkForAvailableChannels(devplugin.ChannelGroup, devplugin.ProgressMonitor)
   */
  public Channel[] checkForAvailableChannels(ChannelGroup group, ProgressMonitor monitor) throws TvBrowserException {
    try {
      ArrayList<Channel> channels = new ArrayList<Channel>();

      monitor.setMessage("Loading BBC Data");
      
      loadBBCData();
      
      monitor.setMessage("Parsing BBC Data");
      // Create parser
      SAXXMLParser parser = new SAXXMLParser();
      // Configure the parser to parse the standard profile (ie. everything).
      ((SAXXMLParser) parser).setParseProfile(SAXXMLParser.STANDARD);

      try {
        // Do the parsing...
        parser.parse(new File(mWorkingDir, "ServiceInformation.xml"));
      } catch (NonFatalXMLException nfxe) {
        // Handle non-fatal XML exceptions
        // Contain any invalid TVAnytime data values from XML source.
        // These are all collated by the parser and thrown at the end to avoid
        // having to abort the parsing.
        nfxe.printStackTrace();
      }

      monitor.setMessage("Storing BBC Data");
      
      int max = parser.getServiceInformationTable().getNumServiceInformations();
      for (int i = 0; i < max; i++) {
        ServiceInformation serviceInfo = parser.getServiceInformationTable().getServiceInformation(i);

        Channel ch = new Channel(this, serviceInfo.getName(), serviceInfo.getServiceID(), TimeZone
            .getTimeZone("GMT"), "gb", "(c) BBC", "http://bbc.co.uk", mBbcChannelGroup);
        channels.add(ch);
        mLog.fine("Channel : " + ch.getName() + "{" + ch.getId() + "}");
      }

      mChannels = channels;

      monitor.setMessage("Done BBC Data");

      return (Channel[]) channels.toArray(new Channel[channels.size()]);
    } catch (TVAnytimeException tvae) {
      // Handle any other TVAnytime-specific exceptions that may be generated.
      // E.g. if the XML parser cannot be initialised.
      tvae.printStackTrace();
    } catch (IOException ioe) {
      // Handle IOExceptions: things like missing file
      ioe.printStackTrace();
    }

    return null;
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#getAvailableChannels(devplugin.ChannelGroup)
   */
  public Channel[] getAvailableChannels(ChannelGroup group) {
    return (Channel[]) mChannels.toArray(new Channel[mChannels.size()]);
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#getAvailableGroups()
   */
  public ChannelGroup[] getAvailableGroups() {
    return new ChannelGroup[] { mBbcChannelGroup };
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#getInfo()
   */
  public PluginInfo getInfo() {
    return new PluginInfo("BBC Data", "BBC Data from BBC Backstage", "Bodo Tasche", new Version(0, 1));
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#getSettingsPanel()
   */
  public SettingsPanel getSettingsPanel() {
    return null;
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#hasSettingsPanel()
   */
  public boolean hasSettingsPanel() {
    return false;
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#loadSettings(java.util.Properties)
   */
  public void loadSettings(Properties settings) {
    mLog.info("Loading settings in BbcBackstageDataService");
    
    int numChannels = Integer.parseInt(settings.getProperty("NumberOfChannels", "0"));
    
    mChannels = new ArrayList<Channel>();
    
    for (int i=0;i<numChannels;i++){
      Channel ch = new Channel(this, settings.getProperty("ChannelTitle-"+i, ""), settings.getProperty("ChannelId-"+i, ""), TimeZone
          .getTimeZone("GMT+0:00"), "gb", "(c) BBC", "http://bbc.co.uk", mBbcChannelGroup);
      mChannels.add(ch);
      mLog.fine("Channel : " + ch.getName() + "{" + ch.getId() + "}");
    }

    mLog.info("Finnished loading settings for BbcBackstageDataService");
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#storeSettings()
   */
  public Properties storeSettings() {
    mLog.info("Storing settings for BbcBackstageDataService");

    Properties prop = new Properties();

    prop.setProperty("NumberOfChannels", Integer.toString(mChannels.size()));
    int max = mChannels.size();
    for (int i = 0; i < max; i++) {
      Channel ch = mChannels.get(i);
      prop.setProperty("ChannelId-" + i, ch.getId());
      prop.setProperty("ChannelTitle-" + i, ch.getName());
    }
    mLog.info("Finnished storing settings for BbcBackstageDataService");

    return prop;
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#setWorkingDirectory(java.io.File)
   */
  public void setWorkingDirectory(File dataDir) {
    mWorkingDir = dataDir;
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#supportsDynamicChannelGroups()
   */
  public boolean supportsDynamicChannelGroups() {
    return false;
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#supportsDynamicChannelList()
   */
  public boolean supportsDynamicChannelList() {
    return true;
  }

  /*
   * (non-Javadoc)
   * @see devplugin.TvDataService#updateTvData(tvdataservice.TvDataUpdateManager, devplugin.Channel[], devplugin.Date, int, devplugin.ProgressMonitor)
   */
  public void updateTvData(TvDataUpdateManager updateManager, Channel[] channelArr, Date startDate, int dateCount,
      ProgressMonitor monitor) throws TvBrowserException {

    int max = channelArr.length;
    
    monitor.setMessage("Loading BBC-Data");
    monitor.setMaximum(3+dateCount);

    mLog.info(mWorkingDir.getAbsolutePath());
    
    loadBBCData();
    
    monitor.setMessage("Parsing BBC Data");
    monitor.setValue(3);
    for (int i=0;i<dateCount;i++) {
      monitor.setValue(3+i);
      StringBuilder date = new StringBuilder();
      date.append(startDate.getYear());
      date.append(addZero(startDate.getMonth()));
      date.append(addZero(startDate.getDayOfMonth()));
      
      for (int v=0;v<max;v++) {
        Date channeldate = startDate;
        StringBuilder filename = new StringBuilder(date);
        filename.append(channelArr[v].getId());
        mLog.info(filename.toString());
        
        try {
          MutableChannelDayProgram mutablechanneldayprogram = new MutableChannelDayProgram(channeldate, channelArr[v]);

          BbcFileParser bbcparser = new BbcFileParser(mutablechanneldayprogram, channeldate);
          
          bbcparser.parseFile(new File(mWorkingDir, filename.toString()));
          
          if ((updateManager != null) && mutablechanneldayprogram.getProgramCount() > 0) {
            updateManager.updateDayProgram(mutablechanneldayprogram);
          }

        } catch (Exception e) {
          e.printStackTrace();
        }
        
      }
      
      startDate = startDate.addDays(1);
    }
    
  }

  /**
   * Download .tar.gz and extract it into the working directory
   */
  private void loadBBCData() {
    cleanWorkingDir();

    try {
      Date date = new Date();
      
      InputStream in = null;
      
      int count = 0;
      do {
        try {
          URL url = new URL(BASEURL + date.getYear() + addZero(date.getMonth()) + addZero(date.getDayOfMonth()) + ".tar.gz");
          in = url.openStream();
          date.addDays(-1);
          count++;
        } catch (Exception e) {
          in = null;
        }
        
      } while ((in == null) || count > 4);
      
      
      File download = new File(mWorkingDir, "file.tar.gz");
      OutputStream out = new FileOutputStream(download);
  
      // Transfer bytes from in to out
      byte[] buf = new byte[1024];
      int len;
      while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
      }
      in.close();
      out.close();
      
      File tar = new File(mWorkingDir, "file.tar");
      IOUtilities.ungzip(download, tar);
      download.delete();
      
      TarInputStream tarfile = new TarInputStream(new FileInputStream(tar));
      TarEntry entry;
      while ((entry = tarfile.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          String filename = entry.getName();
          filename = filename.substring(filename.lastIndexOf('/')+1);

          File tardown = new File(mWorkingDir, filename);
          OutputStream tarout = new FileOutputStream(tardown);
      
          // Transfer bytes from in to out
          while ((len = tarfile.read(buf)) > 0) {
              tarout.write(buf, 0, len);
          }
          tarout.close();
        }
      }
      tarfile.close();
      tar.delete();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    
  }

  /**
   * Clean working directory
   */
  private void cleanWorkingDir() {
    File[] files = mWorkingDir.listFiles();
    
    int max = files.length;
    
    for (int i=0;i<max;i++) {
      files[i].delete();
    }
  }

  /**
   * Add one zero if neccessary
   * @param number
   * @return
   */
  private CharSequence addZero(int number) {
    StringBuilder builder = new StringBuilder();
    
    if (number < 10) {
      builder.append('0');
    }
    
    builder.append(Integer.toString(number));
    return builder.toString();
  }

}