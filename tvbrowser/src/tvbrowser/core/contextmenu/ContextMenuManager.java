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
 *
 */

package tvbrowser.core.contextmenu;

//import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
//import javax.swing.MenuElement;
import javax.swing.SwingUtilities;

import tvbrowser.core.Settings;
import tvbrowser.core.plugin.PluginProxy;
import tvbrowser.core.plugin.PluginProxyManager;
import tvbrowser.core.tvdataservice.TvDataServiceProxy;
import tvbrowser.core.tvdataservice.TvDataServiceProxyManager;
import tvbrowser.extras.common.InternalPluginProxyIf;
import tvbrowser.extras.common.InternalPluginProxyList;
import tvbrowser.extras.searchplugin.SearchPluginProxy;
import tvbrowser.ui.mainframe.MainFrame;
import util.settings.ContextMenuMouseActionSetting;
//import util.settings.StringProperty;
import util.ui.Localizer;
import util.ui.TVBrowserIcons;
import util.ui.menu.MenuUtil;
import devplugin.ActionMenu;
import devplugin.ContextMenuIf;
import devplugin.Program;
import devplugin.SettingsItem;

/**
 * A class that handles the program context menu.
 * 
 * @author René Mach
 *
 */
public class ContextMenuManager {
  private static final Localizer mLocalizer = Localizer.getLocalizerFor(ContextMenuManager.class);
  
  public static final int NO_MOUSE_MODIFIER_EX = MouseEvent.BUTTON1_DOWN_MASK & ~MouseEvent.BUTTON1_DOWN_MASK;
  
  private static ContextMenuManager mInstance;
  
  private Hashtable<Integer, ContextMenuIf> mContextMenuLeftSingleClickTable;
  private Hashtable<Integer, ContextMenuIf> mContextMenuLeftDoubleClickTable;
  private Hashtable<Integer, ContextMenuIf> mContextMenuMiddleSingleClickTable;
  private Hashtable<Integer, ContextMenuIf> mContextMenuMiddleDoubleClickTable;
  
  private ContextMenuManager() {
    mInstance = this;
    mContextMenuLeftSingleClickTable = new Hashtable<Integer, ContextMenuIf>();
    mContextMenuLeftDoubleClickTable = new Hashtable<Integer, ContextMenuIf>();
    mContextMenuMiddleSingleClickTable = new Hashtable<Integer, ContextMenuIf>();
    mContextMenuMiddleDoubleClickTable = new Hashtable<Integer, ContextMenuIf>();
    init();
  }
  
  private void setContextMenuValues(Hashtable<Integer, ContextMenuIf> hashtable, ContextMenuMouseActionSetting[] clickArray) {
    for(ContextMenuMouseActionSetting setting : clickArray) {
      ContextMenuIf menuIf = setting.getContextMenuIf();
      
      if(menuIf != null) {
        hashtable.put(setting.getModifiersEx(), menuIf);
      }
    }
  }
  
  public void init() {
    mContextMenuLeftSingleClickTable.clear();
    mContextMenuLeftDoubleClickTable.clear();
    mContextMenuMiddleSingleClickTable.clear();
    mContextMenuMiddleDoubleClickTable.clear();
    
    
    setContextMenuValues(mContextMenuLeftSingleClickTable,Settings.propLeftSingleClickIfArray.getContextMenuMouseActionArray());
    setContextMenuValues(mContextMenuLeftDoubleClickTable,Settings.propLeftDoubleClickIfArray.getContextMenuMouseActionArray());
    setContextMenuValues(mContextMenuMiddleSingleClickTable,Settings.propMiddleSingleClickIfArray.getContextMenuMouseActionArray());
    setContextMenuValues(mContextMenuMiddleDoubleClickTable,Settings.propMiddleDoubleClickIfArray.getContextMenuMouseActionArray());
  }

	/**
   * Returns the instance of this class.
   * If the instance is null a new
   * will be created.
   * 
   * @return The instance of this class.
   */
  public static synchronized ContextMenuManager getInstance() {
    if(mInstance == null) {
      new ContextMenuManager();
    }
    return mInstance;
  }
  
  /**
   * Gets the ContextMenuIf for a single mouse click.
   * @param e The MouseEvent to get the ContextMenuIf for.
   * @return The ContextMenuIf for a single mouse click that matches the mouse event
   * or <code>null</code> if there is no single mouse click ContextMenuIf for the mouse event.
   * @since 3.3.1
   */
  public ContextMenuIf getContextMenuForSingleClick(MouseEvent e) {
    int cleanModifierEx = e.getModifiersEx() & ~(MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK);
    
    if(SwingUtilities.isLeftMouseButton(e)) {
      return mContextMenuLeftSingleClickTable.get(cleanModifierEx);
    }
    else if(SwingUtilities.isMiddleMouseButton(e)) {
      return mContextMenuMiddleSingleClickTable.get(cleanModifierEx & ~MouseEvent.ALT_DOWN_MASK);
    }
    
    return null;
  }
  
  /**
   * Gets the ContextMenuIf for a mouse click and modifier.
   * @param modifierEx The modifier for the mouse event.
   * @param leftMouseButton If the ContextMenuIf for the left mouse button should be gotten.
   * @param singleClick If the ContextMenuIf for a single mouse click should be gotten.
   * @return The ContextMenuIf for a mouse click that matches given values
   * or <code>null</code> if there is no single mouse click ContextMenuIf.
   * @since 3.3.1
   */
  public ContextMenuIf getContextMenuArrayForModifierEx(int modifierEx, boolean leftMouseButton, boolean singleClick) {
    if(leftMouseButton) {
      if(singleClick) {
        return mContextMenuLeftSingleClickTable.get(modifierEx);
      }
      else {
        return mContextMenuLeftDoubleClickTable.get(modifierEx);
      }
    }
    else {
      if(singleClick) {
        return mContextMenuMiddleSingleClickTable.get(modifierEx);
      }
      else {
        return mContextMenuMiddleDoubleClickTable.get(modifierEx);
      }
    }
  }
  
  /**
   * Gets the ContextMenuIf for a double mouse click.
   * @param e The MouseEvent to get the ContextMenuIf for.
   * @return The ContextMenuIf for a double mouse click that matches the mouse event
   * or <code>null</code> if there is no double mouse click ContextMenuIf for the mouse event.
   */
  public ContextMenuIf getContextMenuForDoubleClick(MouseEvent e) {
    int cleanModifierEx = e.getModifiersEx() & ~(MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK | MouseEvent.BUTTON3_DOWN_MASK);
    
    if(SwingUtilities.isLeftMouseButton(e)) {
      return mContextMenuLeftDoubleClickTable.get(cleanModifierEx);
    }
    else if(SwingUtilities.isMiddleMouseButton(e)) {
      return mContextMenuMiddleDoubleClickTable.get(cleanModifierEx & ~MouseEvent.ALT_DOWN_MASK);
    }
    
    return null;
  }
  
  /**
   * Returns the ContextMenuIf for the id.
   * 
   * @param id The id to get the ContextMenuIf for.
   * @return The ContextMenuIf for the id or null if id wasn't found.
   */
  public ContextMenuIf getContextMenuIfForId(String id) {
  	if (id == null) {
  		return null;
  	}
    PluginProxy plugin = PluginProxyManager.getInstance().getActivatedPluginForId(id);
    if(plugin != null) {
      return plugin;
    }
    TvDataServiceProxy dataService = TvDataServiceProxyManager.getInstance().findDataServiceById(id);
    
    if(dataService != null) {
      return dataService;
    } else if (id != null){
      InternalPluginProxyIf internalPlugin = InternalPluginProxyList.getInstance().getProxyForId(id);
      if(internalPlugin != null && internalPlugin instanceof ContextMenuIf) {
        return (ContextMenuIf)internalPlugin;
      }
      else if(id.compareTo(ConfigMenuItem.CONFIG) == 0) {
        return ConfigMenuItem.getInstance();
      } else if(id.compareTo(LeaveFullScreenMenuItem.LEAVEFULLSCREEN) == 0) {
        return LeaveFullScreenMenuItem.getInstance();
      } else if(id.compareTo(DoNothingContextMenuItem.DONOTHING) == 0) {
        return DoNothingContextMenuItem.getInstance();
      } else if(id.compareTo(SelectProgramContextMenuItem.SELECTPROGRAM) == 0) {
        return SelectProgramContextMenuItem.getInstance();
      }
    }
    
    return null;
  }
  
  /**
   * Returns all available ContextMenuItems
   * 
   * @param includingDisabledItems If true the List also contains all disabled Items
   * @param cleanSeparator If true, all Separators that follow directly another Separator will be removed
   * @return The available context menu interfaces.
   */
  public ContextMenuIf[] getAvailableContextMenuIfs(boolean includingDisabledItems, boolean cleanSeparator) {
    InternalPluginProxyIf[] internalPluginProxies = InternalPluginProxyList.getInstance().getAvailableProxys();
    PluginProxy[] pluginArr = PluginProxyManager.getInstance().getActivatedPlugins();
    TvDataServiceProxy[] dataServiceArr = TvDataServiceProxyManager.getInstance().getDataServices();
    String[] order = Settings.propContextMenuOrder.getStringArray();
    List<ContextMenuIf> disabledList = getDisabledContextMenuIfs();
    
    ArrayList<ContextMenuIf> ifList = new ArrayList<ContextMenuIf>();
        
    boolean lastWasSeparator = false;
    
    if(order == null) {
      for(InternalPluginProxyIf internalPluginProxy : internalPluginProxies) {
        if(internalPluginProxy instanceof ContextMenuIf) {
          ifList.add((ContextMenuIf)internalPluginProxy);
        }
      }

      ifList.add(new SeparatorMenuItem());
      for (PluginProxy pluginProxy : pluginArr) {
        ifList.add(pluginProxy);
      }
      
      ifList.add(new SeparatorMenuItem());
      for(TvDataServiceProxy dataService : dataServiceArr) {
        ifList.add(dataService);
      }
    }
    else {
      for (String element : order) {
        if (element.compareTo(SeparatorMenuItem.SEPARATOR) == 0) {
          // Add Separator only when an entry exists
          if (!cleanSeparator || (cleanSeparator && (ifList.size() > 0) && !lastWasSeparator)) {
            ifList.add(new SeparatorMenuItem());
            lastWasSeparator = true;
          }
        } else if (element.compareTo(ConfigMenuItem.CONFIG) == 0) {
          if ((includingDisabledItems) || (!disabledList.contains(ConfigMenuItem.getInstance()))) {
            ifList.add(ConfigMenuItem.getInstance());
            lastWasSeparator = false;
          }
        } else if (element.compareTo(LeaveFullScreenMenuItem.LEAVEFULLSCREEN) == 0) {
          if ((includingDisabledItems) || (!disabledList.contains(LeaveFullScreenMenuItem.getInstance()))) {
            ifList.add(LeaveFullScreenMenuItem.getInstance());
            lastWasSeparator = false;
          }
        } else {
          ContextMenuIf item = getContextMenuIfForId(element);
          if ((item != null) && (includingDisabledItems || !disabledList.contains(item))) {
            lastWasSeparator = false;
            ifList.add(item);
          }
        }
      }
    }
    
    for(InternalPluginProxyIf internalPluginProxy : internalPluginProxies) {
      if(internalPluginProxy instanceof ContextMenuIf) {
        if(!ifList.contains(internalPluginProxy)) {
          if ((includingDisabledItems) || ((internalPluginProxy != null) && (!disabledList.contains(internalPluginProxy)))) {
            ifList.add((ContextMenuIf)internalPluginProxy);
          }
        }
      }
    }
    
    for(int i = 0; i < pluginArr.length; i++) {
      if(!ifList.contains(pluginArr[i])) {
        if ((includingDisabledItems) || ((pluginArr[i] != null) && (!disabledList.contains(pluginArr[i])))) {
          ifList.add(pluginArr[i]);
        }
      }
    }

    for(int i = 0; i < dataServiceArr.length; i++) {
      if(!ifList.contains(dataServiceArr[i])) {
        if ((includingDisabledItems) || ((dataServiceArr[i] != null) && (!disabledList.contains(dataServiceArr[i])))) {
          ifList.add(dataServiceArr[i]);
        }
      }
    }

    if (!ifList.contains(LeaveFullScreenMenuItem.getInstance())) {
      if ((includingDisabledItems) || (!disabledList.contains(LeaveFullScreenMenuItem.getInstance()))) {
        ifList.add(LeaveFullScreenMenuItem.getInstance());
      }
    }
    if (!ifList.contains(ConfigMenuItem.getInstance())) {
      if ((includingDisabledItems) || (!disabledList.contains(ConfigMenuItem.getInstance()))) {
        if (!lastWasSeparator) {
          ifList.add(new SeparatorMenuItem());
        }
        ifList.add(ConfigMenuItem.getInstance());
      }
    }
    
    if (cleanSeparator) {
      // Wenn letztes Element Separator ist, diesen entfernen
      while (ifList.get(ifList.size()-1) instanceof SeparatorMenuItem) {
        ifList.remove(ifList.size()-1);
      }
    }
    
    ContextMenuIf[] menuIf = new ContextMenuIf[ifList.size()];
    ifList.toArray(menuIf);
    
    return menuIf;
  }


  /**
   * Creates the context menu items.
   * 
   * @param callerIf The caller Context menu interface.
   * @param program The program to show the context menu for.
   * @param markDefaultIf True if the default context menu interfaces should be highlighted.
   * @return The menu items of the context menu.
   */
  public JMenu createContextMenuItems(ContextMenuIf callerIf, Program program, boolean markDefaultIf) {
    ArrayList<JMenuItem> items = new ArrayList<JMenuItem>();
    ContextMenuIf[] menuIfArr = getInstance().getAvailableContextMenuIfs(false, true);

    JMenu rootMenu = new JMenu();
    
    for (ContextMenuIf element : menuIfArr) {
      ContextMenuIf menuIf = element;

      boolean equalsPlugin = false;

      if ((callerIf != null) && (callerIf.getId().equals(menuIf.getId()))) {
        equalsPlugin = true;
      }

      if (menuIf instanceof SeparatorMenuItem) {
        if (rootMenu.getMenuComponentCount() > 0) {
          rootMenu.addSeparator();
        }
      } else if (menuIf instanceof ConfigMenuItem) {
        JMenuItem item = new JMenuItem(menuIf.toString());
        item.setIcon(TVBrowserIcons.preferences(TVBrowserIcons.SIZE_SMALL));
        item.setFont(MenuUtil.CONTEXT_MENU_PLAINFONT);
        item.addActionListener(new ActionListener() {
          public void actionPerformed(java.awt.event.ActionEvent e) {
            MainFrame.getInstance().showSettingsDialog(SettingsItem.CONTEXTMENU);
          };
        });
        rootMenu.add(item);
      } else if (menuIf instanceof LeaveFullScreenMenuItem) {
        if (MainFrame.getInstance().isFullScreenMode()) {
          JMenuItem item = new JMenuItem(menuIf.toString());
          item.setFont(MenuUtil.CONTEXT_MENU_PLAINFONT);
          item.setIcon(TVBrowserIcons.fullScreen(TVBrowserIcons.SIZE_SMALL));
          item.addActionListener(new ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
              if (MainFrame.getInstance().isFullScreenMode()) {
                MainFrame.getInstance().switchFullscreenMode();
              }
            };
          });
          rootMenu.add(item);
        }
      } else if (!equalsPlugin) {
        ActionMenu actionMenu = menuIf.getContextMenuActions(program);
        if (actionMenu != null) {
          JMenuItem menuItem = MenuUtil.createMenuItem(actionMenu);
          items.add(menuItem);

          rootMenu.add(menuItem);
        }
      }
    }

    // Remove last Item if it's a Separator
    while (rootMenu.getMenuComponent(rootMenu.getMenuComponentCount()-1) instanceof JPopupMenu.Separator) {
      rootMenu.remove(rootMenu.getMenuComponentCount()-1);
    }
    
    // remove duplicate separators
    for (int i = rootMenu.getMenuComponentCount() - 2; i > 0; i--) {
      if (rootMenu.getMenuComponent(i) instanceof JPopupMenu.Separator
          && rootMenu.getMenuComponent(i + 1) instanceof JPopupMenu.Separator) {
        rootMenu.remove(i + 1);
      }
    }
    
    return rootMenu;
  }

  /**
   * Returns a List with all disabled ContextMenuIfs
   * @return disabled ContextMenuIfs
   */
  public List<ContextMenuIf> getDisabledContextMenuIfs() {
    String[] disabled = Settings.propContextMenuDisabledItems.getStringArray();
    
    ArrayList<ContextMenuIf> list = new ArrayList<ContextMenuIf>();
    if (disabled == null) {
      return list;
    }
    
    for (String element : disabled) {
      ContextMenuIf item = getContextMenuIfForId(element);
      if (item != null) {
        list.add(item);
      }
    }
    
    return list;
  }
  
  public JPopupMenu createRemovedProgramContextMenu(final Program program) {
    JPopupMenu menu = new JPopupMenu();
    
    ActionMenu repetitionSearch = SearchPluginProxy.getInstance().getContextMenuActions(program);
    
    if(repetitionSearch != null) {
      menu.add(MenuUtil.createMenuItem(repetitionSearch));
    }
    
    JMenuItem item = new JMenuItem(mLocalizer.msg("scrollToPlaceOfProgram","Scroll to last place of program in program table"));
    item.setFont(MenuUtil.CONTEXT_MENU_PLAINFONT);
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        MainFrame.getInstance().goTo(program.getDate());
        MainFrame.getInstance().showChannel(program.getChannel());
        MainFrame.getInstance().scrollToTime(program.getStartTime());
        MainFrame.getInstance().showProgramTableTabIfAvailable();
      }
    });
    
    menu.add(item);
    
    return menu;
  }
}
