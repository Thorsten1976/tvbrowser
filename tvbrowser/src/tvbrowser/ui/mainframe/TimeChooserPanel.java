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
 
 
package tvbrowser.ui.mainframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import tvbrowser.core.Settings;
import util.ui.GridFlowLayout;


public class TimeChooserPanel extends JPanel {
    
    /** The localizer for this class. */
    public static final util.ui.Localizer mLocalizer
        = util.ui.Localizer.getLocalizerFor(TimeChooserPanel.class);

    
    private MainFrame mParent;
    private JPanel mTimeBtnPanel;
    
    private JPanel mGridPn;
    
    public TimeChooserPanel(MainFrame parent) {
      setOpaque(false);
      mParent=parent;
      setLayout(new BorderLayout(0,7));
      setBorder(BorderFactory.createEmptyBorder(5,3,5,3));
      
      mGridPn = new JPanel(new GridFlowLayout(5,5,GridFlowLayout.TOP, GridFlowLayout.CENTER));
      add(mGridPn,BorderLayout.CENTER);
      
      
      createContent();
      
      String msg;
      msg = mLocalizer.msg("button.now", "Now");
      JButton nowBt=new JButton(msg);
      nowBt.addActionListener(new ActionListener(){
        public void actionPerformed(ActionEvent arg0) {
            mParent.scrollToNow();    
        }});      
      add(nowBt, BorderLayout.SOUTH);
 
    }
    
    public void updateButtons() {
      createContent();
    }
    
    private void createContent() {
      mGridPn.removeAll();
      
      int[] times = Settings.propTimeButtons.getIntArray();
      
      for (int i=0; i<times.length; i++) {
        final int time = times[i];
        int h = time/60;
        int m = time%60;
        String title = h+":"+(m<10?"0":"")+m;
        JButton btn = new JButton(title);
        mGridPn.add(btn);
        btn.addActionListener(new ActionListener(){
          public void actionPerformed(ActionEvent arg0) {
            mParent.scrollToTime(time);
          }
        });
      }
        
     

      
    }
    
   

    
    
  }