/*
 * TV-Browser
 * Copyright (C) 2014 TV-Browser team (dev@tvbrowser.org)
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
 *     $Date$
 *   $Author$
 * $Revision$
 */
package tvbrowser.core.icontheme;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import tvbrowser.core.Settings;

/**
 * A class to load all available info icon themes.
 * 
 * @author René Mach
 */
public class InfoThemeLoader {
  private static InfoThemeLoader INSTANCE;
  
  private static final FileFilter THEME_FILE_FILTER = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory() || (pathname.isFile() && pathname.getName().toLowerCase().endsWith(".zip"));
    }
  };
  
  private HashMap<String, InfoIconTheme> mInfoIconThemeMap;
  
  private InfoThemeLoader() {
    mInfoIconThemeMap = new HashMap<String, InfoIconTheme>();
    
    File userThemes = new File(Settings.getUserSettingsDirName(),"infothemes");
    
    if(userThemes.isDirectory()) {
      File[] iconThemes = userThemes.listFiles(THEME_FILE_FILTER);
      
      if(iconThemes != null) {
        for(File iconTheme : iconThemes) {
          addIconTheme(iconTheme);
        }
      }
    }
    
    File globalThemes = new File("infothemes");
    
    if(globalThemes.isDirectory()) {
      File[] iconThemes = globalThemes.listFiles(THEME_FILE_FILTER);
      
      if(iconThemes != null) {
        for(File iconTheme : iconThemes) {
          addIconTheme(iconTheme);
        }
      }
    }
  }
  
  /**
   * Adds a theme to the theme map.
   * Theme will only be added if ID of theme doesn't already exists.
   * <p>
   * @param iconTheme The theme to add.
   */
  public void addIconTheme(File iconTheme) {
    InfoIconTheme theme = new InfoIconTheme(iconTheme);
    
    if(theme.toString() != null && !mInfoIconThemeMap.containsKey(theme.getID())) {
      mInfoIconThemeMap.put(theme.getID(), theme);
    }
  }
  
  /**
   * Get the instance of this class.
   * <p>
   * @return The instance of this class.
   */
  public static synchronized InfoThemeLoader getInstance() {
    if(INSTANCE == null) {
      INSTANCE = new InfoThemeLoader();
    }
    
    return INSTANCE;
  }
  
  /**
   * Gets the theme with the given ID.
   * <p>
   * @param id The ID of the theme to get.
   * @return The theme with the given ID or <code>null</code> if no theme with ID exists.
   */
  public InfoIconTheme getIconThemeForID(String id) {
    return mInfoIconThemeMap.get(id);
  }
  
  /**
   * Gets the theme with the given ID or the default theme if no theme with ID exists.
   * <p>
   * @param id The ID of the theme to get.
   * @return The theme with the given ID or the default theme if no theme with ID exists.
   */
  public InfoIconTheme getIconThemeForIDOrDefault(String id) {
    InfoIconTheme theme = mInfoIconThemeMap.get(id);
    
    if(theme == null) {
      theme = getDefaultTheme();
    }
    
    return theme;
  }
  
  /**
   * Gets the default info icon theme.
   * <p>
   * @return The default info icon theme.
   */
  public InfoIconTheme getDefaultTheme() {
    return mInfoIconThemeMap.get(Settings.propInfoIconThemeID.getDefault());
  }
  
  public InfoIconTheme[] getAvailableInfoIconThemes() {
    InfoIconTheme[] iconThemes = mInfoIconThemeMap.values().toArray(new InfoIconTheme[mInfoIconThemeMap.size()]);
    
    Arrays.sort(iconThemes);
    
    return iconThemes;
  }
}
