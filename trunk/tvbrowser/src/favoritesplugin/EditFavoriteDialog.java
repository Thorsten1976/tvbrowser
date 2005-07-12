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
 *     $Date$
 *   $Author$
 * $Revision$
 */

package favoritesplugin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Calendar;
import java.util.Date;

import javax.swing.*;

import util.exc.ErrorHandler;
import util.exc.TvBrowserException;
import util.ui.ChannelListCellRenderer;
import util.ui.SearchForm;
import util.ui.TabLayout;
import util.ui.UiUtilities;
import devplugin.Channel;
import devplugin.Plugin;
import devplugin.ProgramFilter;

/**
 *
 * @author Til Schneider, www.murfman.de
 */
public class EditFavoriteDialog {

  /** The localizer for this class. */  
  private static final util.ui.Localizer mLocalizer
    = util.ui.Localizer.getLocalizerFor(EditFavoriteDialog.class);

  private Favorite mFavorite;
  private Channel[] mSubscribedChannelArr;

  private JDialog mDialog;
  private SearchForm mSearchForm;
  private JCheckBox mCertainChannelChB, mCertainTimeOfDayChB, mCertainFilterChB;
  private JComboBox mCertainChannelCB, mCertainFilterCB;
  private JSpinner mCertainFromTimeSp, mCertainToTimeSp;
  private JLabel mCertainToTimeLabel;
  private JButton mOkBt, mCancelBt;
  
  private boolean mOkWasPressed = false;
  
  
  
  /** 
   * Creates a new instance of EditFavoriteDialog.
   */
  public EditFavoriteDialog(Component parent, Favorite favorite) {
    mFavorite = favorite;
    mSubscribedChannelArr = Plugin.getPluginManager().getSubscribedChannels();

    mDialog = UiUtilities.createDialog(parent, true);
    mDialog.setTitle(mLocalizer.msg("dlgTitle", "Edit favorite program"));
    
    JPanel main = new JPanel(new TabLayout(1));
    main.setBorder(UiUtilities.DIALOG_BORDER);
    mDialog.setContentPane(main);
    
    JPanel p1, p2;
    String msg;

    // search form
    mSearchForm = new SearchForm(false, false);
    main.add(mSearchForm);
    
    // limitations
    p1 = new JPanel(new TabLayout(2));
    msg = mLocalizer.msg("limitations", "Limitations");
    p1.setBorder(BorderFactory.createTitledBorder(msg));
    main.add(p1);
    
    msg = mLocalizer.msg("certainChannel", "Certain channel");
    mCertainChannelChB = new JCheckBox(msg);
    mCertainChannelChB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        updateEnabled();
      }
    });
    p1.add(mCertainChannelChB);
    
    mCertainChannelCB = new JComboBox(mSubscribedChannelArr);
    mCertainChannelCB.setRenderer(new ChannelListCellRenderer());
    p1.add(mCertainChannelCB);

    msg = mLocalizer.msg("certainTimeOfDay", "Certain time of day");
    mCertainTimeOfDayChB = new JCheckBox(msg);
    mCertainTimeOfDayChB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        updateEnabled();
      }
    });
    p1.add(mCertainTimeOfDayChB);
    
    p2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
    p1.add(p2);
    String timePattern = mLocalizer.msg("timePattern", "hh:mm a");
    
    mCertainFromTimeSp = new JSpinner(new SpinnerDateModel());
    mCertainFromTimeSp.setEditor(new JSpinner.DateEditor(mCertainFromTimeSp, timePattern));
    mCertainFromTimeSp.setBorder(null);
    p2.add(mCertainFromTimeSp);
    
    msg = mLocalizer.msg("timeTo", "to");
    mCertainToTimeLabel = new JLabel("  " + msg + "  ");
    p2.add(mCertainToTimeLabel);

    mCertainToTimeSp = new JSpinner(new SpinnerDateModel());
    mCertainToTimeSp.setEditor(new JSpinner.DateEditor(mCertainToTimeSp, timePattern));
    mCertainToTimeSp.setBorder(null);
    p2.add(mCertainToTimeSp);

    
    mCertainFilterChB = new JCheckBox(mLocalizer.msg("useFilter", "Use Filter")); 
    mCertainFilterChB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          updateEnabled();
        }
      });
    mCertainFilterCB = new JComboBox(Plugin.getPluginManager().getAvailableFilters());
    p1.add(mCertainFilterChB);
    p1.add(mCertainFilterCB);
    
    
    // buttons
    JPanel buttonPn = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    main.add(buttonPn);
    
    mOkBt = new JButton(mLocalizer.msg("ok", "OK"));
    mOkBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        if (saveValues()) {
          mOkWasPressed = true;
          mDialog.dispose();
        }
      }
    });
    buttonPn.add(mOkBt);
    mDialog.getRootPane().setDefaultButton(mOkBt);

    mCancelBt = new JButton(mLocalizer.msg("cancel", "Cancel"));
    mCancelBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        mDialog.dispose();
      }
    });
    buttonPn.add(mCancelBt);
    
    loadValues();
    mDialog.pack();
  }
  
  
  
  public void centerAndShow() {
    UiUtilities.centerAndShow(mDialog);
  }

  
  
  private void updateEnabled() {
    boolean enabled = mCertainChannelChB.isSelected();
    mCertainChannelCB.setEnabled(enabled);

    enabled = mCertainTimeOfDayChB.isSelected();
    mCertainFromTimeSp.setEnabled(enabled);
    mCertainToTimeLabel.setEnabled(enabled);
    mCertainToTimeSp.setEnabled(enabled);
    
    enabled = mCertainFilterChB.isSelected();
    mCertainFilterCB.setEnabled(enabled);
  }

  
  
  private void loadValues() {
    mSearchForm.setSearchFormSettings(mFavorite.getSearchFormSettings());

    mCertainChannelChB.setSelected(mFavorite.getUseCertainChannel());
    Channel certainChannel = mFavorite.getCertainChannel();
    mCertainChannelCB.setSelectedItem(certainChannel);
    
    mCertainTimeOfDayChB.setSelected(mFavorite.getUseCertainTimeOfDay());
    Calendar cal = Calendar.getInstance();
    
    int minutes = mFavorite.getCertainFromTime();
    cal.set(Calendar.HOUR_OF_DAY, minutes / 60);
    cal.set(Calendar.MINUTE, minutes % 60);
    mCertainFromTimeSp.setValue(cal.getTime());
    
    minutes = mFavorite.getCertainToTime();
    cal.set(Calendar.HOUR_OF_DAY, minutes / 60);
    cal.set(Calendar.MINUTE, minutes % 60);
    mCertainToTimeSp.setValue(cal.getTime());

    mCertainFilterChB.setSelected(mFavorite.getUseFilter());
    
    if (mFavorite.getFilter() != null) {
        mCertainFilterCB.setSelectedItem(mFavorite.getFilter());
    }
    
    updateEnabled();
  }
  
  
  
  private boolean saveValues() {
    mFavorite.setSearchFormSettings(mSearchForm.getSearchFormSettings());

    mFavorite.setUseCertainChannel(mCertainChannelChB.isSelected());
    Channel certainChannel = (Channel)mCertainChannelCB.getSelectedItem();
    mFavorite.setCertainChannel(certainChannel);
    
    mFavorite.setUseCertainTimeOfDay(mCertainTimeOfDayChB.isSelected());
    Calendar cal = Calendar.getInstance();
    
    Date fromTime = (Date) mCertainFromTimeSp.getValue();
    cal.setTime(fromTime);
    int minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    mFavorite.setCertainFromTime(minutes);
    
    Date toTime = (Date) mCertainToTimeSp.getValue();
    cal.setTime(toTime);
    minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    mFavorite.setCertainToTime(minutes);
    
    mFavorite.setUseFilter(mCertainFilterChB.isSelected());
    mFavorite.setFilter((ProgramFilter) mCertainFilterCB.getSelectedItem());
    
    try {
      mFavorite.updatePrograms();
      return true;
    }
    catch (TvBrowserException exc) {
      ErrorHandler.handle(exc);
      return false;
    }
  }
  
  
  
  public boolean getOkWasPressed() {
    return mOkWasPressed;
  }

}
