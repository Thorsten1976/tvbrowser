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

package devplugin;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import tvdataservice.SettingsPanel;
import tvdataservice.TvDataUpdateManager;
import util.exc.TvBrowserException;
import util.tvdataservice.IconLoader;

/**
 * Superclass for all TvDataServices.
 * <p>
 * Extend this class to provide your own TvDataService.
 */
public abstract class AbstractTvDataService {
  /** The parent frame. May be used for dialogs. */
  private Frame mParentFrame;

  /** Contains the mirror urls usable for receiving the groups.txt from. */
  private static final String[] DEFAULT_CHANNEL_GROUPS_MIRRORS = {
    "http://tvbrowser.dyndns.tv",
    "http://www.gamers-fusion.de/projects/tvbrowser.org/",
    "http://tvbrowser1.sam-schwedler.de",
    "http://tvbrowser.nicht-langweilig.de/data"
  };

  /**
   * The plugin manager. It's the connection to TV-Browser.
   * <p>
   * Every communication between TV-Browser and the data server is either initiated
   * by TV-Browser or made by using the plugin manager.
   */
  private static PluginManager mPluginManager;

  final public Channel[] getAvailableChannels() {
    return getAvailableChannels(null);
  }

  final public Channel[] checkForAvailableChannels(ProgressMonitor monitor) throws TvBrowserException {
    return checkForAvailableChannels(null, monitor);
  }

  final public Version getAPIVersion() {
    return new Version(1,0);
  }

  /**
   * Use this method to call methods of the plugin manager.
   * <p>
   * The plugin manager is your connection to TV-Browser. Every communication
   * between TV-Browser and the data service is either initiated by TV-Browser or made
   * by using the plugin manager.
   *
   * @return The plugin manager.
   * @since 2.6
   */
  final public static PluginManager getPluginManager() {
    return mPluginManager;
  }

  /**
   * Called by the host-application to provide access to the plugin manager.
   *
   * @param manager The plugin manager the plugins should use.
   */
  final public static void setPluginManager(PluginManager manager) {
    if (mPluginManager == null ) {
      mPluginManager = manager;
    }
  }

  /**
   * Gets the version of this data service.
   *
   * @return The version of this data service.
   */
  public static Version getVersion() {
    return new Version(0,0);
  }


  /**
   * Called by the host-application to provide the parent frame.
   *
   * @param parent The parent frame.
   * @since 2.7
   */
  final public void setParent(Frame parent) {
    mParentFrame = parent;
  }


  /**
   * Gets the parent frame.
   * <p>
   * The parent frame may be used for showing dialogs.
   *
   * @return The parent frame.
   * @since 2.7
   */
  final public Frame getParentFrame() {
    return mParentFrame;
  }

  /**
   * This method is called when the TV-Browser start is complete.
   * @since 2.7
   */
  public void handleTvBrowserStartFinished() {
    // do nothing
  }

  /**
   * Gets if the data service supports auto update of data.
   * @return <code>True</code> if the data service supports the auto update,
   * <code>false</code> otherwise.
   * @since 2.7
   */
  public boolean supportsAutoUpdate() {
    return false;
  }

  /**
   * Gets the action menu with the action supported for toolbar actions.
   * @return The action menu with the supported toolbar actions
   */
  public ActionMenu getButtonAction() {
    return null;
  }

  /**
   * Gets the id of this ButtonActionIf.
   * @return The id of this ButtonActionIf.
   */
  final public String getId() {
    return this.getClass().getName();
  }

  /**
   * Gets the description for this ButtonActionIf.
   * @return The description for this ButtonActionIf.
   */
  public String getButtonActionDescription() {
    return getInfo().getDescription();
  }


  /**
   * Gets the actions for the context menu of a program.
   *
   * @param program The program the context menu will be shown for.
   * @return the actions this plugin provides for the given program or
   *         <code>null</code> if the plugin does not provide this feature.
   */
  public ActionMenu getContextMenuActions(Program program) {
    return null;
  }

  protected IconLoader getIconLoader(final String groupId, final File workingDirectory) throws IOException {
    return new IconLoader(this, groupId, workingDirectory);
  }

  /**
   * get the default mirrors to ask for channel groups
   * @return mirror url array
   * @since 3.0
   */
  protected String[] getDefaultMirrors() {
    return DEFAULT_CHANNEL_GROUPS_MIRRORS.clone();
  }

  /**
   * This method is called by the host application to set the working folder.
   * If required, TvDataService implementations should store their data
   * within this 'dataDir' directory
   * @param dataDir
   */
  public abstract void setWorkingDirectory(File dataDir);

  /**
   * @return an array of the available channel groups.

   */
  public abstract ChannelGroup[] getAvailableGroups();

  /**
   * Updates the TV listings provided by this data service.
   * @param updateManager
   * @param channelArr
   * @param startDate
   * @param dateCount
   * @param monitor
   *
   * @throws util.exc.TvBrowserException
   */
  public abstract void updateTvData(TvDataUpdateManager updateManager,
                           Channel[] channelArr, Date startDate, int dateCount, ProgressMonitor monitor)
    throws TvBrowserException;

  /**
   * Called by the host-application during start-up. Implement this method to
   * load your data service settings from the file system.
   * @param settings
   */
  public abstract void loadSettings(Properties settings);

  /**
   * Called by the host-application during shut-down. Implements this method to
   * store your data service settings to the file system.
   * @return properties that will afterwards be stored by the host application
   */
  public abstract Properties storeSettings();

  /**
   * A TvDataService can have a settings panel within the settings dialog.
   * If the hasSettingsPanel() method returns false, the {@link #getSettingsPanel()}
   * method is never called.
   * @return true, if the settings panel feature is used by this TvDataService
   */
  public abstract boolean hasSettingsPanel();

  /**
   *
   * @return the SettingsPanel of this TvDataService
   */
  public abstract SettingsPanel getSettingsPanel();

  /**
   * Gets the list of the channels that are available for the given channel group.
   */
  public abstract Channel[] getAvailableChannels(ChannelGroup group);

  /**
   * Some TvDataServices may need to connect to the Internet to know their
   * channels. If {@link #supportsDynamicChannelList()} returns true, this method is
   * called to check for available channels.
   * @param group
   * @param monitor
   * @return array of all available channels (new and old)
   * @throws TvBrowserException
   */
  public abstract Channel[] checkForAvailableChannels(ChannelGroup group, ProgressMonitor monitor) throws TvBrowserException;

  public abstract ChannelGroup[] checkForAvailableChannelGroups(ProgressMonitor monitor) throws TvBrowserException;

  /**
   *
   * @return true, if this TvDataService can dynamically load other channels
   */
  public abstract boolean supportsDynamicChannelList();

  /**
   *
   * @return true, if this TvDataService can dynamically load other groups
   */
  public abstract boolean supportsDynamicChannelGroups();

  /**
   * Gets information about this TvDataService
   */
  public abstract PluginInfo getInfo();

  public boolean hasRightToDownloadIcons() {
    return false;
  }
  
  /**
   * Gets the category of this plugin.
   * <p>
   * The category can be one of this values.
   * Note: Don't use the NO_CATEGORY it's only for backward compatibility.
   * <ul>
   * <li>{@link #ALL_CATEGORY}</li>
   * <li>{@link #REMOTE_CONTROL_SOFTWARE_CATEGORY}</li>
   * <li>{@link #REMOTE_CONTROL_HARDWARE_CATEGORY}</li>
   * <li>{@link #ADDITONAL_DATA_SERVICE_SOFTWARE_CATEGORY}</li>
   * <li>{@link #ADDITONAL_DATA_SERVICE_HARDWARE_CATEGORY}</li>
   * <li>{@link #RATINGS_CATEGORY}</li>
   * <li>{@link #OTHER_CATEGORY}</li>
   * </ul>
   * <p>
   * @return The category of this plugin.
   * @since 3.0.2
   */
  public String getPluginCategory() {
    return Plugin.ALL_CATEGORY;
  }
    
  /**
   * If the download of the TV data needs authentication create
   * a panel that contains information about the authentification
   * and the authentication form.
   * <p>
   * @return The panel with the authentication settings.
   * @since 3.0.2
   */
  public SettingsPanel getAuthenticationPanel() {
    return null;
  }
}
