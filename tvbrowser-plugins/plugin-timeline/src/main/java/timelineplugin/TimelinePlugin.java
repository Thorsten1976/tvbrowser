/*
 * Timeline by Reinhard Lehrbaum
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
 */
package timelineplugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import timelineplugin.format.TextFormatter;
import util.ui.UiUtilities;
import util.ui.persona.Persona;
import devplugin.ActionMenu;
import devplugin.Channel;
import devplugin.Date;
import devplugin.Plugin;
import devplugin.PluginCenterPanel;
import devplugin.PluginCenterPanelWrapper;
import devplugin.PluginInfo;
import devplugin.Program;
import devplugin.ProgramFilter;
import devplugin.SettingsTab;
import devplugin.TvBrowserSettings;
import devplugin.Version;

public final class TimelinePlugin extends devplugin.Plugin {
	static final util.ui.Localizer mLocalizer = util.ui.Localizer
			.getLocalizerFor(TimelinePlugin.class);

	private static final Version VERSION = new Version(1, 13, 2, false);
	
	private static TimelinePlugin mInstance;

	private static final Font DEFAULT_FONT = new Font("Dialog", Font.PLAIN, 12);

	private TimelineDialog mDialog;
	private Date mChoosenDate;
	private TextFormatter mFormatter;
	private ProgramFilter mFilter;

	private int mChannelWidth = -1;

	private TimelineSettings mSettings;

	private String mTitleFormat;
	
  private PluginCenterPanelWrapper mWrapper;
  
  private JPanel mCenterPanelWrapper;
  
  private TimelinePanel mTimelinePanel;
  
  private boolean mIsTvBrowserStarted;
  
  static private Color mOnAirLight;
  static private Color mOnAirDark;
  static private Color mProgramTableMouseOverColor;
  static private Color mProgramPanelSelectionColor;
  static private Color mForegroundColor;

	public TimelinePlugin() {
		mInstance = this;
		mIsTvBrowserStarted = false;
	  mProgramTableMouseOverColor = null;
	  mProgramPanelSelectionColor = null;
	  mForegroundColor = null;
	}

	public static TimelinePlugin getInstance() {
		return mInstance;
	}

	public PluginInfo getInfo() {
		final String name = mLocalizer.msg("name", "Timeline");
		final String desc = mLocalizer.msg("description",
				"Timeline view of the program data.");
		final String author = "Reinhard Lehrbaum";

		return new PluginInfo(TimelinePlugin.class, name, desc, author);
	}

	public static Version getVersion() {
		return VERSION;
	}

	public SettingsTab getSettingsTab() {
		return (new TimelinePluginSettingsTab());
	}

  public void onActivation() {
    handleTvBrowserSettingsChanged();
    
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        mCenterPanelWrapper = UiUtilities.createPersonaBackgroundPanel();
      }
    });
    
    mWrapper = new PluginCenterPanelWrapper() {
      @Override
      public PluginCenterPanel[] getCenterPanels() {
        return new PluginCenterPanel[] {new TimelineCenterPanel()};
      }
      
      public void scrolledToChannel(Channel channel) {
        if(mTimelinePanel != null) {
          mTimelinePanel.scrollToChannel(channel);
        }
      }
      
      public void filterSelected(ProgramFilter filter) {
        if(mTimelinePanel != null) {
          mTimelinePanel.setFilter(filter);
        }
      }
      
      public void scrolledToNow() {
        if(mTimelinePanel != null) {
          mTimelinePanel.gotoNowLock();
        }
      }
      
      public void scrolledToDate(final Date date) {
        if(mTimelinePanel != null) {
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              mTimelinePanel.gotoDate(date);              
            }
          });
        }
      }
      
      public void scrolledToTime(int time) {
        if(mTimelinePanel != null) {
          mTimelinePanel.scrollToTime(time);
        }
      }
      
      public void programScrolled(Program prog) {
        if(mTimelinePanel != null) {
          mTimelinePanel.scrollToProgram(prog);
        }
      }
    };
    
    addCenterPanel();
  }
  
  void resetFormatter() {
    mFormatter = new TextFormatter();
    mFormatter.setFont(getFont());
    mFormatter.setInitialiseMaxLine(true);
    mFormatter.setFormat(mSettings.getTitleFormat());
  }
  
  private void addCenterPanel() {
    new Thread() {
      public void run() {
        while(!mIsTvBrowserStarted) {
          try {
            sleep(100);
          } catch (InterruptedException e) {}
        }
        
        try {
          sleep(1000);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        
        resetFormatter();

        setChoosenDate(Date.getCurrentDate());
        
        if(mSettings.showHeaderPanel()) {
          mCenterPanelWrapper.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        }
                
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            mTimelinePanel = new TimelinePanel(mSettings.startWithNow(),mSettings.showHeaderPanel());
            mCenterPanelWrapper.addAncestorListener(new AncestorListener() {
              private InputMap mDefaultInputMap;
              private ActionMap mDefaultActionMap;
              
              @Override
              public void ancestorRemoved(AncestorEvent e) {
                for(KeyStroke keyStroke : mDefaultInputMap.keys()) {
                  ((JFrame)getParentFrame()).getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, mDefaultInputMap.get(keyStroke));
                }
                
                for(Object o : mDefaultActionMap.keys()) {
                  ((JFrame)getParentFrame()).getRootPane().getActionMap().put(o, mDefaultActionMap.get(o));
                }
              }
              
              @Override
              public void ancestorMoved(AncestorEvent e) {}
              
              @Override
              public void ancestorAdded(AncestorEvent e) {
                if(mDefaultInputMap == null) {
                  mDefaultInputMap = new InputMap();
                  mDefaultActionMap = new ActionMap();
                  
                  for(KeyStroke keyStroke : ((JFrame)getParentFrame()).getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).keys()) {
                    mDefaultInputMap.put(keyStroke, ((JFrame)getParentFrame()).getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(keyStroke));
                  }
                  
                  for(Object o : ((JFrame)getParentFrame()).getRootPane().getActionMap().keys()) {
                    mDefaultActionMap.put(o, ((JFrame)getParentFrame()).getRootPane().getActionMap().get(o));
                  }
                }
                
                mTimelinePanel.addKeyboardAction(((JFrame)getParentFrame()).getRootPane());
              }
            });
            
            Persona.getInstance().registerPersonaListener(mTimelinePanel);

            mCenterPanelWrapper.add(mTimelinePanel,BorderLayout.CENTER);
            
            mCenterPanelWrapper.updateUI();
            
            mTimelinePanel.updatePersona();
          }
        });
      }
    }.start();
  }
	
	public void onDeactivation() {
		if (mDialog != null && mDialog.isVisible()) {
			mDialog.dispose();
		}
	}

	public ActionMenu getButtonAction() {
		final AbstractAction action = new AbstractAction() {
			public void actionPerformed(final ActionEvent evt) {
				showTimeline();
			}
		};

		action.putValue(Action.NAME, mLocalizer.msg("name", "Timeline"));
		action.putValue(Action.SMALL_ICON,
				createImageIcon("actions", "timeline", 16));
		action.putValue(BIG_ICON, createImageIcon("actions", "timeline", 22));

		return new ActionMenu(action);
	}

	void showTimeline() {
		if (mDialog != null && mDialog.isVisible()) {
			mDialog.dispose();
		}
		
		mDialog = new TimelineDialog(getParentFrame(), mSettings.startWithNow());
		mDialog.pack();

		final Rectangle rect = mSettings.getPosition();
		mDialog.setBounds(rect);
		mDialog.setVisible(true);
		savePosition();
	}

	public void handleTvBrowserStartFinished() {
	  mIsTvBrowserStarted = true;
		if (mSettings.showAtStartUp()) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					showTimeline();
				}
			});
		}
	}

	double getSizePerMinute() {
		return mSettings.getHourWidth() / 60.0;
	}

	int getChannelWidth() {
		if (mChannelWidth < 0) {
			final JLabel l = new JLabel();
			final FontMetrics fm = l.getFontMetrics(getFont());
			int neededWidth = 0;
			if (mSettings.showChannelName()) {
				final Channel[] mChannels = Plugin.getPluginManager()
						.getSubscribedChannels();
				for (Channel mChannel : mChannels) {
					final int width = fm.stringWidth(mChannel.getName());
					if (neededWidth < width) {
						neededWidth = width;
					}
				}
				neededWidth += 10;
			}
			mChannelWidth = neededWidth + (mSettings.showChannelIcon() ? 42 : 0);
		}
		return mChannelWidth;
	}

	void setChannelWidth(final int value) {
		mChannelWidth = value;
	}

	int getOffset() {
		return mSettings.getHourWidth();
	}

	void resetChannelWidth() {
		mChannelWidth = -1;
	}

	static Font getFont() {
		return DEFAULT_FONT;
	}

	Date getChoosenDate() {
		return mChoosenDate;
	}

	void setChoosenDate(final Date d) {
		mChoosenDate = d;
	}

	static int getNowMinute() {
		final Calendar now = Calendar.getInstance();
		return now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
	}

	int getNowPosition() {
		return getOffset() + (int) Math.round(getSizePerMinute() * getNowMinute());
	}

	void setFilter(final ProgramFilter filter) {
		mFilter = filter;
	}

	ProgramFilter getFilter() {
		return mFilter;
	}

	TextFormatter getFormatter() {
		return mFormatter;
	}

	void resize() {
	  if(mDialog != null && mDialog.isVisible()) {
	    mDialog.resize();
	  }
	  
	  if(mTimelinePanel != null) {
	    mTimelinePanel.resize();
	  }
	}

	private void savePosition() {
		mSettings.savePosition(mDialog.getX(), mDialog.getY(), mDialog.getWidth(),
				mDialog.getHeight());
	}

	public void loadSettings(final Properties prop) {
		mSettings = new TimelineSettings(prop);
		if (mTitleFormat != null) {
		  mSettings.setTitleFormat(mTitleFormat);
		}
		mChannelWidth = mSettings.getChannelWidth();
	}

	public Properties storeSettings() {
		return mSettings.storeSettings();
	}

	public void readData(final ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		final int version = in.readInt();

		if (version == 1) {
			mTitleFormat = (String) in.readObject();
		}
	}

	public void writeData(final ObjectOutputStream out) throws IOException {
		out.writeInt(1); // version
		out.writeObject(mSettings.getTitleFormat());
	}

	public static TimelineSettings getSettings() {
		return getInstance().mSettings;
	}
	
	public String getPluginCategory() {
	  return Plugin.OTHER_CATEGORY;
	}

	public PluginCenterPanelWrapper getPluginCenterPanelWrapper() {
	  return mWrapper;
	}
	
	private class TimelineCenterPanel extends PluginCenterPanel {
    @Override
    public String getName() {
      return getInfo().getName();
    }

    @Override
    public JPanel getPanel() {
      return mCenterPanelWrapper;
    }

	}
	
  public static void paintComponentInternal(Graphics g,JComponent component) {
    Color c = Persona.getInstance().getAccentColor().darker().darker().darker();

    g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),110));
    g.fillRect(0,0,component.getWidth(),component.getHeight());
    
    c = Persona.getInstance().getAccentColor();
    
    double test = (0.2126 * Persona.getInstance().getTextColor().getRed()) + (0.7152 * Persona.getInstance().getTextColor().getGreen()) + (0.0722 * Persona.getInstance().getTextColor().getBlue());
    int alpha = 100;
    
    if(test <= 30) {
      c = Color.white;
      alpha = 200;
    }
    else if(test <= 40) {
      c = c.brighter().brighter().brighter().brighter().brighter().brighter();
      alpha = 200;
    }
    else if(test <= 60) {
      c = c.brighter().brighter().brighter();
      alpha = 160;
    }
    else if(test <= 100) {
      c = c.brighter().brighter();
      alpha = 140;
    }
    else if(test <= 145) {
      alpha = 120;
    }
    else if(test <= 170) {
      c = c.darker();
      alpha = 120;
    }
    else if(test <= 205) {
      c = c.darker().darker();
      alpha = 120;
    }
    else if(test <= 220){
      c = c.darker().darker().darker();
      alpha = 100;
    }
    else if(test <= 235){
      c = c.darker().darker().darker().darker();
      alpha = 100;
    }
    else {
      c = Color.black;
      alpha = 100;
    }
    
    g.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),alpha));
    g.fillRect(0,0,component.getWidth(),component.getHeight());
  }
  
  void deselectProgram() {
    if(mTimelinePanel != null) {
      mTimelinePanel.scrollToProgram(null);
    }
  }
  
  static Color getOnAirLight() {
    return mOnAirLight;
  }
  
  static Color getOnAirDark() {
    return mOnAirDark;
  }
  
  static Color getProgramTableMouseOverColor() {
    return mProgramTableMouseOverColor;
  }
  
  static Color getProgramPanelSelectionColor() {
    return mProgramPanelSelectionColor;
  }
  
  static Color getForegroundColor() {
    return mForegroundColor;
  }
  
  public void handleTvBrowserSettingsChanged() {
    mOnAirLight = Plugin.getPluginManager().getTvBrowserSettings().getProgramPanelOnAirLightColor();
    mOnAirDark = Plugin.getPluginManager().getTvBrowserSettings().getProgramPanelOnAirDarkColor();
    TvBrowserSettings tvbSet = Plugin.getPluginManager().getTvBrowserSettings();
    
    mProgramTableMouseOverColor = null;
    mProgramPanelSelectionColor = null;
    mForegroundColor = null;
    
    try {
      Method colorMethod = TvBrowserSettings.class.getMethod("getProgramTableMouseOverColor", new Class[0]);
      Object o = colorMethod.invoke(tvbSet, new Object[0]);
      
      if(o instanceof Color) {
        mProgramTableMouseOverColor = (Color)o;
      }
      
      colorMethod = TvBrowserSettings.class.getMethod("getProgramPanelSelectionColor", new Class[0]);
      
      o = colorMethod.invoke(tvbSet, new Object[0]);
      
      if(o instanceof Color) {
        mProgramPanelSelectionColor = (Color)o;
      }
      
      colorMethod = TvBrowserSettings.class.getMethod("getProgramTableForegroundColor", new Class[0]);
      
      o = colorMethod.invoke(tvbSet, new Object[0]);
      
      if(o instanceof Color) {
        mForegroundColor = (Color)o;
      }
    } catch (Exception e) {
      mForegroundColor = UIManager.getColor("List.foreground");
      mProgramPanelSelectionColor = new Color(130, 255, 0, 120);
    }
  }
}
