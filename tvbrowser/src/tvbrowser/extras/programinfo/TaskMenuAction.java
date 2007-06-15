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

package tvbrowser.extras.programinfo;


import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.SwingConstants;

import util.ui.findasyoutype.TextComponentFindAction;

import com.l2fprod.common.swing.JLinkButton;
import com.l2fprod.common.swing.JTaskPaneGroup;

import devplugin.ActionMenu;
import devplugin.Program;

/**
 * A class that holds a ContextMenuAction of a Plugin.
 * 
 * @author Ren� Mach
 * 
 */
public class TaskMenuAction {

  private Action mAction;
  private ProgramInfoDialog mInfo;
  private TextComponentFindAction mFind;
  
  /**
   * @param parent
   *          The parent JTaskPaneGroup
   * @param program
   *          The Program for the Action.
   * @param menu
   *          The ActionMenu.
   * @param info
   *          The ProgramInfoDialog.
   * @param id
   *          The id of the Plugin.
   * @param comp
   *          The Text Component find action to register the keyListener on.
   */
  public TaskMenuAction(JTaskPaneGroup parent, Program program,
      ActionMenu menu, ProgramInfoDialog info, String id,
      TextComponentFindAction comp) {
    mInfo = info;
    mFind = comp;

    if (!menu.hasSubItems()) {
      addAction(parent, menu);
    } else {
      addTaskPaneGroup(parent, program, menu, info, id);
    }
  }

  // Adds the action to the TaskPaneGroup.
  private void addAction(JTaskPaneGroup parent, ActionMenu menu) {
    final Action a = menu.getAction();
    
    mAction = new AbstractAction() {
      private static final long serialVersionUID = 1L;

      public void actionPerformed(ActionEvent e) {
        a.actionPerformed(e);
        
        if (mAction.getValue(Action.ACTION_COMMAND_KEY) == null
            || !mAction.getValue(Action.ACTION_COMMAND_KEY).equals("action")) {
          mInfo.addPluginActions(true);
        }
      }      
    };
    
    mAction.putValue(Action.NAME,"<html>" + a.getValue(Action.NAME)+ "</html>");
    mAction.putValue(Action.ACTION_COMMAND_KEY,a.getValue(Action.ACTION_COMMAND_KEY));
    mAction.putValue(Action.SMALL_ICON,a.getValue(Action.SMALL_ICON));
    
    Component c = parent.add(mAction);
    mFind.installKeyListener(c);
    
    if(c instanceof JLinkButton) {
      ((JLinkButton)c).setVerticalTextPosition(SwingConstants.TOP);
      ((JLinkButton)c).setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
    }
  }

  /**
   * Adds a new TaskPaneGroup to the parent TaskPaneGroup for an ActionMenu with
   * submenus.
   */
  private void addTaskPaneGroup(JTaskPaneGroup parent,
      final Program program, final ActionMenu menu, final ProgramInfoDialog info,
      final String id) {
    final ActionMenu[] subs = menu.getSubItems();

    final JTaskPaneGroup group = new JTaskPaneGroup();
    group.setTitle((String) menu.getAction().getValue(Action.NAME));
    boolean expanded = ProgramInfo.getInstance().getExpanded(
        id + "_" + (String) menu.getAction().getValue(Action.NAME));
    group.setExpanded(expanded);
    group.setEnabled(true);
    mFind.installKeyListener(group);
    
    /*
     * Listener to get expand state changes and store the state in the
     * Properties for the Plugins menu.
     */
    group.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        ProgramInfo.getInstance().setExpanded(
            id + "_" + (String) menu.getAction().getValue(Action.NAME),
            group.isExpanded());
      }
    });

    if (menu.getAction().getValue(Action.SMALL_ICON) != null) {
      group.setIcon((Icon) menu.getAction().getValue(Action.SMALL_ICON));
    }

    // delay group creation if it is not expanded
    if (expanded) {
      for (ActionMenu subMenu : subs) {
        new TaskMenuAction(group, program, subMenu, info, id, mFind);
      }
    }
    else {
      Thread thread = new Thread("Lazy task menus") {
        @Override
        public void run() {
          for (ActionMenu subMenu : subs) {
            new TaskMenuAction(group, program, subMenu, info, id, mFind);
          }
        }
      };
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    }
    parent.add(Box.createRigidArea(new Dimension(0, 10)));
    parent.add(group);
    parent.add(Box.createRigidArea(new Dimension(0, 5)));
  }
  
  protected void setText(String value) {
    mAction.putValue(Action.NAME, "<html>" + value + "</html>");
  }
}
