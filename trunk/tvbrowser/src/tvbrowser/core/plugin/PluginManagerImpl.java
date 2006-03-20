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
package tvbrowser.core.plugin;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import tvbrowser.core.ChannelList;
import tvbrowser.core.Settings;
import tvbrowser.core.TvDataBase;
import tvbrowser.core.contextmenu.ContextMenuManager;
import tvbrowser.core.filters.FilterList;
import tvbrowser.core.filters.SeparatorFilter;
import tvbrowser.core.icontheme.IconLoader;
import tvbrowser.core.search.booleansearch.BooleanSearcher;
import tvbrowser.core.search.booleansearch.ParserException;
import tvbrowser.core.search.regexsearch.RegexSearcher;
import tvbrowser.core.tvdataservice.TvDataServiceProxy;
import tvbrowser.core.tvdataservice.TvDataServiceProxyManager;
import tvbrowser.ui.mainframe.MainFrame;
import tvdataservice.MarkedProgramsList;
import tvdataservice.MutableProgram;
import tvdataservice.TvDataService;
import util.exc.TvBrowserException;
import devplugin.ActionMenu;
import devplugin.Channel;
import devplugin.ChannelDayProgram;
import devplugin.ContextMenuIf;
import devplugin.Date;
import devplugin.Plugin;
import devplugin.PluginAccess;
import devplugin.PluginManager;
import devplugin.Program;
import devplugin.ProgramFieldType;
import devplugin.ProgramFilter;
import devplugin.ProgramSearcher;
import devplugin.ThemeIcon;
import devplugin.TvBrowserSettings;

/**
 * The implementation of the PluginManager interface. This class is the
 * connection for the plugins to TV-Browser.
 *
 * @author Til Schneider, www.murfman.de
 */
public class PluginManagerImpl implements PluginManager {

  /** An example program. */
  private Program mExampleProgram;

   /** The logger for this class */
  private static java.util.logging.Logger mLog
    = Logger.getLogger(PluginManagerImpl.class.getName());

  private static PluginManagerImpl mInstance;

  /**
   * Creates a new instance of PluginManagerImpl.
   */
  private PluginManagerImpl() {
  }


  public static PluginManager getInstance() {
    if (mInstance == null) {
      mInstance = new PluginManagerImpl();
    }
    return mInstance;
  }

  /**
   * Gets a program.
   *
   * @param date The date when the program is shown.
   * @param progID The ID of the program.
   * @return The program or <code>null</code> if there is no such program.
   */
  public Program getProgram(Date date, String progID) {
    TvDataBase db = TvDataBase.getInstance();

  //  Channel[] channels = ChannelList.getSubscribedChannels();  /* 04-17-2005: changed from getAvailableChannels() to getSubscribedChannels */

    Channel ch = getChannelFromProgId(progID);
    if (ch != null) {
      ChannelDayProgram dayProg = db.getDayProgram(date, ch);
      if (dayProg != null) {
        Program prog = dayProg.getProgram(progID);
        if (prog != null) {
          return prog;
        }
        else {
          mLog.warning("could not find program with id '"+progID+"' (date: "+date+")");
        }
      }
      else {
        mLog.warning("day program not found: "+progID+"; "+date);
      }
    }else{
      mLog.warning("channel for program '"+progID+"' not found");
    }

    return null;
  }


  private Channel getChannelFromProgId(String progId) {
    String[] s = progId.split("_");
    return ChannelList.getChannel(s[0]);
  }

  /**
   * Gets all channels the user has subscribed.
   *
   * @return all channels the user has subscribed.
   */
  public Channel[] getSubscribedChannels() {
    return ChannelList.getSubscribedChannels();
  }


  /**
   * Gets an iterator through all programs of the specified channel at the
   * specified date.
   *
   * @param date The date of the programs.
   * @param channel The channel of the programs.
   * @return an Iterator for all programs of one day and channel or
   *         <code>null</code> if the requested data is not available.
   */
  public Iterator getChannelDayProgram(Date date, Channel channel) {
    ChannelDayProgram channelDayProgram = TvDataBase.getInstance()
        .getDayProgram(date, channel);
    if (channelDayProgram == null) {
      return null;
    }
    return channelDayProgram.getPrograms();
  }


  /**
   * Searches the TV data for programs which match a regular expression.
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
   *
   * @deprecated Use {@link #createProgramSearcher(int, String, boolean)}
   *             instead.
   */
  public Program[] search(String regex, boolean inTitle, boolean inText,
    boolean caseSensitive, Channel[] channels, devplugin.Date startDate,
    int nrDays) throws TvBrowserException
  {
    ProgramFieldType[] fieldArr;
    if (inTitle && inText) {
      fieldArr = new ProgramFieldType[] {
        ProgramFieldType.TITLE_TYPE,
        ProgramFieldType.SHORT_DESCRIPTION_TYPE,
        ProgramFieldType.DESCRIPTION_TYPE
      };
    }
    else if (inTitle) {
      fieldArr = new ProgramFieldType[] { ProgramFieldType.TITLE_TYPE };
    }
    else if (inText) {
      fieldArr = new ProgramFieldType[] {
        ProgramFieldType.SHORT_DESCRIPTION_TYPE,
        ProgramFieldType.DESCRIPTION_TYPE
      };
    }
    else {
      fieldArr = new ProgramFieldType[0];
    }

    return search(regex, caseSensitive, fieldArr, startDate, nrDays, channels, false);
  }


  /**
   * Searches the TV data base for programs that match a regular expression.
   *
   * @param regex The regular expression programs must match to.
   * @param caseSensitive Should the search be case sensitive?
   * @param fieldArr The fields to search in
   * @param startDate The date to start the search.
   * @param nrDays The number of days to include after the start date. If
   *        negative the days before the start date are used.
   * @param channels The channels to search in.
   * @param sortByStartTime Should the results be sorted by the start time?
   *        If not, the results will be grouped by date and channel and the
   *        search will be faster.
   * @return The matching programs.
   * @throws TvBrowserException
   * @throws TvBrowserException If there is a syntax error in the regular expression.
   *
   * @deprecated Since 1.1. Use {@link #createProgramSearcher(int, String, boolean)}
   *             instead.
   */
  public Program[] search(String regex, boolean caseSensitive,
    ProgramFieldType[] fieldArr, devplugin.Date startDate, int nrDays,
    Channel[] channels, boolean sortByStartTime) throws TvBrowserException
  {
    ProgramSearcher searcher = createProgramSearcher(SEARCHER_TYPE_REGULAR_EXPRESSION,
        regex, caseSensitive);

    return searcher.search(fieldArr, startDate, nrDays, channels, sortByStartTime);
  }


  /**
   * Creates a ProgramSearcher.
   *
   * @param type The searcher type to create. Must be one of
   *        {@link #SEARCHER_TYPE_EXACTLY}, {@link #SEARCHER_TYPE_KEYWORD},
   *        {@link #SEARCHER_TYPE_REGULAR_EXPRESSION} or
   *        {@link #SEARCHER_TYPE_BOOLEAN}.
   * @param searchTerm The search term the searcher should look for.
   * @param caseSensitive Specifies whether the searcher should be case sensitive.
   * @return A program searcher.
   * @throws TvBrowserException If creating the program searcher failed.
   */
  public ProgramSearcher createProgramSearcher(int type, String searchTerm,
      boolean caseSensitive)
      throws TvBrowserException
  {
    switch(type) {
      case SEARCHER_TYPE_EXACTLY: {
        String regex = RegexSearcher.searchTextToRegex(searchTerm, false);
        return new RegexSearcher(regex, caseSensitive);
      }
      case SEARCHER_TYPE_KEYWORD: {
        String regex = RegexSearcher.searchTextToRegex(searchTerm, true);
        return new RegexSearcher(regex, caseSensitive);
      }
      case SEARCHER_TYPE_REGULAR_EXPRESSION:
        return new RegexSearcher(searchTerm, caseSensitive);
      case SEARCHER_TYPE_BOOLEAN:
        try {
          return new BooleanSearcher(searchTerm, caseSensitive);
        }catch (ParserException e) {
          throw new TvBrowserException(PluginManagerImpl.class, "parser.error","Invalid input: {0}", e.getLocalizedMessage());          
        }
      default: throw new IllegalArgumentException("Unknown searcher type: " + type);
    }
  }


  /**
   * Returns all activated Plugins.
   *
   * @return all activated Plugins.
   * @since 1.1
   */
  public PluginAccess[] getActivatedPlugins() {
    return PluginProxyManager.getInstance().getActivatedPlugins();
  }


  /**
   * Gets the ID of the given Java plugin.
   *
   * @param javaPlugin The Java plugin to get the ID for.
   * @return The ID of the given Java plugin.
   */
  public String getJavaPluginId(Plugin javaPlugin) {
    return JavaPluginProxy.getJavaPluginId(javaPlugin);
  }


  /**
   * Gets the activated plugin with the given ID.
   *
   * @param pluginId The ID of the wanted plugin.
   * @return The plugin with the given ID or <code>null</code> if no such plugin
   *         exists or if the plugin is not activated.
   */
  public PluginAccess getActivatedPluginForId(String pluginId) {
    return PluginProxyManager.getInstance().getActivatedPluginForId(pluginId);
  }


  /**
   * Returns a list of all installed Plugins.
   * <p>
   * This method always returns an empty array! Use
   * {@link #getActivatedPlugins()} instead!
   *
   * @return An empty array!
   *
   * @deprecated Since 1.1. Use {@link #getActivatedPlugins()} instead.
   */
  public Plugin[] getInstalledPlugins() {
    return new Plugin[0];
  }


  /**
   * Gets a TvDataService for a class name.
   *
   * @deprecated
   *
   * @param dataServiceClassName the class name of the wanted TvDataService.
   * @return The TvDataService or <code>null</code> if there is no such
   *         TvDataService.
   */
  public TvDataService getDataService(String dataServiceClassName) {
  //  return TvDataServiceManager.getInstance().getDataService(dataServiceClassName);
    return null; // todo: find a smarter implementation
  }

  public TvDataServiceProxy getDataServiceProxy(String id) {
    return TvDataServiceProxyManager.getInstance().findDataServiceById(id);
  }

  /**
   * Creates a context menu for the given program containing all plugins.
   *
   * @param program The program to create the context menu for
   * @param caller The calling plugin.
   * @return a context menu for the given program.
   */
  public JPopupMenu createPluginContextMenu(Program program, Plugin caller) {

    return PluginProxyManager.createPluginContextMenu(program, caller);
  }


  /**
   * Returns an array of all available filters.
   *
   * @return An array of all available filters.
   * @since 0.9.7.4
   */
  public ProgramFilter[] getAvailableFilters() {

    ArrayList filters = new ArrayList();

    FilterList filterList = FilterList.getInstance();

    ProgramFilter[] filter = filterList.getFilterArr();

    for (int i=0;i<filter.length;i++) {
      if (!(filter[i] instanceof SeparatorFilter)) {
        filters.add(filter[i]);
      }
    }

    return (ProgramFilter[]) filters.toArray(new ProgramFilter[0]);
  }


  /**
   * Returns an example program. You can use it for preview stuff.
   *
   * @return an example program.
   * @since 0.9.7.4
   */
  public Program getExampleProgram() {
    if (mExampleProgram == null) {
      // TODO: interationalize

      Channel exampleChannel = new Channel(null, "Channel 1",
          TimeZone.getDefault(), "de", "");

      MutableProgram prog = new MutableProgram(exampleChannel,
                                               Date.getCurrentDate(), 14, 45);
      prog.setTitle("Die Waltons");
      prog.setShortInfo("Die Verfilmung der Kindheits- und Jugenderinnerungen des Romanschriftstellers Earl Hamner jr.");
      prog.setDescription("Olivia ist schon seit einigen Tagen niedergeschlagen, obwohl ihr Geburtstag bevorsteht. Ihre einzige Freude scheint das Postflugzeug zu sein, dem sie allabendlich von der Haust\u00FCr aus sehnsuchtsvoll hinterhersieht.");
      prog.setTextField(ProgramFieldType.SHOWVIEW_NR_TYPE, "123-456");
      prog.setTextField(ProgramFieldType.ACTOR_LIST_TYPE,
                        "Ralph Waite (Vater John Walton), Mary McDonough (Erin Walton), Michael Learned (Mutter Olivia Walton), Kami Cotler (Elisabeth Walton), Jon Walmsley (Jason Walton), Ellen Corby (Gro\u00dfmutter Ester Walton), David Harper (Jim Bob Walton), Judy Taylor (Mary Ellen Walton), Richard Thomas (John-Boy Walton)");
      prog.setIntField(ProgramFieldType.AGE_LIMIT_TYPE, 6);
      prog.setTextField(ProgramFieldType.EPISODE_TYPE, "Der Postflieger");
      prog.setTextField(ProgramFieldType.GENRE_TYPE, "Familie");
      prog.setTextField(ProgramFieldType.ORIGINAL_EPISODE_TYPE, "Air Mail Man");
      prog.setTextField(ProgramFieldType.ORIGINAL_TITLE_TYPE, "The Waltons");
      prog.setTextField(ProgramFieldType.ORIGIN_TYPE, "USA");
      prog.setIntField(ProgramFieldType.PRODUCTION_YEAR_TYPE, 1972);
      prog.setTextField(ProgramFieldType.REPETITION_OF_TYPE, "Wh von gestern, 8:00");
      //prog.setTextField(ProgramFieldType.SCRIPT_TYPE,"");
      prog.setIntField(ProgramFieldType.NET_PLAYING_TIME_TYPE, 45);
      prog.setTimeField(ProgramFieldType.END_TIME_TYPE, 15 * 60 + 45);
      prog.setTextField(ProgramFieldType.URL_TYPE, "http://www.thewaltons.com");
      prog.setTimeField(ProgramFieldType.VPS_TYPE, 14 * 60 + 45);
      prog.setInfo(Program.INFO_AUDIO_TWO_CHANNEL_TONE
                   | Program.INFO_SUBTITLE_FOR_AURALLY_HANDICAPPED);

      mExampleProgram = prog;
    }

    return mExampleProgram;
  }

  /**
   * Handles a double click on a program.
   * <p>
   * Executes the default context menu plugin. Plugins should use
   * handleProgramDoubleClick(Program program, Plugin caller). It prevetns the
   * Plugin to be activated a second time.
   *
   * @param program The program to pass to the default context menu plugin.
   *
   * @since 1.1
   */
  public void handleProgramDoubleClick(Program program) {
    handleProgramDoubleClick(program, null);
  }

  /**
   * Handles a double click on a program.
   * <p>
   * Executes the default context menu plugin.
   *
   * @param program The program to pass to the default context menu plugin.
   * @param caller Plugin that calls this. Prevents the Plugin to be activated twice
   *
   * @since 1.1
   */
  public void handleProgramDoubleClick(Program program, Plugin caller) {
    if (program == null) {
      // Nothing to do
      return;
    }
    
    ContextMenuIf defaultContextMenuIf = 
      ContextMenuManager.getInstance().getDefaultContextMenuIf();

    if (defaultContextMenuIf == null) {
      return;
    }

    if ((caller != null)  && (defaultContextMenuIf.getId().equals(caller.getId()))) {
      return;
    }


    ActionMenu menu = defaultContextMenuIf.getContextMenuActions(program);
    while (menu != null && menu.hasSubItems()) {
      ActionMenu[] subItems = menu.getSubItems();
      if (subItems.length>0) {
        menu = subItems[0];
      }
      else {
        menu = null;
      }
    }
    if (menu == null) {
      return;
    }

    Action action = menu.getAction();

    if (action != null) {
      ActionEvent evt = new ActionEvent(program, 0, (String)action.
          getValue(Action.ACTION_COMMAND_KEY));
      action.actionPerformed(evt);
    }


  }


  /**
   * Handles a middle click on a program.
   * <p>
   * Executes the middle click context menu plugin. Plugins should use
   * handleProgramMiddleClick(Program program, Plugin caller). It prevents the
   * Plugin to be activated a second time.
   *
   * @param program The program to pass to the middle click context menu plugin.
   *
   * @since 1.1
   */
  public void handleProgramMiddleClick(Program program) {
    handleProgramMiddleClick(program, null);
  }


  /**
   * Handles a middle click on a program.
   * <p>
   * Executes the middle click context menu plugin.
   *
   * @param program The program to pass to the middle click context menu plugin.
   * @param caller Plugin that calls this. Prevents the Plugin to be activated twice.
   *
   * @since 1.1
   */
  public void handleProgramMiddleClick(Program program, Plugin caller) {
    if (program == null) {
      // Nothing to do
      return;
    }

    ContextMenuIf middleClickIf
      = ContextMenuManager.getInstance().getMiddleClickIf();

    if (middleClickIf == null) {
      return;
    }

    if ((caller != null)  && (middleClickIf.getId().equals(caller.getId()))) {
      return;
    }

    ActionMenu menu = middleClickIf.getContextMenuActions(program);
    while (menu != null && menu.hasSubItems()) {
      ActionMenu[] subItems = menu.getSubItems();
      if (subItems.length>0) {
        menu = subItems[0];
      }
      else {
        menu = null;
      }
    }
    if (menu == null) {
      return;
    }

    Action action = menu.getAction();

    if (action != null) {
      ActionEvent evt = new ActionEvent(program, 0, (String)action.
          getValue(Action.ACTION_COMMAND_KEY));
      action.actionPerformed(evt);
    }

  }


  /**
   * Gets the plugin that is used as default in the context menu.
   *
   * @return the default context menu plugin.
   * @since 1.1
   */
  public PluginAccess getDefaultContextMenuPlugin() {
    return null ;//PluginProxyManager.getInstance().getDefaultContextMenuPlugin();
  }


  /**
   * Returns some settings a plugin may need.
   *
   * @return Some settings a plugin may need.
   */
  public TvBrowserSettings getTvBrowserSettings() {
    return new TvBrowserSettings(){
      public String getTvBrowserUserHome() {
        return Settings.getUserDirectoryName();
      }

      public int[] getTimeButtonTimes() {
        return Settings.propTimeButtons.getIntArray();
      }

      public Date getLastDownloadDate() {
        return Settings.propLastDownloadDate.getDate();
      }
    };
  }

  /**
   * Returns an Icon from the Icon-Theme-System
   *  
   * If your Plugin has Icons that are not available as Icons within an Theme, you can add
   * your Icons into your Jar-File.
   * 
   * The Directory-Structure must be like this:
   * 
   * [PackageOfYourPlugin]/icons/[Size]x[Size]/[category]/[icon].png
   * 
   * Please try to use the FreeDesktop-Icon Naming Conventions
   * http://cvs.freedesktop.org/[*]checkout[*]/icon-theme/default-icon-theme/spec/icon-naming-spec.xml
   * (please remove the [ ])
   *  
   * @param plugin Plugin that wants to load an Icon
   * @param category Category of the Icon (Action, etc...) 
   * @param iconName Icon-Name without File-Extension
   * @param size Size of the Icon
   * @return Icon if found, null if not
   */
  public ImageIcon getIconFromTheme(Plugin plugin, String category, String iconName, int size) {
    return IconLoader.getInstance().getIconFromTheme(plugin, category, iconName, size);
  }


  /**
   * Returns an Icon from the Icon-Theme-System
   *  
   * If your Plugin has Icons that are not available as Icons within an Theme, you can add
   * your Icons into your Jar-File.
   * 
   * The Directory-Structure must be like this:
   * 
   * [PackageOfYourPlugin]/icons/[Size]x[Size]/[category]/[icon].png
   * 
   * Please try to use the FreeDesktop-Icon Naming Conventions
   * http://cvs.freedesktop.org/[*]checkout[*]/icon-theme/default-icon-theme/spec/icon-naming-spec.xml
   * (please remove the [ ])
   *  
   * @param plugin Plugin that wants to load an Icon
   * @param icon ThemeIcon that represents the Icon
   * @param size Size of the Icon
   * @return Icon if found, null if not
   */
  public ImageIcon getIconFromTheme(Plugin plugin, ThemeIcon icon) {
    return IconLoader.getInstance().getIconFromTheme(plugin, icon);
  }


  /**
   * Show the Settings-Dialog for a Plugin
   * 
   * @param plugin Use this Plugin
   * @since 2.2
   */
  public void showSettings(Plugin plugin) {
    MainFrame.getInstance().showSettingsDialog(plugin);
  }

  /**
   * Return all marked programs.
   * 
   * @return The marked programs
   * @since 2.2
   */
  public Program[] getMarkedPrograms() {
    return MarkedProgramsList.getInstance().getMarkedPrograms();
  }  
}