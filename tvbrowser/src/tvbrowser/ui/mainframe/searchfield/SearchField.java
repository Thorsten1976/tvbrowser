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
package tvbrowser.ui.mainframe.searchfield;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import tvbrowser.core.filters.FilterManagerImpl;
import tvbrowser.core.icontheme.IconLoader;
import tvbrowser.ui.mainframe.MainFrame;
import util.exc.TvBrowserException;
import util.settings.PluginPictureSettings;
import util.settings.ProgramPanelSettings;
import util.ui.Localizer;
import util.ui.SearchForm;
import util.ui.SearchFormSettings;
import util.ui.SearchHelper;
import util.ui.UiUtilities;
import util.ui.WindowClosingIf;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import devplugin.Plugin;

/**
 * A SearchField for the Toolbar
 * 
 * @author bodum
 */
public class SearchField extends JPanel {
  /** The localizer of this class. */  
  private static final util.ui.Localizer mLocalizer
    = util.ui.Localizer.getLocalizerFor(SearchField.class);
  /** TextField */
  private SearchTextField mText;
  /** Settings for the Search */
  private SearchFormSettings mSearchFormSettings;
  /** Button for the Settings-Popup*/
  private JButton mSearchButton, mCancelButton;
  
  private static final String SETTINGS_FILE = "searchfield.SearchField.dat";
  
  /**
   * Create SearchField
   */
  public SearchField() { 
    readSearchFormSettings();
    createGui();
  }
  
  /**
   * Read the search form settings from the settings file.
   */
  private void readSearchFormSettings() {
    try {
      String home = Plugin.getPluginManager().getTvBrowserSettings().getTvBrowserUserHome();
      File settingsFile = new File(home,SETTINGS_FILE);
      
      ObjectInputStream stream = new ObjectInputStream(new FileInputStream(settingsFile));
      mSearchFormSettings = new SearchFormSettings(stream);
      stream.close();
    }catch(Exception e) {
      createDefaultSearchFormSettings();
    }    
  }
  
  /**
   * Save the search form settings to the settings file.
   */
  private void saveSearchFormSettings() {
    try {
      String home = Plugin.getPluginManager().getTvBrowserSettings().getTvBrowserUserHome();
      File settingsFile = new File(home,SETTINGS_FILE);
      
      ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(settingsFile));
      mSearchFormSettings.writeData(stream);
      stream.close();
    }catch(Exception e) {} 
  }
  
  /**
   * Create the default search form settings.
   */
  private void createDefaultSearchFormSettings() {
    mSearchFormSettings = new SearchFormSettings("");
    mSearchFormSettings.setNrDays(0);
  }

  /**
   * Create the GUI
   */
  private void createGui() {
    final JPanel panel = new JPanel(new BorderLayout(3,0));
    panel.setBorder(BorderFactory.createCompoundBorder(UIManager.getBorder("TextField.border"),BorderFactory.createEmptyBorder(2,2,1,2)));
    
    mText = new SearchTextField(15);
    
    if(UIManager.getLookAndFeel().getClass().getCanonicalName().equals("com.sun.java.swing.plaf.gtk.GTKLookAndFeel")) {
      mText.setBackground(Color.white);
      mText.setBorder(BorderFactory.createLineBorder(mText.getBackground(), 3));
    }
    else {
      mText.setBorder(BorderFactory.createLineBorder(mText.getBackground()));
    }
    
    mText.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          mSearchFormSettings.setSearchText(mText.getText());
          
          if (mSearchFormSettings.getNrDays() == 0) {
            if (mText.getText().length() > 0) {
              try {
                SearchFilter filter = SearchFilter.getInstance();
                filter.setSearch(mSearchFormSettings);
                MainFrame.getInstance().setProgramFilter(filter);
                mCancelButton.setVisible(true);
                
                SwingUtilities.invokeLater(new Runnable() {
                  public void run() {
                    mText.setCaretPosition(mText.getText().length());    
                  }
                });
              } catch (TvBrowserException e1) {
                e1.printStackTrace();
              }
            } else {
              SearchFilter.getInstance().deactivateSearch();
              MainFrame.getInstance().setProgramFilter(FilterManagerImpl.getInstance().getDefaultFilter());
              mCancelButton.setVisible(false);
            }
          } else {
            SearchHelper.search(mText, mSearchFormSettings, new ProgramPanelSettings(new PluginPictureSettings(PluginPictureSettings.ALL_PLUGINS_SETTINGS_TYPE),false),true);
          }
        }
      }
    });
    
    panel.setBackground(mText.getBackground());

    mSearchButton = new JButton(IconLoader.getInstance().getIconFromTheme("action", "system-search", 16)); 
    mSearchButton.setBorder(BorderFactory.createEmptyBorder());
    mSearchButton.setContentAreaFilled(false);
    mSearchButton.setMargin(new Insets(0, 0, 0, 0));
    mSearchButton.setFocusPainted(false);
    mSearchButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showConfigureDialog(mText);
      };
    });
    
    mCancelButton = new JButton(IconLoader.getInstance().getIconFromTheme("action", "process-stop", 16)); 
    mCancelButton.setBorder(BorderFactory.createEmptyBorder());
    mCancelButton.setContentAreaFilled(false);
    mCancelButton.setMargin(new Insets(0, 0, 0, 0));
    mCancelButton.setFocusPainted(false);
    mCancelButton.setVisible(false);
    mCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        cancelPressed();
      };
    });mText.setEditable(true);
    
    panel.setOpaque(true);
    
    panel.add(mSearchButton, BorderLayout.WEST);
    panel.add(mText, BorderLayout.CENTER);
    panel.add(mCancelButton, BorderLayout.EAST);
    
    setLayout(new FormLayout("70dlu, 2dlu", "fill:pref:grow, pref, fill:pref:grow"));
    add(panel, new CellConstraints().xy(1, 2));
  }

  /**
   * Cancel the Search
   */
  protected void cancelPressed() {
    mText.setText("");
    SearchFilter.getInstance().deactivateSearch();
    MainFrame.getInstance().setProgramFilter(FilterManagerImpl.getInstance().getDefaultFilter());
    mCancelButton.setVisible(false);
    mText.focusLost(null);
  }

  /**
   * Show the Configuration-Dialog
   * @param textField 
   */
  protected void showConfigureDialog(final SearchTextField textField) {
    final SearchForm form = new SearchForm(false, false, true);
    form.setSearchFormSettings(mSearchFormSettings);
    
    final JDialog configure = new JDialog(MainFrame.getInstance(), mLocalizer.msg("settingsTitle","Search-Settings"), false);
    configure.setUndecorated(true);
    JPanel panel = (JPanel)configure.getContentPane();
    panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(),Borders.DLU4_BORDER));
    panel.setLayout(new FormLayout("fill:pref:grow, 3dlu, pref", "pref, fill:3dlu:grow, pref"));
    
    form.setParentDialog(configure);
    
    CellConstraints cc = new CellConstraints();
    
    panel.add(form, cc.xyw(1, 1, 3));
    
    JButton ok = new JButton(Localizer.getLocalization(Localizer.I18N_OK));
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        configure.setVisible(false);
        mSearchFormSettings = form.getSearchFormSettings();
        saveSearchFormSettings();
        textField.requestFocusInWindow();
        textField.selectAll();
      }
    });
    
    panel.add(ok, cc.xy(3,3));

    UiUtilities.registerForClosing(new WindowClosingIf() {
      public void close() {
        configure.removeWindowListener(configure.getWindowListeners()[0]);
        configure.setVisible(false);
        textField.requestFocusInWindow();
        textField.selectAll();
      }

      public JRootPane getRootPane() {
        return configure.getRootPane();
      }
    });    
    
    configure.pack();
    
    configure.addWindowListener(new WindowAdapter() {
      public void windowDeactivated(WindowEvent e) {
        if(!form.isSearchFieldsSelectionDialogVisible()) {
          ((JDialog)e.getSource()).setVisible(false);
          mSearchFormSettings = form.getSearchFormSettings();
          saveSearchFormSettings();
        }
      }
    });
    
    Point p = mSearchButton.getLocationOnScreen();
    
    if(MainFrame.getInstance().getToolbar().getToolbarLocation().compareTo(BorderLayout.NORTH) == 0)
      configure.setLocation(p.x - configure.getWidth() + mSearchButton.getWidth(),p.y + mSearchButton.getHeight());
    else
      configure.setLocation(p.x ,p.y - configure.getHeight());
    configure.setVisible(true);
  }

  /**
   * SearchFilter was deactivated
   */
  public void deactivateSearch() {
    mCancelButton.setVisible(false);
  }

  
}