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

package util.program;

import java.awt.Color;
import java.awt.Font;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.UIManager;

import org.apache.commons.lang3.StringUtils;

import tvbrowser.core.plugin.PluginManagerImpl;
import tvbrowser.core.plugin.PluginProxy;
import tvbrowser.core.plugin.PluginProxyManager;
import tvbrowser.extras.common.InternalPluginProxyIf;
import tvbrowser.extras.common.InternalPluginProxyList;
import tvbrowser.extras.favoritesplugin.FavoritesPluginProxy;
import tvbrowser.extras.favoritesplugin.core.Favorite;
import tvbrowser.extras.favoritesplugin.dlgs.FavoriteTreeModel;
import util.settings.PluginPictureSettings;
import util.settings.ProgramPanelSettings;
import util.ui.Localizer;
import util.ui.UiUtilities;
import util.ui.html.ExtendedHTMLDocument;
import util.ui.html.HTMLTextHelper;
import util.ui.html.HorizontalLine;
import devplugin.Date;
import devplugin.Marker;
import devplugin.Plugin;
import devplugin.PluginAccess;
import devplugin.Program;
import devplugin.ProgramFieldType;
import devplugin.ProgramInfoHelper;
import devplugin.ToolTipIcon;
import devplugin.UniqueIdNameGenericValue;

/**
 * Creates the String for the ProgramInfoDialog
 */
public class ProgramTextCreator {

  /**
   * The Localizer for this class.
   */
  private static final util.ui.Localizer mLocalizer = util.ui.Localizer
      .getLocalizerFor(ProgramTextCreator.class);

  private static String mBodyFontSize;

  /** The used link protocol for actor links */
  public static final String TVBROWSER_URL_PROTOCOL = "tvbrowser://";


  /**
   *
   * @param prog
   *          The Program to show
   * @param doc
   *          The HTMLDocument.
   * @param fieldArr The object array with the field types.
   * @param tFont The title Font.
   * @param bFont The body Font.
   * @param showImage If the image should be shown if it is available.
   * @param showHelpLinks Show the Help-Links (Quality of Data, ShowView)
   * @return The HTML String.
   */
  public static String createInfoText(Program prog, ExtendedHTMLDocument doc,
      Object[] fieldArr, Font tFont, Font bFont, boolean showImage, boolean showHelpLinks) {
    return createInfoText(prog,doc,fieldArr,tFont,bFont,new ProgramPanelSettings(showImage ? ProgramPanelSettings.SHOW_PICTURES_EVER : ProgramPanelSettings.SHOW_PICTURES_NEVER, -1, -1, false, true, 10),showHelpLinks, 100);
  }

  /**
   *
   * @param prog
   *          The Program to show
   * @param doc
   *          The HTMLDocument.
   * @param fieldArr
   *          The object array with the field types.
   * @param tFont
   *          The title Font.
   * @param bFont
   *          The body Font.
   * @param settings
   *          Settings of the ProgramPanel
   * @param showHelpLinks
   *          Show the Help-Links (Quality of Data, ShowView)
   * @param zoom The zoom value for the picture.
   * @return The HTML String.
   * @since 2.2.2
   */
  public static String createInfoText(Program prog, ExtendedHTMLDocument doc,
      Object[] fieldArr, Font tFont, Font bFont, ProgramPanelSettings settings,
      boolean showHelpLinks, int zoom) {
    return createInfoText(prog,doc,fieldArr,tFont,bFont,settings,showHelpLinks, zoom, true);
  }

  /**
   *
   * @param prog
   *          The Program to show
   * @param doc
   *          The HTMLDocument.
   * @param fieldArr
   *          The object array with the field types.
   * @param tFont
   *          The title Font.
   * @param bFont
   *          The body Font.
   * @param settings
   *          Settings of the ProgramPanel
   * @param showHelpLinks
   *          Show the Help-Links (Quality of Data, ShowView)
   * @param zoom The zoom value for the picture.
   * @return The HTML String.
   * @since 2.6
   */
  public static String createInfoText(Program prog, ExtendedHTMLDocument doc,
      Object[] fieldArr, Font tFont, Font bFont, PluginPictureSettings settings,
      boolean showHelpLinks, int zoom) {
    return createInfoText(prog,doc,fieldArr,tFont,bFont,new ProgramPanelSettings(settings,false),showHelpLinks, zoom, true);
  }

  /**
   *
   * @param prog
   *          The Program to show
   * @param doc
   *          The HTMLDocument.
   * @param fieldArr
   *          The object array with the field types.
   * @param tFont
   *          The title Font.
   * @param bFont
   *          The body Font.
   * @param settings
   *          Settings of the ProgramPanel
   * @param showHelpLinks
   *          Show the Help-Links (Quality of Data, ShowView)
   * @param zoom The zoom value for the picture.
   * @param showPluginIcons If the plugin icons should be shown.
   * @return The HTML String.
   * @since 2.5.3
   */
  public static String createInfoText(Program prog, ExtendedHTMLDocument doc,
      Object[] fieldArr, Font tFont, Font bFont, ProgramPanelSettings settings,
      boolean showHelpLinks, int zoom, boolean showPluginIcons) {
    return createInfoText(prog, doc, fieldArr, tFont, bFont, settings,
        showHelpLinks, zoom, showPluginIcons, true);
  }

  /**
  *
  * @param prog
  *          The Program to show
  * @param doc
  *          The HTMLDocument.
  * @param fieldArr
  *          The object array with the field types.
  * @param tFont
  *          The title Font.
  * @param bFont
  *          The body Font.
  * @param settings
  *          Settings of the ProgramPanel
  * @param showHelpLinks
  *          Show the Help-Links (Quality of Data, ShowView)
  * @param zoom
  *          The zoom value for the picture.
  * @param showPluginIcons
  *          If the plugin icons should be shown.
  * @return The HTML String.
  * @since 3.0
  */
 public static String createInfoText(Program prog, ExtendedHTMLDocument doc,
     Object[] fieldArr, Font tFont, Font bFont, ProgramPanelSettings settings,
     boolean showHelpLinks, int zoom, boolean showPluginIcons,
     boolean showPersonLinks) {
   return createInfoText(prog,doc,fieldArr,tFont,bFont,settings,showHelpLinks,zoom,showPluginIcons,showPersonLinks,false);
 }
  
  /**
   *
   * @param prog
   *          The Program to show
   * @param doc
   *          The HTMLDocument.
   * @param fieldArr
   *          The object array with the field types.
   * @param tFont
   *          The title Font.
   * @param bFont
   *          The body Font.
   * @param settings
   *          Settings of the ProgramPanel
   * @param showHelpLinks
   *          Show the Help-Links (Quality of Data, ShowView)
   * @param zoom
   *          The zoom value for the picture.
   * @param showPluginIcons
   *          If the plugin icons should be shown.
   * @return The HTML String.
   * @since 3.1
   */
  public static String createInfoText(Program prog, ExtendedHTMLDocument doc,
      Object[] fieldArr, Font tFont, Font bFont, ProgramPanelSettings settings,
      boolean showHelpLinks, int zoom, boolean showPluginIcons,
      boolean showPersonLinks, boolean useThemeColors) {
    Color foreground = Color.black;
    Color background = Color.white;
    Color infoColor = Color.gray;
    Color episodeColor = new Color(0x0,0x33,0x66);
    
    if(useThemeColors) {
      foreground = UIManager.getColor("List.foreground");
      background = UIManager.getColor("List.background");
      
      int r = (foreground.getRed()   + background.getRed()) >> 1;
      int g = (foreground.getGreen() + background.getGreen()) >> 1;
      int b = (foreground.getBlue()  + background.getBlue()) >> 1;
      
      infoColor = new Color(r,g,b);
      
      double testBackground = (0.2126 * background.getRed()) + (0.7152 * background.getGreen()) + (0.0722 * background.getBlue());
      double testInfoColor = (0.2126 * infoColor.getRed()) + (0.7152 * infoColor.getGreen()) + (0.0722 * infoColor.getBlue());
      double testInfoEpisode = (0.2126 * episodeColor.getRed()) + (0.7152 * episodeColor.getGreen()) + (0.0722 * episodeColor.getBlue());
      
      if(testBackground - testInfoColor > 90) {
        infoColor = new Color(infoColor.getRed()+30,infoColor.getGreen()+30,infoColor.getBlue()+30);
      }
      
      if(testBackground - testInfoEpisode < -30) {
        episodeColor = episodeColor.brighter().brighter();
      }
    }
    
    String debugTables = "0"; //set to "1" for debugging, to "0" for no debugging
    try {
    // NOTE: All field types are included until type 25 (REPETITION_ON_TYPE)
      StringBuilder buffer = new StringBuilder(1024);

    String titleFont, titleSize, bodyFont;

    int bodyStyle;
    int titleStyle;
    if (tFont == null && bFont != null) {
      titleFont = bodyFont = bFont.getFamily();
      titleSize = mBodyFontSize = String.valueOf(bFont.getSize());
      titleStyle = bodyStyle = bFont.getStyle();
    } else if (tFont != null && bFont != null) {
      titleFont = tFont.getFamily();
      bodyFont = bFont.getFamily();
      titleSize = String.valueOf(tFont.getSize());
      mBodyFontSize = String.valueOf(bFont.getSize());
      titleStyle = tFont.getStyle();
      bodyStyle = bFont.getStyle();
    } else {
      return null;
    }

    if (fieldArr == null) {
      return null;
    }

    buffer.append("<html>");
    buffer.append("<table width=\"100%\" border=\"" + debugTables + "\" style=\"font-family:");

    buffer.append(bodyFont);

    buffer.append(";").append("background-color:").append(HTMLTextHelper.getCssRgbColorEntry(background)).append(";").append(getCssStyle(bodyStyle)).append("\"><tr>");
    buffer.append("<td width=\"60\">");
    buffer.append("<p \"align=center\">");

    JLabel channelLogo = new JLabel(prog.getChannel().getIcon());
    channelLogo.setBackground(Color.white);
    channelLogo.setOpaque(true);
    channelLogo.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    channelLogo.setToolTipText(prog.getChannel().getName());
    buffer.append(doc.createCompTag(channelLogo));

    buffer.append("</p></td><td><table width=\"100%\" border=\""+ debugTables +"\" cellpadding=\"0\"><tr><td>");
    buffer.append("<div style=\"color:#ff0000; font-size:");

    buffer.append(mBodyFontSize);

    buffer.append(";\"><b>");

    Date currentDate = Date.getCurrentDate();
    Date programDate = prog.getDate();
    if(programDate.equals(currentDate.addDays(-1))) {
      buffer.append(Localizer.getLocalization(Localizer.I18N_YESTERDAY));
      buffer.append(" · ");
    }
    else if(programDate.equals(currentDate)){
      buffer.append(Localizer.getLocalization(Localizer.I18N_TODAY));
      buffer.append(" · ");
    }
    else if(programDate.equals(currentDate.addDays(1))){
      buffer.append(Localizer.getLocalization(Localizer.I18N_TOMORROW));
      buffer.append(" · ");
    }
    buffer.append(prog.getDateString());

    buffer.append(" · ");
    buffer.append(prog.getTimeString());
    if (prog.getLength() > 0) {
      buffer.append('-');
      buffer.append(prog.getEndTimeString());
    }
    buffer.append(" · ");
    buffer.append(prog.getChannel());

    buffer.append("</b></div>");
    String seriesField = prog.getTextField(ProgramFieldType.SERIES_TYPE);
    if (seriesField != null) {
      buffer.append("<div style=\"color:"+HTMLTextHelper.getCssRgbColorEntry(episodeColor)+"; margin-top:1em; font-size:");

      buffer.append(mBodyFontSize);

      buffer.append(";\"><b>");
      buffer.append(HTMLTextHelper.convertTextToHtml(seriesField, false));
      buffer.append("</b></div>");
      
    }
    buffer.append("<div style=\"color:"+HTMLTextHelper.getCssRgbColorEntry(episodeColor)+"; font-size:");

    buffer.append(titleSize);

    buffer.append("; margin-top:0.5em; margin-bottom:0.5em; font-family:");
    buffer.append(titleFont).append(";").append(getCssStyle(titleStyle));
    buffer.append("\">");
    buffer.append(prog.getTitle());
    buffer.append("</div>");

    String episode = CompoundedProgramFieldType.EPISODE_COMPOSITION.getFormattedValueForProgram(prog);

    if (episode != null && episode.trim().length() > 0) {
      buffer.append("<div style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoColor)).append("; font-size:");

      buffer.append(mBodyFontSize);

      buffer.append("\">");
      buffer.append(episode);
      buffer.append("</div>");
    }

    buffer.append("</td><td align=\"right\" valign=\"top\"><table border=\"" + debugTables +"\" style=\"font-size:0\">");
    buffer.append("</table></td></tr></table></td></tr>");

    boolean show = false;

    if(settings.isShowingPictureForPlugins()) {
      String[] pluginIds = settings.getPluginIds();
      Marker[] markers = prog.getMarkerArr();

      if(markers != null && pluginIds != null) {
        for (Marker marker : markers) {
          for (String pluginId : pluginIds) {
            if(marker.getId().compareTo(pluginId) == 0) {
              show = true;
              break;
            }
          }
        }
      }
    }

    if(settings.isShowingPictureEver() ||
      (settings.isShowingPictureInTimeRange() && !ProgramUtilities.isNotInTimeRange(settings.getPictureTimeRangeStart(),settings.getPictureTimeRangeEnd(), prog)) ||
      show || (settings.isShowingPictureForDuration() && settings.getDuration() <= prog.getLength())) {
      byte[] image = prog.getBinaryField(ProgramFieldType.PICTURE_TYPE);
      if (image != null) {
        String line = "<tr><td></td><td valign=\"top\" style=\"color:"+HTMLTextHelper.getCssRgbColorEntry(foreground)+"; font-size:0\">";
        buffer.append(line);
        try {
          ImageIcon imageIcon = new ImageIcon(image);

          if(zoom != 100) {
            imageIcon = (ImageIcon)UiUtilities.scaleIcon(imageIcon, imageIcon.getIconWidth() * zoom/100);
          }

          StringBuilder value = new StringBuilder();

          String textField = prog.getTextField(ProgramFieldType.PICTURE_COPYRIGHT_TYPE);
          
          if (textField != null) {
            if(textField.toLowerCase().startsWith("(c)")) {
              textField = "\u00A9" + textField.substring(3);
            }
            
            value.append(textField);
          }

          if (settings.isShowingPictureDescription()) {
              textField = prog
                  .getTextField(ProgramFieldType.PICTURE_DESCRIPTION_TYPE);
              if (textField != null) {
                value.append("<br>").append(textField);
              }
          }

          buffer.append(doc.createCompTag(new JLabel(imageIcon)));
          buffer.append("<div style=\"font-size:");

          buffer.append(mBodyFontSize);

          buffer.append("\">");
          buffer.append(value);
          buffer.append("</div>");
          buffer.append("</td></tr>");
        } catch (Exception e) {
          // Picture was wrong;
          buffer.delete(buffer.length() - line.length(), buffer.length());
        }
      }
    }

    Marker[] pluginArr = prog.getMarkerArr();
    if (showPluginIcons && (pluginArr != null) && (pluginArr.length != 0)) {
      addSeparator(doc, buffer);

      buffer.append("<tr><td valign=\"top\" style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoColor)).append("; font-size:");

      buffer.append(mBodyFontSize);

      buffer.append("\"><b>");
      buffer.append(mLocalizer.msg("markedBy", "Marked by"));
      buffer.append("</b></td><td valign=\"middle\" style=\"font-size:4\">");
      openPara(buffer, "info");

      // Workaround: Without the &nbsp; the component are not put in one line.
      buffer.append("&nbsp;");
      for (int markerCount = pluginArr.length-1; markerCount >= 0; markerCount--) {
        Icon[] icons = pluginArr[markerCount].getMarkIcons(prog);

        if (icons != null) {
          for(int i = icons.length - 1; i >= 0 ; i--) {
            JLabel iconLabel = new JLabel(icons[i]);
            PluginAccess plugin = Plugin.getPluginManager()
                .getActivatedPluginForId(pluginArr[markerCount].getId());
            if (plugin != null) {
              iconLabel.setToolTipText(plugin.getInfo().getName());
            }
            else {
              InternalPluginProxyIf internalPlugin = InternalPluginProxyList.getInstance().getProxyForId(pluginArr[markerCount].getId());
              if (internalPlugin != null) {
                iconLabel.setToolTipText(internalPlugin.getName());
                if (internalPlugin.equals(FavoritesPluginProxy.getInstance())) {
                  // if this is a favorite, add the names of the favorite
                  StringBuilder favTitles = new StringBuilder();
                  for (Favorite favorite : FavoriteTreeModel.getInstance().getFavoritesContainingProgram(prog)) {
                    if (favTitles.length() > 0) {
                      favTitles.append(", ");
                    }
                    favTitles.append(favorite.getName());
                  }
                  if (favTitles.length() > 0) {
                    iconLabel.setToolTipText(iconLabel.getToolTipText() + " (" + favTitles.toString() + ")");
                  }
                }
              }
              else {
                iconLabel.setToolTipText(pluginArr[markerCount].toString());
              }
            }

            buffer.append(doc.createCompTag(iconLabel));
            buffer.append("&nbsp;&nbsp;");
          }
        }
      }
      closePara(buffer);
      buffer.append("</td></tr>");
    }

    PluginAccess[] plugins = Plugin.getPluginManager().getActivatedPlugins();
    ArrayList<JLabel> iconLabels = new ArrayList<JLabel>();
    for (PluginAccess plugin : plugins) {
      Icon[] icons = plugin.getProgramTableIcons(prog);
      ToolTipIcon[] toolTips = plugin.getProgramTableToolTipIcons(prog);
      String singleText = plugin.getProgramTableIconText();

      if (icons != null) {
        for (int i = 0; i < icons.length; i++) {
          JLabel iconLabel = new JLabel(icons[i]);
          
          if(toolTips != null && toolTips.length > i) {
            iconLabel.setToolTipText(toolTips[i].toString());
          }
          else if(singleText != null) {
            iconLabel.setToolTipText(singleText);
          }
          else {
            iconLabel.setToolTipText(plugin.getInfo().getName());
          }
          
          iconLabels.add(iconLabel);
        }
      }
    }

    if (showPluginIcons && iconLabels.size() > 0) {
      addSeparator(doc, buffer);

      buffer
          .append("<tr><td valign=\"middle\" style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoColor)).append("; font-size:");

      buffer.append(mBodyFontSize);

      buffer.append("\"><b>");
      buffer.append("Plugin-Icons");
      buffer.append("</b></td><td valign=\"middle\" style=\"font-size:4\">");

      openPara(buffer, "info");
      // Workaround: Without the &nbsp; the component are not put in one line.
      buffer.append("&nbsp;");

      for (JLabel iconLabel : iconLabels) {
        buffer.append(doc.createCompTag(iconLabel));
        buffer.append("&nbsp;&nbsp;");
      }

      closePara(buffer);
      buffer.append("</td></tr>");
    }

    addSeparator(doc, buffer);

    for (Object id : fieldArr) {
      ProgramFieldType type = null;

      if (id instanceof String) {
        if (((String) id).matches("\\d+")) {
          try {
            type = ProgramFieldType
                .getTypeForId(Integer.parseInt((String) id, 10));
          } catch (Exception e) {
            // Empty Catch
          }
        }

        if (type == null) {
          int length = prog.getLength();
          if (length > 0 && ((String) id).trim().length() > 0) {

            buffer
                .append("<tr><td valign=\"top\" style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoColor)).append("; font-size:");

            buffer.append(mBodyFontSize);

            buffer.append("\"><b>");
            buffer.append(mLocalizer.msg("duration",
                "Program duration/<br>-end"));
            buffer.append("</b></td><td style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(foreground)).append("; font-size:");

            buffer.append(mBodyFontSize);

            buffer.append("\">");

            openPara(buffer, "time");

            String msg = mLocalizer.msg("minutes", "{0} min", length);
            buffer.append(msg).append(" (");
            buffer.append(mLocalizer.msg("until", "until {0}", prog.getEndTimeString()));

            int netLength = prog
                .getIntField(ProgramFieldType.NET_PLAYING_TIME_TYPE);
            if (netLength != -1) {
              msg = mLocalizer.msg("netMinuted", "{0} min net", netLength);
              buffer.append(" - ").append(msg);
            }
            buffer.append(')');

            closePara(buffer);

            buffer.append("</td></tr>");
            addSeparator(doc, buffer);
          }
        }
      } else if(id instanceof devplugin.ProgramInfo) {
        String pluginId = ((devplugin.ProgramInfo)id).getPluginId();
        
        PluginProxy plugin = PluginProxyManager.getInstance().getActivatedPluginForId(pluginId);
        
        if(plugin != null) {
          devplugin.ProgramInfo[] infos = plugin.getAddtionalProgramInfoForProgram(prog,((devplugin.ProgramInfo)id).getUniqueId());
          
          for(devplugin.ProgramInfo info : infos) {
            buffer.append("<tr><td valign=\"top\" style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoColor)).append("; font-size:");
    
            buffer.append(mBodyFontSize);
        
            buffer.append("\"><b>");
            buffer.append(info.getName());
            buffer.append("</b></td><td style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(foreground)).append("; font-size:");
        
            buffer.append(mBodyFontSize);
        
            buffer.append("\">");
            
            buffer.append(info.getValue());
    
            buffer.append("</td></tr>");
            addSeparator(doc, buffer);
          }
        }
      } else if(id instanceof CompoundedProgramFieldType) {
        CompoundedProgramFieldType value = (CompoundedProgramFieldType) id;
        String entry = value.getFormattedValueForProgram(prog);

        if(entry != null) {
          startInfoSection(buffer, value.getName(), infoColor, foreground);
          buffer.append(HTMLTextHelper.convertTextToHtml(entry, false));

          addSeparator(doc,buffer);
        }
      }
      else {
        type = (ProgramFieldType) id;

        if (type == ProgramFieldType.DESCRIPTION_TYPE) {
          String description = removeMultipleLineBreaksFromDescription(prog.getDescription());
          if (description != null
              && description.length() > 0) {
            addEntry(doc, buffer, prog, ProgramFieldType.DESCRIPTION_TYPE, true,
                showHelpLinks, showPersonLinks, infoColor, foreground);
          } else {
            addEntry(doc, buffer, prog, ProgramFieldType.SHORT_DESCRIPTION_TYPE,
                true, showHelpLinks,
                  showPersonLinks, infoColor, foreground);
          }
        } else if (type == ProgramFieldType.INFO_TYPE) {
          int info = prog.getInfo();
          if ((info != -1) && (info != 0)) {
            buffer
                .append("<tr><td valign=\"top\" style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoColor)).append("; font-size:");

            buffer.append(mBodyFontSize);

            buffer.append("\"><b>");
            buffer
                .append(type.getLocalizedName());
            buffer
                .append("</b></td><td valign=\"middle\" style=\"font-size:5\">");

            openPara(buffer, "info");
            // Workaround: Without the &nbsp; the component are not put in one
            // line.
            buffer.append("&nbsp;");

            int[] infoBitArr = ProgramInfoHelper.getInfoBits();
            Icon[] infoIconArr = ProgramInfoHelper.getInfoIcons();
            String[] infoMsgArr = ProgramInfoHelper.getInfoIconMessages();

            for (int i = 0; i < infoBitArr.length; i++) {
              if (ProgramInfoHelper.bitSet(info, infoBitArr[i])) {
                JLabel iconLabel;

                if (infoIconArr[i] != null) {
                  iconLabel = new JLabel(infoIconArr[i]);
                }
                else {
                  iconLabel = new JLabel(infoMsgArr[i]);
                }

                iconLabel.setToolTipText(infoMsgArr[i]);
                buffer.append(doc.createCompTag(iconLabel));

                buffer.append("&nbsp;&nbsp;");
              }
            }

            closePara(buffer);

            buffer.append("</td></tr>");
            addSeparator(doc, buffer);
          }
        } else if (type == ProgramFieldType.ADDITIONAL_INFORMATION_TYPE) {
          addEntry(doc, buffer, prog, ProgramFieldType.ADDITIONAL_INFORMATION_TYPE, true,
              showHelpLinks, showPersonLinks, infoColor, foreground);
        } else if (type == ProgramFieldType.URL_TYPE) {
          addEntry(doc, buffer, prog, ProgramFieldType.URL_TYPE, true,
              showHelpLinks, showPersonLinks, infoColor, foreground);
        }
        else if (type == ProgramFieldType.ACTOR_LIST_TYPE) {
          ArrayList<String> knownNames = new ArrayList<String>();
          String[] recognizedActors = ProgramUtilities.getActorNames(prog);
          if (recognizedActors != null) {
            knownNames.addAll(Arrays.asList(recognizedActors));
          }
          String actorField = prog.getTextField(type);
          if (actorField != null) {
            ArrayList<String>[] lists = ProgramUtilities.splitActors(prog);
            if (lists == null) {
              lists = splitActorsSimple(prog);
            }
            if (lists != null && lists[0].size() > 0) {
              startInfoSection(buffer, type.getLocalizedName(), infoColor, foreground);
              buffer.append("<table border=\"0\" cellpadding=\"0\" style=\"font-family:");
              buffer.append(bodyFont);
              buffer.append(";\">");
              for (int i=0; i < lists[0].size(); i++) {
                String[] parts = new String[2];
                parts[0] = lists[0].get(i);
                parts[1] = "";
                if (i < lists[1].size()) {
                  parts[1] = lists[1].get(i);
                }
                int actorIndex = 0;
                if (showPersonLinks) {
                    if (knownNames.contains(parts[0])) {
                      parts[0] = addPersonLink(parts[0],foreground);
                    } else if (knownNames.contains(parts[1])) {
                      parts[1] = addPersonLink(parts[1],foreground);
                      actorIndex = 1;
                    }
                }
                buffer.append("<tr><td valign=\"top\">&#8226;&nbsp;</td><td valign=\"top\">");
                buffer.append(parts[actorIndex]);
                buffer.append("</td><td width=\"10\">&nbsp;</td>");

                if (parts[1-actorIndex].length() > 0) {
                  buffer.append("<td valign=\"top\">");
                  buffer.append(parts[1-actorIndex]);
                  buffer.append("</td>");
                } else {
                  // if roles are missing add next actor in the same line
                   if (i+1 < lists[0].size() && lists[1].size() == 0) {
                    i++;
                    buffer.append("<td valign=\"top\">&#8226;&nbsp;</td><td valign=\"top\">");
                    if (showPersonLinks) {
                        buffer.append(addSearchLink(lists[0].get(i),foreground));
                      } else {
                        buffer.append(lists[0].get(i));
                      }
                    buffer.append("</td>");
                  }
                }
                buffer.append("</td></tr>");
              }
              buffer.append("</table>");
              buffer.append("</td></tr>");
              addSeparator(doc, buffer);
            }
            else {
              addEntry(doc, buffer, prog, type, showHelpLinks,
                    showPersonLinks, infoColor, foreground);
            }
          }
        }
        else {
          addEntry(doc, buffer, prog, type, showHelpLinks, showPersonLinks, infoColor, foreground);
        }
      }
    }

    if (showHelpLinks) {
      buffer
          .append("<tr><td colspan=\"2\" valign=\"top\" align=\"center\" style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoColor)).append("; font-size:");
      buffer.append(mBodyFontSize).append("\">");
      buffer.append("<a href=\"");
      buffer.append(
          mLocalizer.msg("dataInfo",
              "http://wiki.tvbrowser.org/index.php/Qualit%C3%A4t_der_Daten"))
          .append("\">");
      buffer.append(mLocalizer
          .msg("dataQuality", "Details of the data quality"));
      buffer.append("</a>");
      buffer.append("</td></tr>");
    }
    buffer.append("</table></html>");
    
    return buffer.toString();}catch(Exception e) {e.printStackTrace();}
    return "";
  }
  
  private static String getCssStyle(final int style) {
    StringBuilder result = new StringBuilder();
    if ((style & Font.BOLD) == Font.BOLD) {
      result.append("font-weight:bold;");
    }
    if ((style & Font.ITALIC) == Font.ITALIC) {
      result.append("font-style:italic;");
    }
    return result.toString();
  }

  private static String removeMultipleLineBreaksFromDescription(String desc) {
    if (desc != null) {
      desc = desc.replaceAll("\n{3,}", "\n\n");
    }
    
    return desc;
  }

  private static String addPersonLink(final String name, Color foreground) {
    if (name == null || name.isEmpty()) {
      return mLocalizer.msg("unknown", "(unknown)");
    }
    return addSearchLink(name, foreground);
  }

  private static ArrayList<String>[] splitActorsSimple(Program prog) {
    @SuppressWarnings("unchecked")
    ArrayList<String> list1 = new ArrayList();
    @SuppressWarnings("unchecked")
    ArrayList<String> list2 = new ArrayList();
    String actorField = prog.getTextField(ProgramFieldType.ACTOR_LIST_TYPE).trim();
    String[] actors;
    // don't try any parsing if newlines and commas are available
    // this must be recognized by the more advanced actors parsing
    if (actorField.contains("\n")) {
      if (actorField.contains(",")) {
        return null;
      }
      actors = actorField.split("\n");
    }
    else if (actorField.contains(",")) {
      actors = actorField.split(",");
    }
    else if (actorField.contains("\t")) {
      actors = new String[1];
      actors[0] = actorField;
    }
    else {
      return null;
    }
    for (String actor : actors) {
      actor = actor.trim();
      if (actor.length() > 0) {
        String part1 = actor;
        String part2 = "";
        if (actor.contains("\t")) {
          part1 = StringUtils.substringBefore(actor,"\t").trim();
          part2 = StringUtils.substringAfter(actor,"\t").trim();
        }
        else if (actor.contains("(") && actor.contains(")")) {
          part1 = actor.substring(0, actor.indexOf("(")).trim();
          part2 = actor.substring(actor.indexOf("(")+1, actor.lastIndexOf(")")).trim();
        }
        list1.add(part1);
        list2.add(part2);
      }
    }
    @SuppressWarnings("unchecked")
    ArrayList<String>[] result = new ArrayList[2];
    result[0] = list1;
    result[1] = list2;
    return result;
  }

  private static String addSearchLink(String topic, String displayText, Color foreground) {

    String style = " style=\"color:"+HTMLTextHelper.getCssRgbColorEntry(foreground)+"; border-bottom: 1px dashed;\"";
      StringBuilder buffer = new StringBuilder(32);
      buffer.append("<a href=\"");
      buffer.append(TVBROWSER_URL_PROTOCOL);
      buffer.append(StringUtils.remove(StringUtils.remove(topic, '"'), '\''));

      buffer.append("\" ");
      buffer.append(style);
      buffer.append('>');
      buffer.append(displayText);
      buffer.append("</a>");
      return buffer.toString();
  }

  private static String addSearchLink(String topic, Color foreground) {
    if (topic == null || topic.isEmpty()) {
      return "";
    }
    return addSearchLink(topic, topic, foreground);
  }

  private static void addEntry(ExtendedHTMLDocument doc, StringBuilder buffer,
      Program prog, ProgramFieldType fieldType, boolean showHelpLinks,
      boolean showPersonLinks, Color infoColor, Color foreground) {
    addEntry(doc, buffer, prog, fieldType, false, showHelpLinks,
        showPersonLinks,infoColor,foreground);
  }

  private static void addEntry(ExtendedHTMLDocument doc, StringBuilder buffer,
      Program prog, ProgramFieldType fieldType, boolean createLinks,
      boolean showHelpLinks, boolean showPersonLinks, Color infoColor, Color foreground) {

    try {
      String text = null;
      String name = fieldType.getLocalizedName();
      int blank = name.indexOf(' ', 16);
      if (blank > 0) {
        name = name.substring(0, blank) + "<br>" + name.substring(blank +1);
      }
      if (fieldType.getFormat() == ProgramFieldType.TEXT_FORMAT) {
        text = prog.getTextField(fieldType);
        if (fieldType == ProgramFieldType.SHORT_DESCRIPTION_TYPE) {
          text = removeMultipleLineBreaksFromDescription(text);
        }

        // Lazily add short description, but only if it differs from description
        if (fieldType == ProgramFieldType.DESCRIPTION_TYPE) {
          String description = removeMultipleLineBreaksFromDescription(prog.getDescription());
          text = description;

          if (prog.getShortInfo() != null) {
            StringBuilder shortInfo = new StringBuilder(removeMultipleLineBreaksFromDescription(prog.getShortInfo())
                .trim());

            // delete "..." at the end, but only for duplication check, not for display
            while (shortInfo.toString().endsWith(".")) {
              shortInfo.deleteCharAt(shortInfo.length() - 1);
            }
            
            if(shortInfo.toString().endsWith("\u2026")) {
              shortInfo.deleteCharAt(shortInfo.length() - 1);
            }

            if (!description.trim().startsWith(shortInfo.toString())) {
              addEntry(doc, buffer, prog,
                  ProgramFieldType.SHORT_DESCRIPTION_TYPE, true, showHelpLinks, infoColor, foreground);
            }
          }
          text = text.replace("\\-", ""); // replace conditional dashes
          text = HTMLTextHelper.convertTextToHtml(text, createLinks);
          // scan for moderation in beginning of description
          String[] lines = text.split("<br>");
          String[] tags = { "von und mit", "pr\00E4entiert von", "mit", "film von",
              "moderation", "zu gast" };
          for (int i = 0; i < 2; i++) {
            if (lines.length > i && lines[i].length() < 60) {
              String line = lines[i];
              for (String tag : tags) {
                if (line.toLowerCase().startsWith(tag)
                    || line.toLowerCase().startsWith(tag + ':')) {
                  String personsString = line.substring(tag.length(), line.length())
                      .trim();
                  if (personsString.startsWith(":")) {
                    personsString = personsString.substring(1).trim();
                  }
                  if (personsString.endsWith(".")) {
                    personsString = personsString.substring(0, personsString.length() - 1).trim();
                  }
                  String[] persons = personsString.split(" und ");
                  boolean doLink = true;
                  for (String person : persons) {
                    if (person.isEmpty() || !Character.isLetter(person.charAt(0)) || Character.isLowerCase(person.charAt(0))) {
                      doLink = false;
                      break;
                    }
                  }
                  if (doLink) {
                    for (String person : persons) {
                      String[] names = person.split(" ");
                      int partCount = names.length;
                      if (partCount >= 2 && partCount < 4) {
                        for (String n : names) {
                          if (!StringUtils.isAlpha(n)) {
                            doLink = false;
                          }
                        }
                        if (doLink) {
                          text = StringUtils.replaceOnce(text, person, addSearchLink(person, foreground));
                        }
                      }
                    }
                    break;
                  }
                }
              }
            }
          }
        }

      } else if (fieldType.getFormat() == ProgramFieldType.TIME_FORMAT) {
        text = prog.getTimeFieldAsString(fieldType);
      } else if (fieldType.getFormat() == ProgramFieldType.INT_FORMAT) {
        if (fieldType == ProgramFieldType.RATING_TYPE) {
          int value = prog.getIntField(fieldType);
          if (value > -1) {
            text = new DecimalFormat("##.#").format((double)prog.getIntField(fieldType) / 10) + "/10";
          }
        } else {
          text = prog.getIntFieldAsString(fieldType);
          if (text == null && fieldType == ProgramFieldType.AGE_LIMIT_TYPE) {
            final String ageRating = prog.getTextField(ProgramFieldType.AGE_RATING_TYPE);
            if (ageRating != null && !ageRating.isEmpty()) {
              int age = ProgramUtilities.getAgeLimit(ageRating);
              if (age >= 0) {
                text = Integer.toString(age);
              }
            }
          }
        }
      }

      if (fieldType == ProgramFieldType.ORIGIN_TYPE) {
        String temp = prog
            .getIntFieldAsString(ProgramFieldType.PRODUCTION_YEAR_TYPE);
        if (temp != null && temp.trim().length() > 0) {
          if (text == null || text.trim().length() < 1) {
            name = ProgramFieldType.PRODUCTION_YEAR_TYPE.getLocalizedName();
            text = temp;
          } else {
            name += "/<br>"
                + ProgramFieldType.PRODUCTION_YEAR_TYPE.getLocalizedName();
            text += " " + temp;
          }
        }
        temp = prog
        .getIntFieldAsString(ProgramFieldType.LAST_PRODUCTION_YEAR_TYPE);
        if (temp != null && temp.trim().length() > 0) {
          if (text == null || text.trim().length() < 1) {
            name = ProgramFieldType.LAST_PRODUCTION_YEAR_TYPE.getLocalizedName();
            text = temp;
          } else {

            text += " - " + temp;
          }
        }
      }

      if (text == null || text.trim().length() < 1) {
        if (ProgramFieldType.CUSTOM_TYPE == fieldType) {
          text = mLocalizer.msg("noCustom", "No custom information ");
        } else {
          return;
        }
      }

      startInfoSection(buffer, name, infoColor, foreground);

      // add person links
      if (ProgramFieldType.DIRECTOR_TYPE == fieldType
          || ProgramFieldType.SCRIPT_TYPE == fieldType
          || ProgramFieldType.CAMERA_TYPE == fieldType
          || ProgramFieldType.CUTTER_TYPE == fieldType
          || ProgramFieldType.MUSIC_TYPE == fieldType
          || ProgramFieldType.MODERATION_TYPE == fieldType
          || ProgramFieldType.ADDITIONAL_PERSONS_TYPE == fieldType
          || ProgramFieldType.PRODUCER_TYPE == fieldType) {
        if (showPersonLinks && text.length() < 200) {
          // if field is longer, this is probably not a list of names
          if (text.endsWith(".")) {
            text = text.substring(0, text.length() - 1);
          }
          String[] persons = splitPersons(text);
          for (int i = 0; i < persons.length; i++) {
            // remove duplicate entries
            boolean duplicate = false;
            if (i < persons.length - 1) {
              for (int j = i + 1; j < persons.length; j++) {
                if (persons[i].equalsIgnoreCase(persons[j])) {
                  duplicate = true;
                  break;
                }
              }
            }
            if (duplicate) {
              text = text.replaceFirst(Pattern.quote(persons[i]), "").trim();
              if (text.startsWith(",")) {
                text = text.substring(1).trim();
              }
              text = text.replaceAll(",\\s*,", ",");
              continue;
            }
            // a name shall not have more name parts
            if (persons[i].trim().split(" ").length <= 3) {
              String link;
              if (persons[i].contains("(")) {
                int index = persons[i].indexOf('(');
                String topic = persons[i].substring(0, index).trim();
                link = addSearchLink(topic,foreground) + " " + persons[i].substring(index).trim();
              } 
              if (persons[i].contains(":")) {
                int index = persons[i].indexOf(':')+1;
                String label = persons[i].substring(0, index).trim();
                link = label + " " + addSearchLink(persons[i].substring(index).trim(),foreground);
              } 
              else {
                link = addSearchLink(persons[i],foreground);
              }
              text = text.replace(persons[i], link);
            }
          }
        }
        buffer.append(text);
      }
      else if (ProgramFieldType.DESCRIPTION_TYPE == fieldType) {
        buffer.append(text);
      } else {
        buffer.append(HTMLTextHelper.convertTextToHtml(text, createLinks));
      }

      if ((ProgramFieldType.CUSTOM_TYPE == fieldType) && (showHelpLinks)) {
        buffer.append(" (<a href=\"").append(
            mLocalizer.msg("customInfo",
                "http://enwiki.tvbrowser.org/index.php/CustomInformation")).append(
            "\">?</a>)");
      }
      if ((ProgramFieldType.AGE_RATING_TYPE == fieldType) && (showHelpLinks)) {
        addHelpLink(buffer, mLocalizer.msg("ratingInfo", "http://en.wikipedia.org/wiki/Motion_picture_rating_system"));
      }

      buffer.append("</td></tr>");
      
      addSeparator(doc, buffer);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static void addHelpLink(final StringBuilder buffer, final String url) {
    buffer.append(" (<a href=\"").append(url).append("\">?</a>)");
  }

  private static String[] splitPersons(final String textField) {
    return ProgramUtilities.splitPersons(textField);
  }
  
  private static void startInfoSection(StringBuilder buffer, String section, Color infoForeground, Color foreground) {
    
    
    buffer.append("<tr><td valign=\"top\" style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(infoForeground)).append("; font-size:");
    buffer.append(mBodyFontSize);
    buffer.append("\"><b>");
    
    if (section.length() > 21 && section.indexOf("<br") == -1) {
      //try to split long names
      int space = section.lastIndexOf(' ');
      if (space > 1 && space < section.length()-2) {
        //if (space != section.indexOf(' ')) {
          //TODO      
        //}
        buffer.append(section.substring(0, space)).append("<br>").append(section.substring(space+1));
      }      
    } else {
      buffer.append(section);
    }
    
    buffer.append("</b></td><td style=\"color:").append(HTMLTextHelper.getCssRgbColorEntry(foreground)).append("; font-size:");
    buffer.append(mBodyFontSize);
    buffer.append("\">");
  }

  private static void addSeparator(ExtendedHTMLDocument doc,
      StringBuilder buffer) {
    buffer.append("<tr><td colspan=\"2\">");
    buffer.append("<div style=\"font-size:0;\">").append(
        doc.createCompTag(new HorizontalLine())).append("</div></td></tr>");
  }

  private static void openPara(StringBuilder buffer, String style) {
    buffer.append("<div id=\"").append(style).append("\">");
  }

  private static void closePara(StringBuilder buffer) {
    buffer.append("</div>\n");
  }

  /**
   *
   * @return The default order of the entries.
   */
  public static Object[] getDefaultOrder() {
    ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(new Object[] {
        ProgramFieldType.GENRE_TYPE,
        ProgramFieldType.DESCRIPTION_TYPE,
        ProgramFieldType.ADDITIONAL_INFORMATION_TYPE,
        ProgramFieldType.RATING_TYPE,
        ProgramFieldType.ORIGIN_TYPE,
        ProgramFieldType.DIRECTOR_TYPE,
        ProgramFieldType.SCRIPT_TYPE,
        ProgramFieldType.ACTOR_LIST_TYPE,
        ProgramFieldType.MODERATION_TYPE,
        ProgramFieldType.MUSIC_TYPE,
        ProgramFieldType.PRODUCER_TYPE,
        ProgramFieldType.CAMERA_TYPE,
        ProgramFieldType.CUTTER_TYPE,
        ProgramFieldType.ADDITIONAL_PERSONS_TYPE,
        ProgramFieldType.URL_TYPE,
        ProgramFieldType.ORIGINAL_TITLE_TYPE,
        ProgramFieldType.ORIGINAL_EPISODE_TYPE,
        ProgramFieldType.PRODUCTION_COMPANY_TYPE,
        ProgramFieldType.REPETITION_OF_TYPE,
        ProgramFieldType.REPETITION_ON_TYPE,
        ProgramFieldType.AGE_LIMIT_TYPE,
        ProgramFieldType.INFO_TYPE,
        ProgramFieldType.VPS_TYPE,
        ProgramFieldType.CUSTOM_TYPE,
        getDurationTypeString()}));
    // append missing fields (which may have been added in recent versions)
    for (Iterator<ProgramFieldType> iterator = ProgramFieldType.getTypeIterator(); iterator.hasNext();) {
      ProgramFieldType type = iterator.next();
      // exclude binary information which need special handling anyway
      if (type.getFormat() == ProgramFieldType.BINARY_FORMAT) {
        continue;
      }
      if (!list.contains(type) && !type.equals(ProgramFieldType.SHORT_DESCRIPTION_TYPE)
          && !type.equals(ProgramFieldType.PICTURE_COPYRIGHT_TYPE) && !type.equals(ProgramFieldType.PICTURE_DESCRIPTION_TYPE)
          && !type.equals(ProgramFieldType.TITLE_TYPE) && !type.equals(ProgramFieldType.EPISODE_NUMBER_TYPE) && !type.equals(ProgramFieldType.EPISODE_TOTAL_NUMBER_TYPE)
          && !type.equals(ProgramFieldType.EPISODE_TYPE)) {
        list.add(type);
      }
    }
    return list.toArray();
  }
  
  public static devplugin.ProgramInfo[] getDefaultOrderOfActivatedPluginInfo() {
    final ArrayList<devplugin.ProgramInfo> defaultOrderList = new ArrayList<devplugin.ProgramInfo>();
    final PluginProxy[] activated = PluginProxyManager.getInstance().getActivatedPlugins();
    
    for(final PluginProxy proxy : activated) {
      final devplugin.ProgramInfo[] pluginProgramInfo = proxy.getAddtionalProgramInfoForProgram(PluginManagerImpl.getInstance().getExampleProgram(),null);
      
      if(pluginProgramInfo != null) {
        defaultOrderList.addAll(Arrays.asList(pluginProgramInfo));
      }
    }
    
    return defaultOrderList.toArray(new devplugin.ProgramInfo[defaultOrderList.size()]);
  }
  
  public static Object[] getDefaultOrderWithActivatedPluginInfo() {
    final ArrayList<Object> defaultOrderList = new ArrayList<Object>();
    
    final Object[] defaultOrder = ProgramTextCreator.getDefaultOrder();
    
    defaultOrderList.addAll(Arrays.asList(defaultOrder));
    
    final PluginProxy[] activated = PluginProxyManager.getInstance().getActivatedPlugins();
    
    for(final PluginProxy proxy : activated) {
      final devplugin.ProgramInfo[] pluginProgramInfo = proxy.getAddtionalProgramInfoForProgram(PluginManagerImpl.getInstance().getExampleProgram(),null);
      
      if(pluginProgramInfo != null) {
        defaultOrderList.addAll(Arrays.asList(pluginProgramInfo));
      }
    }
    
    return defaultOrderList.toArray();
  }

  /**
   * @return The String for the duration/end of a program.
   */
  public static String getDurationTypeString() {
    return mLocalizer.msg("duration", "Program duration/<br>-end").replaceAll(
        "<br>", "");
  }
}
