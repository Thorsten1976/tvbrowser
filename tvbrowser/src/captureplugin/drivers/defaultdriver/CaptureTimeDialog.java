/*
 * CapturePlugin by Andreas Hessel (Vidrec@gmx.de), Bodo Tasche
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
package captureplugin.drivers.defaultdriver;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import util.ui.Localizer;


/**
 * A Dialog that lets the user choose start/end time of the
 * recording
 */
public class CaptureTimeDialog extends JDialog {

    /** Translator */
    private static final Localizer mLocalizer = Localizer.getLocalizerFor(CaptureTimeDialog.class);

    /** ProgramTime*/
    private ProgramTime mPrgTime;
    /** Start-Time*/
    private TimeDateChooserPanel mStart;
    /** End-Time */
    private TimeDateChooserPanel mEnd;
    
    /**
     * Crate the Dialog
     * @param parent Parent-Frame
     * @param prgTime ProgramTime
     */
    public CaptureTimeDialog(JDialog parent, ProgramTime prgTime) {
        super(parent, true);
        mPrgTime = prgTime;
        createGui();
    }

    /**
     * Crate the Dialog
     * @param parent Parent-Frame
     * @param prgTime ProgramTime
     */
    public CaptureTimeDialog(JFrame parent, ProgramTime prgTime) {
        super(parent, true);
        mPrgTime = prgTime;
        createGui();
    }

    /**
     * Create the GUI
     */
    private void createGui() {
        
        setTitle(mLocalizer.msg("SetTime","Set Time"));
        
        JPanel panel = (JPanel)getContentPane();
        panel.setLayout(new BorderLayout());
        
        
        mStart = new TimeDateChooserPanel(mPrgTime.getStart());
        mStart.setBorder(BorderFactory.createTitledBorder(
                mLocalizer.msg("StartTime","Start-Time")));
        
        mEnd =  new TimeDateChooserPanel(mPrgTime.getEnd());
        mEnd.setBorder(BorderFactory.createTitledBorder(
                mLocalizer.msg("EndTime","End-Time")));
        

        JPanel center = new JPanel();
        center.setLayout(new GridBagLayout());
        
        GridBagConstraints c = new GridBagConstraints();
        
        c.weightx = 1.0;
        c.fill = GridBagConstraints.BOTH;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weighty = 0.5;
        
        JPanel namePanel = new JPanel();
        namePanel.setBorder(BorderFactory.createTitledBorder("Sendung:"));
        namePanel.setLayout(new BorderLayout());
        JLabel title = new JLabel(mPrgTime.getProgram().getTitle());
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        namePanel.add(title);
        
        center.add(namePanel, c);
        center.add(mStart, c);
        
        center.add(mStart, c);
        center.add(mEnd, c);
        
        panel.add(center, BorderLayout.CENTER);
        
        
        JPanel btPanel = new JPanel();
        
        btPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
       
        JButton ok = new JButton(mLocalizer.msg("OK","OK"));
        
        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mPrgTime.setStart(mStart.getDate());
                mPrgTime.setEnd(mEnd.getDate());
                hide();
            }
        });
        
        btPanel.add(ok);
        
        JButton cancel = new JButton(mLocalizer.msg("Cancel","Cancel"));
        
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mPrgTime = null;
                hide();
            }
            
        });

        
        btPanel.add(cancel);

        getRootPane().setDefaultButton(ok);
        
        panel.add(btPanel, BorderLayout.SOUTH);
        
        pack();
    }
    
    
    /**
     * Get the ProgramTime
     * @return ProgramTime
     */
    public ProgramTime getPrgTime() {
        return mPrgTime;
    }

}