/*
 * TV-Browser
 * Copyright (C) 04-2003 Martin Oberhauser (darras@users.sourceforge.net)
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

package tvbrowser.core;

import java.io.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import java.util.zip.*;
import java.util.regex.*;
import java.util.logging.Level;

import javax.swing.JProgressBar;

import util.exc.*;
import util.io.IOUtilities;

import devplugin.Channel;
import devplugin.ChannelDayProgram;
import devplugin.Date;
import devplugin.Program;

import tvdataservice.TvDataService;
import tvdataservice.MutableChannelDayProgram;

/**
 * The DataService is a singletons that manages the TV data.
 *
 * @author Martin Oberhauser
 */
public class DataService implements devplugin.PluginManager {

  /** The logger for this class. */
  private static java.util.logging.Logger mLog
    = java.util.logging.Logger.getLogger(DataService.class.getName());

  /** The localizer for this class. */
  private static final util.ui.Localizer mLocalizer
    = util.ui.Localizer.getLocalizerFor(DataService.class);

  /** The singleton. */
  private static DataService mSingleton;

  /** <CODE>true</CODE> if we are in online mode. */
  private boolean onlineMode=false;

  /** The progress bar to use for showing the update process. */
  private JProgressBar progressBar;

  /** <CODE>True</CODE> if we are currently downloading TV data. */
  private boolean mIsDownloading = false;

  /** Holds for a Date (key) a DayProgram (value). Used as cache. */
  private HashMap mDayProgramHash;



  /**
   * Creates a new instance of DataService.
   */
  private DataService() {
    mDayProgramHash = new HashMap();

    progressBar = new JProgressBar();
    progressBar.setStringPainted(false);

    //loadTvDataService();
  }



  /**
   * Gets the DataService singleton.
   *
   * @return the DataService singleton.
   */
  public static DataService getInstance() {
    if (mSingleton == null) {
      mSingleton = new DataService();
    }
    return mSingleton;
  }



  /**
   * Sets whether we are in online mode.
   *
   * @param newMode whether we are in online mode.
   */
  public void setOnlineMode(boolean newMode) {
    if ((newMode == onlineMode)) {
      return;
    }

    onlineMode = newMode;

    if (newMode) {
      TvDataServiceManager.getInstance().connect();
    } else {
      TvDataServiceManager.getInstance().disconnect();
    }
  }



  /**
   * Gets whether we are in online mode.
   *
   * @return whether we are in online mode.
   */
  public boolean isOnlineMode() {
    return onlineMode;
  }

	public void stopDownload() {
		mIsDownloading=false;
	}

  /**
   * Starts the download of new TV data
   *
   * @param daysToDownload The number of days until today to download the
   *        program for.
   */
  public void startDownload(int daysToDownload) {
    File tvdataDir = new File(Settings.getTVDataDirectory());
    if (!tvdataDir.exists()) {
      tvdataDir.mkdir();
    }

    // connect
    if (!onlineMode) {
    	progressBar.setString(mLocalizer.msg("connecting", "Connecting..."));
    	progressBar.setStringPainted(true);
    	TvDataServiceManager.getInstance().connect();
    }
    
    // download the missing data day by day and channel by channel
    progressBar.setStringPainted(false);
    mIsDownloading = true;

    Channel[] subscribedChannels=ChannelList.getSubscribedChannels();
    progressBar.setMaximum((daysToDownload + 2) * subscribedChannels.length);
    devplugin.Date date=new Date();
    date.addDays(-1); // get yesterday too
    TvBrowserException downloadException = null;
    for (int i = 0; i < daysToDownload + 2; i++) {
      DayProgram dayProgram = (DayProgram) mDayProgramHash.get(date);
      if (dayProgram == null) {
        dayProgram = new DayProgram(date);
      }

      for (int j = 0; (j < subscribedChannels.length) && mIsDownloading; j++) {
        progressBar.setValue(i * subscribedChannels.length + j + 1);

        // Check whether we already have the wanted ChannelDayProgram
        devplugin.Channel channel = subscribedChannels[j];
        File file = getChannelDayProgramFile(date, channel);
        if (! file.exists()) {
          // We don't have -> download it
          try {
            ChannelDayProgram prog = downloadDayProgram(date, channel);

            if (prog != null) {
              dayProgram.addChannelDayProgram(prog);
            }
          }
          catch (Throwable thr) {
            TvBrowserException tvExc;
            if (thr instanceof TvBrowserException) {
              tvExc = (TvBrowserException) thr;
            } else {
              tvExc = new TvBrowserException(getClass(), "error.5",
                "Download of dayprogram failed!", thr);
            }
            
            // Remeber only the first exception, log it if there are more exception
            if (downloadException == null) {
              downloadException = tvExc;
            } else {
              mLog.log(Level.WARNING, "Download of dayprogram failed!", tvExc);
            }
          }
        }
      }

      if (! dayProgram.isEmpty()) {
        mDayProgramHash.put(date, dayProgram);
      }

      // Create a new Date object, because the other one is used as key
      // in mDayProgramHash.
      date = new devplugin.Date(date.getDaysSince1970() + 1);
    }

    mIsDownloading = false;

	if (!onlineMode) {
    	TvDataServiceManager.getInstance().disconnect();
	}

    if (downloadException != null) {
      String msg = mLocalizer.msg("error.7", "Couldn't download the whole program!");
      ErrorHandler.handle(msg, downloadException);
    }

    // Let the plugins react on the new data
    PluginManager.fireTvDataChanged();
  }



  /**
   * Gets the day program for the specified date.
   *
   * @param date The date to get the day program for.
   * @return the day program for the specified date.
   */
  public DayProgram getDayProgram(devplugin.Date date) {
    // if date is null throw a NullPointerException
    if (date == null) {
      throw new NullPointerException("date is null!");
    }

    // try to get the DayProgram from the cache.
    DayProgram dayProgram = (DayProgram) mDayProgramHash.get(date);

    if (dayProgram == null) {
      try {
        // The program is not in the cache -> try to load it
        dayProgram = loadDayProgram(date);
        // mLog.info("Loading program for " + date + " (" + date.hashCode() + ") "
        //   + ((dayProgram == null) ? "failed" : "suceed"));
      }
      catch (TvBrowserException exc) {
        ErrorHandler.handle(exc);
      }
    }

    return dayProgram;
  }



  /**
   * Loads the day program for the specified date. If the program could not be
   * found on the disk, it will be downloaded if we are in online mode or if
   * there is currently an update running.
   *
   * @param date The date to download the program for.
   * @throws TvBrowserException If the download failed.
   * @return The DayProgram for the specified day.
   */
  protected DayProgram loadDayProgram(devplugin.Date date)
    throws TvBrowserException
  {
 
    Channel[] channels=ChannelList.getSubscribedChannels();

    boolean useProgressBar=false;
    if (isOnlineMode() && !dataAvailable(date)) {
      useProgressBar=true;
      progressBar.setMaximum(channels.length);
    }

    // Get the day program for the specified date from the cache
    DayProgram dayProgram = (DayProgram) mDayProgramHash.get(date);
    if (dayProgram == null) {
      dayProgram = new DayProgram(date);
    }

    // Load or download all missing channel day programs
    boolean someDataWasDownloaded = false;
    for (int i = 0; i < channels.length; i++) {
      // Update the progress bar
      TvDataService dataService = channels[i].getDataService();
      if (useProgressBar) {
        progressBar.setValue(i+1);
      }

      // Check whether this channel day program is already present
      if (dayProgram.getChannelDayProgram(channels[i]) != null) {
        continue;
      }

      // Check whether we have it on disk
      File file = getChannelDayProgramFile(date, channels[i]);
      if (file.exists()) {
        // We have it on disk -> load it
        ObjectInputStream in = null;
        try {
          in = new ObjectInputStream(new FileInputStream(file));
          ChannelDayProgram prog = new MutableChannelDayProgram(in);
          dayProgram.addChannelDayProgram(prog);
        }
        catch (Exception exc) {
          throw new TvBrowserException(getClass(), "error.1",
            "Error when reading program of {0} on {1}!\n({2})",
            channels[i].getName(), date, file.getAbsolutePath(), exc);
        }
        finally {
          if (in != null) {
            try { in.close(); } catch (IOException exc) {}
          }
        }
      }
      else if (isOnlineMode()) {
        // We don't have it on disk, but we are online -> download it
        ChannelDayProgram prog = downloadDayProgram(date, channels[i]);

        if (prog != null) {
          dayProgram.addChannelDayProgram(prog);
          someDataWasDownloaded = true;
        }
      }
    }

    // If the day program is not empty -> return it and put it in the cache
    if (! dayProgram.iterator().hasNext()) {
      // day program is empty -> return null
      return null;
    } else {
      mDayProgramHash.put(date, dayProgram);

      // Let the plugins react on the new data
      if (someDataWasDownloaded) {
        PluginManager.fireTvDataChanged();
      }

      return dayProgram;
    }
  }



  /**
   * Returns true, if tv data is available on disk for the given date.
   *
   * @param date The date to check.
   * @return if the data is available.
   */
  public static boolean dataAvailable(devplugin.Date date) {
    final String dateStr = "" + date.getDaysSince1970();

    String fList[] = new File(Settings.getTVDataDirectory()).list(
      new java.io.FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(dateStr);
        }
      }
    );

    if (fList == null) return false;
    return fList.length > 0;
  }

/**
 * Deletes expired tvdata files older then lifespan days.
 * @param lifespan
 */

public static void deleteExpiredFiles(int lifespan) {
	
		if (lifespan<0) {
			return;  // manually
		}
		devplugin.Date d=new devplugin.Date();
		d.addDays(-lifespan);
		final int date=d.getDaysSince1970();
	
		File fList[]=new File(Settings.getTVDataDirectory()).listFiles(
			new java.io.FilenameFilter() {
				public boolean accept(File dir, String name) {
				
					int p=name.lastIndexOf('.');
					String s=name.substring(p+1,name.length());
					int val=Integer.parseInt(s);
					return val<date;
				}
			}	
		);
		if (fList!=null) {
			for (int i=0;i<fList.length;i++) {
				fList[i].delete();
			}
		}
	
	
}




  /**
   * Gets a program.
   * <p>
   * Returns <CODE>null</CODE>, if the specified program could not be found.
   *
   * @param date The date to get the data for.
   * @param progID The ID of the program to get the data for.
   * @return The program for the specified channel and date.
   */
  public devplugin.Program getProgram(devplugin.Date date, String progID) {
    DayProgram dayProgram = getDayProgram(date);

    if (dayProgram == null) {
      return null;
    } else {
      return dayProgram.getProgram(progID);
    }
  }



  /**
   * Gets the progress bar used to show the download progress.
   *
   * @return the progress bar used to show the download progress.
   */
  public JProgressBar getProgressBar() {
    return progressBar;
  }



  /**
   * Gets whether the data service is currently downloading data.
   *
   * @return <CODE>true</CODE>, if the data service is currently downloading data.
   */
  public boolean isDownloading() {
    return mIsDownloading;
  }



  /**
   * Gets the subscribed channels.
   *
   * @return the subscribed channels.
   */
  public devplugin.Channel[] getSubscribedChannels() {
    return ChannelList.getSubscribedChannels();
  }



  /**
   * Called, when the ChannelList has changed.
   */
  public void subscribedChannelsChanged() {
    Iterator dayProgramIter = mDayProgramHash.values().iterator();
    while (dayProgramIter.hasNext()) {
      DayProgram dayProgram = (DayProgram) dayProgramIter.next();
      try {
        loadDayProgram(dayProgram.getDate());
      }
      catch (TvBrowserException exc) {
        ErrorHandler.handle(exc);
      }
    }
  }



  /**
   * Gets an iterator through all programs of the specified channel at the
   * specified date.
   * <p>
   * If the requested data is not available, null is returned.
   *
   * @param date The date to get the programs for.
   * @param channel The channel to get the programs for.
   * @return the programs of the specified channel and date.
   */
  public Iterator getChannelDayProgram(devplugin.Date date, devplugin.Channel channel) {
    DayProgram dayProgram = getDayProgram(date);
    if (dayProgram == null) {
      return null;
    }

    ChannelDayProgram channelDayProgram = dayProgram.getChannelDayProgram(channel);
    if (channelDayProgram == null) {
      return null;
    }

    return channelDayProgram.getPrograms();
  }



  /**
   * Imports previously exported tv data.
   * <p>
   * In fact the files that are not already present in the tvdata directory
   * are extracted from the zip file.
   *
   * @param srcFile The file to import the tv data from.
   * @throws TvBrowserException If the import failed.
   */
  public void importTvData(File srcFile) throws TvBrowserException {
    ZipFile zipFile = null;
    InputStream in = null;

    try {
      zipFile = new ZipFile(srcFile);
      Enumeration entryEnum = zipFile.entries();
      while (entryEnum.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entryEnum.nextElement();

        File tvdataFile = new File(Settings.getTVDataDirectory(), entry.getName());
        if (! tvdataFile.exists()) {
          in = zipFile.getInputStream(entry);
          IOUtilities.saveStream(in, tvdataFile);
          in.close();
        }
      }
    }
    catch (Exception exc) {
      throw new TvBrowserException(getClass(), "error.3",
        "Importing tv data failed!\n({0})", srcFile.getAbsolutePath(), exc);
    }
    finally {
      if (zipFile != null) {
        try { zipFile.close(); } catch (IOException exc) {}
      }
      if (in != null) {
        try { in.close(); } catch (IOException exc) {}
      }
    }

    // Let the plugins react on the new data
    PluginManager.fireTvDataChanged();
  }



  /**
   * Exports all known tv data to the specified file.
   * <p>
   * In fact the tvdata directory is packed into a zip file.
   *
   * @param targetFile The file to export the tv data to.
   * @throws TvBrowserException If the export failed.
   */
  public void exportTvData(File targetFile) throws TvBrowserException {
    File tvdataDir = new File(Settings.getTVDataDirectory());
    File[] children = tvdataDir.listFiles();

    // When the tvdata directory is empty -> do nothing
    if (children == null) {
      // No data available
      return;
    }

    FileOutputStream out = null;
    ZipOutputStream zipOut = null;
    FileInputStream in = null;

    try {
      out = new FileOutputStream(targetFile);
      zipOut = new ZipOutputStream(out);

      // Add all files in the tvdata directory to the zip file
      for (int i = 0; i < children.length; i++) {
        String fileName = children[i].getName();
        ZipEntry entry = new ZipEntry(fileName);

        zipOut.putNextEntry(entry);
        in = new FileInputStream(children[i]);
        IOUtilities.pipeStreams(in, zipOut);
        in.close();
      }
    }
    catch (Exception exc) {
      throw new TvBrowserException(getClass(), "error.4",
        "Exporting tv data failed!\n({0})", targetFile.getAbsolutePath(), exc);
    }
    finally {
      if (zipOut != null) {
        try { zipOut.close(); } catch (IOException exc) {}
      }
      if (out != null) {
        try { out.close(); } catch (IOException exc) {}
      }
      if (in != null) {
        try { in.close(); } catch (IOException exc) {}
      }
    }
  }



  /**
   * Searches the data for programs which match a regular expression.
   *
   * @param regex The regular expression programs must match to.
   * @param inTitle Should be searched in the title?
   * @param inText Should be searched in the desription?
   * @param caseSensitive Should the search be case sensitive?
   * @param channels The channels to search in.
   * @param startDate The date to start the search.
   * @param nrDays The number of days to include after the start date. If
   *        negative the days before the start date are used.
   * @throws TvBrowserException If there is a syntax error in the regular expression.
   * @return The matching programs.
   */
  public devplugin.Program[] search(String regex, boolean inTitle, boolean inText,
    boolean caseSensitive, devplugin.Channel[] channels,
    devplugin.Date startDate, int nrDays)
    throws TvBrowserException
  {
    int flags = 0;
    if (! caseSensitive) {
      flags |= Pattern.CASE_INSENSITIVE;
    }

    Pattern pattern;
    try {
      pattern = Pattern.compile(regex, flags);
    }
    catch (PatternSyntaxException exc) {
      throw new TvBrowserException(getClass(), "error.8",
        "Syntax error in the regualar expression of the search pattern!", exc);
    }

    if (nrDays < 0) {
      startDate.addDays(nrDays);
      nrDays = 0 - nrDays;
    }

    ArrayList hitList = new ArrayList();
    int missingDataCount = 0;
    for (int day = 0; day <= nrDays; day++) {
      for (int channelIdx = 0; channelIdx < channels.length; channelIdx++) {
        devplugin.Channel channel = channels[channelIdx];
        Iterator programIter = getChannelDayProgram(startDate, channel);
        if (programIter == null) {
          // Give up if we didn't get data for tenth time
          missingDataCount++;
          if (missingDataCount > 10) {
            // There is no more data -> stop
            day = nrDays;
          }
        } else {
          while (programIter.hasNext()) {
            Program prog = (Program) programIter.next();
            boolean matches = false;

            if (inTitle) {
              Matcher matcher = pattern.matcher(prog.getTitle());
              matches = matcher.matches();
            }
            if ((! matches) && inText) {
              Matcher matcher = pattern.matcher(prog.getDescription());
              matches = matcher.matches();
            }

            if (matches) {
              hitList.add(prog);
            }
          }
        }
      }

      // The next day
      startDate.addDays(1);
    }

    Program[] hitArr = new Program[hitList.size()];
    hitList.toArray(hitArr);

    return hitArr;
  }



  protected ChannelDayProgram downloadDayProgram(devplugin.Date date,
    devplugin.Channel channel)
    throws TvBrowserException
  {
    mLog.info("Downloading the TV data for " + channel.getName() + " on " + date);
    ChannelDayProgram prog;

    // download the program
    TvDataService dataService = channel.getDataService();
    if (dataService == null) {
      return null;
    }
    prog = dataService.downloadDayProgram(date, channel);

    // save the program to disk
    if (prog != null) {
      File file = getChannelDayProgramFile(date, channel);

      ObjectOutputStream out = null;
      try {
        out = new ObjectOutputStream(new FileOutputStream(file));
        MutableChannelDayProgram mutProg = (MutableChannelDayProgram) prog;
        mutProg.writeData(out);
      }
      catch (IOException exc) {
        String msg = mLocalizer.msg("error.2",
          "Error when saving program of {0} on {1}!\n({2})",
          channel.getName(), date, file.getAbsolutePath());
        ErrorHandler.handle(msg, exc);

        prog = null;
      }
      finally {
        if (out != null) {
          try { out.close(); } catch (IOException exc) {}
        }
      }
    }

    return prog;
  }

  public devplugin.Plugin[] getInstalledPlugins() {
  	return PluginManager.getInstalledPlugins();
  }



  private File getChannelDayProgramFile(devplugin.Date date,
    devplugin.Channel channel)
  {
    String fileName = "" + channel.getId()
      + "_" + channel.getDataService().getClass().getName()
      + "." + date.getDaysSince1970();
    return new File(Settings.getTVDataDirectory(), fileName);
  }



  public TvDataService getDataService(String className) {
    return TvDataServiceManager.getInstance().getDataService(className);
  }

}