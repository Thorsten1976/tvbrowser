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

package tvbrowser.extras.favoritesplugin.dlgs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import tvbrowser.core.icontheme.IconLoader;
import tvbrowser.extras.common.ReminderConfiguration;
import tvbrowser.extras.favoritesplugin.FavoritesPlugin;
import tvbrowser.extras.favoritesplugin.core.AdvancedFavorite;
import tvbrowser.extras.favoritesplugin.core.Favorite;
import tvbrowser.extras.favoritesplugin.wizards.TypeWizardStep;
import tvbrowser.extras.favoritesplugin.wizards.WizardHandler;
import tvbrowser.extras.reminderplugin.ReminderPlugin;
import tvbrowser.extras.reminderplugin.ReminderPluginProxy;
import util.exc.ErrorHandler;
import util.exc.TvBrowserException;
import util.ui.DragAndDropMouseListener;
import util.ui.ExtensionFileFilter;
import util.ui.ListDragAndDropHandler;
import util.ui.ListDropAction;
import util.ui.ProgramList;
import util.ui.SendToPluginDialog;
import util.ui.UiUtilities;
import util.ui.WindowClosingIf;

import com.jgoodies.forms.factories.Borders;

import devplugin.Program;
import devplugin.ProgramReceiveIf;


/**
 * A dialog for managing the favorite programs.
 *
 * @author Til Schneider, www.murfman.de
 */
public class ManageFavoritesDialog extends JDialog implements ListDropAction, WindowClosingIf{

  /** The localizer for this class. */
  public static final util.ui.Localizer mLocalizer
    = util.ui.Localizer.getLocalizerFor(ManageFavoritesDialog.class);

  private DefaultListModel mFavoritesListModel, mProgramListModel;
  private JList mFavoritesList;
  private ProgramList mProgramList;
  private JSplitPane mSplitPane;
  private JButton mNewBt, mEditBt, mDeleteBt, mUpBt, mDownBt, mSortBt, mImportBt, mSendBt;
  private JButton mCloseBt;
  private JScrollPane mProgramScrollPane;

  private boolean mShowNew = false;
  private static ManageFavoritesDialog mInstance = null;
  private JCheckBox mBlackListChb;

  public ManageFavoritesDialog(Dialog parent, Favorite[] favoriteArr, int splitPanePosition, boolean showNew) {
    super(parent, true);
    init(favoriteArr, splitPanePosition, showNew);
  }

  public ManageFavoritesDialog(Frame parent, Favorite[] favoriteArr, int splitPanePosition, boolean showNew) {
    super(parent, true);
    init(favoriteArr, splitPanePosition, showNew);
  }

  private void init(Favorite[] favoriteArr, int splitPanePosition, boolean showNew) {
    mInstance = this;

    mShowNew = showNew;

    String msg;
    Icon icon;

    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        mInstance = null;
      }
    });

    UiUtilities.registerForClosing(this);

    if(mShowNew)
      setTitle(mLocalizer.msg("newTitle", "New programs found"));
    else
      setTitle(mLocalizer.msg("title", "Manage favorite programs"));

    JPanel main = new JPanel(new BorderLayout(5, 5));
    main.setBorder(Borders.DLU4_BORDER);
    setContentPane(main);

    JToolBar toolbarPn = new JToolBar();
    toolbarPn.setFloatable(false);
    toolbarPn.setBorder(BorderFactory.createEmptyBorder());
    main.add(toolbarPn, BorderLayout.NORTH);

    msg = mLocalizer.msg("new", "Create a new favorite...");
    icon = FavoritesPlugin.getInstance().getIconFromTheme("actions", "document-new", 22);
    mNewBt = UiUtilities.createToolBarButton(msg, icon);
    mNewBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        newFavorite();
      }
    });
    if(!mShowNew) {
      toolbarPn.add(mNewBt);
      toolbarPn.addSeparator();
    }

    msg = mLocalizer.msg("edit", "Edit the selected favorite...");
    icon = IconLoader.getInstance().getIconFromTheme("actions", "document-edit", 22);
    mEditBt = UiUtilities.createToolBarButton(msg, icon);
    mEditBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        editSelectedFavorite();
      }
    });
    toolbarPn.add(mEditBt);

    msg = mLocalizer.msg("delete", "Delete selected favorite...");
    icon = FavoritesPlugin.getInstance().getIconFromTheme("actions", "edit-delete", 22);
    mDeleteBt = UiUtilities.createToolBarButton(msg, icon);
    mDeleteBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        deleteSelectedFavorite();
      }
    });
    toolbarPn.add(mDeleteBt);
    toolbarPn.addSeparator();

    msg = mLocalizer.msg("up", "Move the selected favorite up");
    icon = FavoritesPlugin.getInstance().getIconFromTheme("actions", "go-up", 22);
    mUpBt = UiUtilities.createToolBarButton(msg, icon);
    mUpBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        moveSelectedFavorite(-1);
      }
    });

    if(!mShowNew)
      toolbarPn.add(mUpBt);

    msg = mLocalizer.msg("down", "Move the selected favorite down");
    icon = FavoritesPlugin.getInstance().getIconFromTheme("actions", "go-down", 22);
    mDownBt = UiUtilities.createToolBarButton(msg, icon);
    mDownBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        moveSelectedFavorite(1);
      }
    });

    if(!mShowNew)
      toolbarPn.add(mDownBt);

    msg = mLocalizer.msg("send", "Send Programs to another Plugin");
    icon = FavoritesPlugin.getInstance().getIconFromTheme("actions", "edit-copy", 22);
    mSendBt = UiUtilities.createToolBarButton(msg, icon);
    mSendBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
         showSendDialog();
      }
    });
    toolbarPn.addSeparator();
    toolbarPn.add(mSendBt);
    toolbarPn.addSeparator();

    msg = mLocalizer.msg("sort", "Sort favorites alphabetically");
    icon = new ImageIcon(this.getClass().getClassLoader().getResource("tvbrowser/extras/favoritesplugin/Sort24.gif"));
    mSortBt = UiUtilities.createToolBarButton(msg, icon);
    mSortBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        sortFavorites();
      }
    });

    if(!mShowNew)
      toolbarPn.add(mSortBt);

    msg = mLocalizer.msg("import", "Import favorites from TVgenial");
    icon = FavoritesPlugin.getInstance().getIconFromTheme("actions", "document-open", 22);
    mImportBt = UiUtilities.createToolBarButton(msg, icon);
    mImportBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        importFavorites();
      }
    });

    if(!mShowNew)
      toolbarPn.add(mImportBt);

    mSplitPane = new JSplitPane();
    mSplitPane.setDividerLocation(splitPanePosition);
    main.add(mSplitPane, BorderLayout.CENTER);

    mFavoritesListModel = new DefaultListModel();
    mFavoritesListModel.ensureCapacity(favoriteArr.length);
    for (int i = 0; i < favoriteArr.length; i++) {
      mFavoritesListModel.addElement(favoriteArr[i]);
    }

    mFavoritesList = new JList(mFavoritesListModel);
    mFavoritesList.setCellRenderer(new FavoriteListCellRenderer());
    ListSelectionModel selModel = mFavoritesList.getSelectionModel();
    selModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    selModel.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent evt) {
        if(!evt.getValueIsAdjusting())
          favoriteSelectionChanged();
      }
    });

    if(!mShowNew) {
      ListDragAndDropHandler dnDHandler = new ListDragAndDropHandler(mFavoritesList,mFavoritesList,this);
      new DragAndDropMouseListener(mFavoritesList,mFavoritesList,this,dnDHandler);
    }

    mFavoritesList.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
          showFavoritesPopUp(e.getX(), e.getY());
        }
      }

      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
          showFavoritesPopUp(e.getX(), e.getY());
        }
      }

      public void mouseClicked(MouseEvent e) {
        if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
          editSelectedFavorite();
      }
    });

    JScrollPane scrollPane = new JScrollPane(mFavoritesList);
    scrollPane.setBorder(null);
    mSplitPane.setLeftComponent(scrollPane);

    mProgramListModel = new DefaultListModel();
    mProgramList = new ProgramList(mProgramListModel);
    mProgramList.addMouseListeners(null);
    mProgramScrollPane = new JScrollPane(mProgramList);
    mProgramScrollPane.setBorder(null);
    mSplitPane.setRightComponent(mProgramScrollPane);
    
    msg = mLocalizer.msg("showBlack", "Show single removed programs");
    mBlackListChb = new JCheckBox(msg);
    mBlackListChb.setSelected(FavoritesPlugin.getInstance().isShowingBlackListEntries());
    mBlackListChb.setOpaque(false);
    mBlackListChb.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        FavoritesPlugin.getInstance().setIsShowingBlackListEntries(mBlackListChb.isSelected());
        favoriteSelectionChanged();
      }
    });

    JPanel buttonPn = new JPanel(new BorderLayout());
    
    if(!mShowNew)
      buttonPn.add(mBlackListChb, BorderLayout.WEST);
    
    main.add(buttonPn, BorderLayout.SOUTH);

    mCloseBt = new JButton(mLocalizer.msg("close", "Close"));
    mCloseBt.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        close();
      }
    });
    buttonPn.add(mCloseBt, BorderLayout.EAST);
    getRootPane().setDefaultButton(mCloseBt);

    setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        close();
      }
    });
    
    favoriteSelectionChanged();
  }

  /**
   * Show the Popup-Menu
   */
  protected void showFavoritesPopUp(int x, int y) {
    JPopupMenu menu = new JPopupMenu();

    mFavoritesList.setSelectedIndex(mFavoritesList.locationToIndex(new Point(x,y)));

    if (!mShowNew) {
      JMenuItem createNew = new JMenuItem(mLocalizer.msg("new", "Create a new favorite..."),
          FavoritesPlugin.getInstance().getIconFromTheme("actions", "document-new", 16));

      createNew.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          newFavorite();
        }
      });

      menu.add(createNew);
      menu.addSeparator();
    }

    JMenuItem edit = new JMenuItem(mLocalizer.msg("edit", "Edit the selected favorite..."),
        IconLoader.getInstance().getIconFromTheme("actions", "document-edit", 16));

    edit.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        editSelectedFavorite();
      }
    });

    menu.add(edit);

    JMenuItem delete = new JMenuItem(mLocalizer.msg("delete", "Delete selected favorite..."),
        IconLoader.getInstance().getIconFromTheme("actions", "edit-delete", 16));

    delete.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        deleteSelectedFavorite();
      }
    });

    menu.add(delete);

    if (!mShowNew) {
      JMenuItem moveUp = new JMenuItem(mLocalizer.msg("up", "Move the selected favorite up"),
          FavoritesPlugin.getInstance().getIconFromTheme("actions", "go-up", 16));

      moveUp.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          moveSelectedFavorite(-1);
        }
      });

      moveUp.setEnabled(mFavoritesList.getSelectedIndex() > 0);
      menu.add(moveUp);

      JMenuItem moveDown = new JMenuItem(mLocalizer.msg("down", "Move the selected favorite down"),
          FavoritesPlugin.getInstance().getIconFromTheme("actions", "go-down", 16));

      moveDown.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          moveSelectedFavorite(1);
        }
      });

      moveDown.setEnabled((mFavoritesList.getSelectedIndex() != -1) && (mFavoritesList.getSelectedIndex() < (mFavoritesListModel.getSize() - 1)));
      menu.add(moveDown);
    }

    menu.addSeparator();

    JMenuItem sendPrograms = new JMenuItem(mLocalizer.msg("send", "Send Programs to another Plugin"),
        FavoritesPlugin.getInstance().getIconFromTheme("actions", "edit-copy", 16));

    sendPrograms.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showSendDialog();
      }
    });
    sendPrograms.setEnabled(mProgramListModel.size() > 0);

    menu.add(sendPrograms);

    menu.show(mFavoritesList, x, y);
  }

  public static ManageFavoritesDialog getInstance() {
    return mInstance;
  }

  public int getSplitpanePosition() {
    return mSplitPane.getDividerLocation();
  }

  public void setSplitpanePosition(int val) {
    mSplitPane.setDividerLocation(val);
  }
  
  /**
   * Refresh the program list.
   */
  public void favoriteSelectionChanged() {
    int selection = mFavoritesList.getSelectedIndex();
    int size = mFavoritesListModel.getSize();

    mEditBt.setEnabled(selection != -1);
    mDeleteBt.setEnabled(selection != -1);

    mUpBt.setEnabled(selection > 0);
    mDownBt.setEnabled((selection != -1) && (selection < (size - 1)));

    mSortBt.setEnabled(size >= 2);

    if (selection == -1) {
      mProgramListModel.clear();
      mSendBt.setEnabled(false);
    } else {
      Favorite fav = (Favorite) mFavoritesListModel.get(selection);
      Program[] programArr = mShowNew ? fav.getNewPrograms() : fav.getWhiteListPrograms();
      Program[] blackListPrograms = fav.getBlackListPrograms();
      
      mSendBt.setEnabled(programArr.length > 0);
      
      mProgramListModel.clear();
      mProgramListModel.ensureCapacity(mShowNew ? programArr.length : programArr.length + blackListPrograms.length);
      
      for (int i = 0; i < programArr.length; i++) {
        mProgramListModel.addElement(programArr[i]);
      }
      
      if(!mShowNew && mBlackListChb.isSelected())
        for (int i = 0; i < blackListPrograms.length; i++)
          mProgramListModel.addElement(blackListPrograms[i]);
      
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          mProgramScrollPane.getVerticalScrollBar().setValue(0);
          mProgramScrollPane.getHorizontalScrollBar().setValue(0);
        }
      });
    }
  }

  public void showSendDialog() {
    int selection = mFavoritesList.getSelectedIndex();

    if(selection == -1) {
        return;
    }

    Program[] programs = mProgramList.getSelectedPrograms();

    if(programs == null || programs.length == 0) {
      Favorite fav = (Favorite) mFavoritesListModel.get(selection);
      programs = mShowNew ? fav.getNewPrograms() : fav.getWhiteListPrograms();
    }

    ProgramReceiveIf caller = null;
    
    Favorite fav = (Favorite) mFavoritesListModel.get(selection);
    
    if(fav.getReminderConfiguration().containsService(ReminderConfiguration.REMINDER_DEFAULT))
      caller = ReminderPluginProxy.getInstance();
    
    SendToPluginDialog send = new SendToPluginDialog(caller, this, programs);

    send.setVisible(true);

}


  protected void newFavorite() {
    Favorite favorite;
    if (FavoritesPlugin.getInstance().isUsingExpertMode()) {
      favorite = new AdvancedFavorite("");
      EditFavoriteDialog dlg = new EditFavoriteDialog(this, favorite);
      UiUtilities.centerAndShow(dlg);

      if (!dlg.getOkWasPressed()) {
        favorite = null;
      }

    } else {
      WizardHandler handler = new WizardHandler(this, new TypeWizardStep());
      favorite = (tvbrowser.extras.favoritesplugin.core.Favorite)handler.show();
    }

    addFavorite(favorite);
  }

  public void addFavorite(Favorite fav) {
    if (fav != null) {
    try {
      fav.updatePrograms();
      mFavoritesListModel.addElement(fav);
      int idx = mFavoritesListModel.size() - 1;
      mFavoritesList.setSelectedIndex(idx);
      mFavoritesList.ensureIndexIsVisible(idx);
      FavoritesPlugin.getInstance().addFavorite(fav);
    } catch (TvBrowserException e) {
      ErrorHandler.handle("Creating favorites failed.", e);
    }
    }
  }

  public void addFavorite(Favorite fav, Object dummy) {
    mFavoritesListModel.addElement(fav);
  }

  protected void editSelectedFavorite() {
    Favorite fav = (Favorite) mFavoritesList.getSelectedValue();
    EditFavoriteDialog dlg = new EditFavoriteDialog(this, fav);
    UiUtilities.centerAndShow(dlg);
    if (dlg.getOkWasPressed()) {
      mFavoritesList.repaint();
      favoriteSelectionChanged();
      FavoritesPlugin.getInstance().updateRootNode();
    }
  }



  protected void deleteSelectedFavorite() {
    int selection = mFavoritesList.getSelectedIndex();
    if (selection != -1) {
      if (JOptionPane.showConfirmDialog(this,
              mLocalizer.msg("reallyDelete", "Really delete favorite?"),
              mLocalizer.msg("delete", "Delete selected favorite..."),
              JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        Favorite fav = (Favorite) mFavoritesListModel.get(selection);
        mFavoritesListModel.remove(selection);
        FavoritesPlugin.getInstance().deleteFavorite(fav);
      }
    }
  }



  protected void moveSelectedFavorite(int rowCount) {
    int selection = mFavoritesList.getSelectedIndex();
    if (selection != -1) {
      int targetPos = selection + rowCount;
      if ((targetPos >= 0) && (targetPos < mFavoritesListModel.size())) {
        Favorite fav = (Favorite) mFavoritesListModel.remove(selection);
        mFavoritesListModel.add(targetPos, fav);
        mFavoritesList.setSelectedIndex(targetPos);
        mFavoritesList.ensureIndexIsVisible(targetPos);
      }
    }
  }


  protected void sortFavorites() {
    String msg = mLocalizer.msg("reallySort", "Do you really want to sort your " +
        "favorites?\n\nThe current order will get lost.");
    String title = UIManager.getString("OptionPane.titleText");

    int result = JOptionPane.showConfirmDialog(this, msg, title, JOptionPane.YES_NO_OPTION);
    if (result == JOptionPane.YES_OPTION) {
      // Create a comparator for Favorites
      Comparator<Object> comp = new Comparator<Object>() {
        public int compare(Object o1, Object o2) {
          return ((Favorite)o1).getName().toLowerCase().compareTo(((Favorite)o2).getName().toLowerCase());
        }
      };

      // Sort the list
      
      Object[] asArr = mFavoritesListModel.toArray();
      Arrays.sort(asArr, comp);
      mFavoritesListModel.removeAllElements();
      for (int i = 0; i < asArr.length; i++) {
        mFavoritesListModel.addElement(asArr[i]);
      }

      // Update the buttons
      favoriteSelectionChanged();
    }
  }


  protected void importFavorites() {
    JFileChooser fileChooser = new JFileChooser();
    String[] extArr = { ".txt" };
    String msg = mLocalizer.msg("importFile.TVgenial", "Text file (from TVgenial) (.txt)");
    fileChooser.setFileFilter(new ExtensionFileFilter(extArr, msg));
    fileChooser.showOpenDialog(this);

    File file = fileChooser.getSelectedFile();
    if (file != null) {
      FileReader reader = null;
      int importedFavoritesCount = 0;
      try {
        reader = new FileReader(file);
        BufferedReader lineReader = new BufferedReader(reader);
        String line;
        while ((line = lineReader.readLine()) != null) {
          line = line.trim();
          if ((line.length() > 0) && (! line.startsWith("***"))) {
            // This is a favorite -> Check whether we already have such a favorite
            Enumeration en = mFavoritesListModel.elements();
            boolean alreadyKnown = false;
            while (en.hasMoreElements()) {
              Favorite fav = (Favorite) en.nextElement();
              String favName = fav.getName();
              if (line.equalsIgnoreCase(favName)) {
                alreadyKnown = true;
                break;
              }
            }

            // Import the favorite if it is new
            if (! alreadyKnown) {
              AdvancedFavorite fav = new AdvancedFavorite(line);
              fav.updatePrograms();

              mFavoritesListModel.addElement(fav);
              importedFavoritesCount++;
            }
          }
        }
      }
      catch (Exception exc) {
        msg = mLocalizer.msg("error.1", "Importing text file failed: {0}.",
                             file.getAbsolutePath());
        ErrorHandler.handle(msg, exc);
      }
      finally {
        if (reader != null) {
          try { reader.close(); } catch (IOException exc) {
            // ignore
          }
        }
      }

      if (importedFavoritesCount == 0) {
        msg = mLocalizer.msg("error.2", "There are no new favorites in {0}.",
                             file.getAbsolutePath());
        JOptionPane.showMessageDialog(this, msg);
      } else {
        // Scroll to the end
        mFavoritesList.ensureIndexIsVisible(mFavoritesListModel.size() - 1);

        // Select the first new fevorite
        int firstNewIdx = mFavoritesListModel.size() - importedFavoritesCount;
        mFavoritesList.setSelectedIndex(firstNewIdx);
        mFavoritesList.ensureIndexIsVisible(firstNewIdx);

        msg = mLocalizer.msg("importDone", "There were {0} new favorites imported.",
            new Integer(importedFavoritesCount));
        JOptionPane.showMessageDialog(this, msg);
      }
    }
  }

  public Favorite[] getFavorites() {
    Favorite[] favoriteArr = new Favorite[mFavoritesListModel.size()];
    mFavoritesListModel.copyInto(favoriteArr);
    return favoriteArr;
  }


  // inner class FavoriteListCellRenderer


  class FavoriteListCellRenderer extends DefaultListCellRenderer {
    
    public Component getListCellRendererComponent(JList list, Object value,
                                                  int index, boolean isSelected, boolean cellHasFocus)
    {
      Component c = super.getListCellRendererComponent(list, value, index, isSelected,
          cellHasFocus);
      
      if (value instanceof Favorite && c instanceof JLabel) {
        Favorite fav = (Favorite)value;
        ((JLabel)c).setText(fav.getName() + " (" + (mShowNew ? fav.getNewPrograms().length : fav.getWhiteListPrograms().length) + ")");
        
        if(mShowNew && fav.getNewPrograms().length > 0 && !isSelected)
          c.setForeground(Color.red);
      }
      return c;
    }

  }

  /*
   * (non-Javadoc)
   * @see util.ui.WindowClosingIf#close()
   */
  public void close() {
    mInstance = null;
    dispose();
  }

  /*
   * (non-Javadoc)
   * @see util.ui.ListDropAction#drop(javax.swing.JList, javax.swing.JList, int, boolean)
   */
  public void drop(JList source, JList target, int rows, boolean move) {
    UiUtilities.moveSelectedItems(target,rows,true);
  }  
}
