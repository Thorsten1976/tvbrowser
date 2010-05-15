/*
 * Copyright Michael Keppler
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package util.misc;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import util.io.IOUtilities;
import devplugin.Date;
import devplugin.Program;
import devplugin.ProgramFieldType;

/**
 * @author bananeweizen
 *
 */
public abstract class AbstractXmlTvDataHandler extends DefaultHandler {
  private static final String COMMA_SPACE = ", ";
  private static final String COMMA_LINE_BREAK = ",\n";
  /**
   * RegEx-Pattern for the Actor
   */
  private static final Pattern ACTOR_PATTERN = Pattern.compile("(.*)\\((.*)\\)");
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddkkmmss ZZZZ");
  /**
   * Holds the text of the current tag.
   */
  private StringBuffer mText = new StringBuffer();

  /**
   * Value for some elements (e.g. star-rating)
   */
  private String mValue = null;

  /**
   * The value of the attribute 'lang' of the current tag.
   */
  private String mLang;

  /**
   * The channel ID of the current program
   */
  private String mChannelId;

  /**
   * if the currently parsed program is valid
   */
  private boolean mIsValid = false;
  private String mEpisodeType;
  private String mTitle;
  private boolean mSetTitle;

  /**
   * Handles the occurrence of tag text by buffering it for later analysis
   */
  public void characters(char ch[], int start, int length) throws SAXException {
    // There is some text -> Add it to the text buffer
    mText.append(ch, start, length);
  }

  /**
   * occurrence of a start tag<br>
   * Here we only handle tags with attributes, all other tags are handled when
   * they end.
   */
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    // A new tag begins -> Clear the text buffer
    mText.setLength(0);

    // Set the lang
    mLang = attributes.getValue("lang");

    // Special tag treatment
    if (qName.equals("programme")) {
      mIsValid = false;
      mSetTitle = false;
      mTitle = null;
      String startDateTime = attributes.getValue("start");
      mChannelId = attributes.getValue("channel");
      if (startDateTime == null) {
        logMessage("Start time missing in programme tag");
      } else if (mChannelId == null) {
        logMessage("Channel missing in programme tag");
      } else {
        try {
          Date startDate = extractDate(startDateTime);
          int startTime = extractTime(startDateTime);
          startProgram(startDate, startTime);

          addField(ProgramFieldType.START_TIME_TYPE, startTime);

          String vps = attributes.getValue("vps-start");
          if (vps != null) {
            int time = extractTime(vps);
            addField(ProgramFieldType.VPS_TYPE, time);
          }

          String endDateTime = attributes.getValue("stop");
          if (endDateTime != null) {
            int endTime = extractTime(endDateTime);
            addField(ProgramFieldType.END_TIME_TYPE, endTime);
          }

          mIsValid = true;
        } catch (IOException exc) {
          logException(exc);
        }
      }
    } else if (qName.equals("previously-shown")) {
      try {
        Date prevDate = extractDate(attributes.getValue("start"));
        addField(ProgramFieldType.REPETITION_OF_TYPE, prevDate.toString());
      } catch (IOException exc) {
        logException(exc);
      }
    } else if (qName.equals("next-time-shown")) {
      try {
        Date nextDate = extractDate(attributes.getValue("start"));
        addField(ProgramFieldType.REPETITION_ON_TYPE, nextDate.toString());
      } catch (IOException exc) {
        logException(exc);
      }
    } else if ("episode-num".equals(qName)) {
      mEpisodeType = attributes.getValue("system");
    }
  }

  /**
   * starts a new program
   *
   * @param startDate
   *          start date of the program
   * @param startTime
   *          time in minutes after midnight
   */
  protected abstract void startProgram(final Date startDate, final int startTime);

  /**
   * Handles the occurrence of an end tag.
   */
  public void endElement(String uri, String localName, String qName) throws SAXException {
    if (isValid()) {
      // every value shall be trimmed by default
      String text = mText.toString().trim();

      if (qName.equals("title")) {
        mTitle = text;
        if ((mLang == null) || mLang.equals(getChannelCountry()) || getCountries(mLang).contains(getChannelCountry())) {
          addField(ProgramFieldType.TITLE_TYPE, text);
          mSetTitle = true;
        } else {
          addField(ProgramFieldType.ORIGINAL_TITLE_TYPE, text);
        }
      } else if (qName.equals("sub-title")) {
        if ((mLang == null) || mLang.equals(getChannelCountry())) {
          addField(ProgramFieldType.EPISODE_TYPE, text);
        } else {
          addField(ProgramFieldType.ORIGINAL_EPISODE_TYPE, text);
        }
      } else if (qName.equals("desc")) {
        addField(ProgramFieldType.DESCRIPTION_TYPE, text);
      } else if (qName.equals("date")) {
        if (text.length() < 4) {
          logMessage("WARNING: The date value must have at least 4 chars: '" + text + '\'');
        } else {
          int year = Integer.parseInt(text.substring(0, 4));
          addField(ProgramFieldType.PRODUCTION_YEAR_TYPE, year);
        }
      } else if (qName.equals("rating")) {
        try {
          int ageLimit = Integer.valueOf(text);
          addField(ProgramFieldType.AGE_LIMIT_TYPE, ageLimit);
        } catch (NumberFormatException exc) {
          addField(ProgramFieldType.AGE_RATING_TYPE, text);
        }
      } else if (qName.equals("url")) {
        addField(ProgramFieldType.URL_TYPE, text);
      } else if (qName.equals("category")) {
        text = text.substring(0, 1).toUpperCase() + text.substring(1);
        if (text.toLowerCase().indexOf("serie") > -1) {
          setInfoBit(Program.INFO_CATEGORIE_SERIES);
        } else if (text.toLowerCase().indexOf("movie") > -1) {
          setInfoBit(Program.INFO_CATEGORIE_MOVIE);
        }
        addToList(ProgramFieldType.GENRE_TYPE, text, COMMA_SPACE);
      } else if (qName.equals("country")) {
        addField(ProgramFieldType.ORIGIN_TYPE, text);
      } else if (qName.equals("subtitles")) {
        if ((mLang == null) || mLang.equals(getChannelCountry())) {
          setInfoBit(Program.INFO_SUBTITLE_FOR_AURALLY_HANDICAPPED);
        } else {
          setInfoBit(Program.INFO_ORIGINAL_WITH_SUBTITLE);
        }
      } else if (qName.equals("live")) {
        setInfoBit(Program.INFO_LIVE);
      } else if (qName.equals("length")) {
        try {
          int length = Integer.parseInt(text);
          addField(ProgramFieldType.NET_PLAYING_TIME_TYPE, length);
        } catch (NumberFormatException exc) {
          logMessage("WARNING: length is no number: '" + text + "' and will be ignored.");
        }
      } else if (qName.equals("actor")) {
        Matcher m = ACTOR_PATTERN.matcher(text);
        if (m.matches()) {
          text = m.group(2).trim() + "\t\t-\t\t" + m.group(1).trim();
        }
        if (text.toLowerCase().startsWith("presented by")) {
          text = text.substring("Presented By".length()).trim();
          addToList(ProgramFieldType.MODERATION_TYPE, text, COMMA_SPACE);
        }
        else {
          addToList(ProgramFieldType.ACTOR_LIST_TYPE, text, COMMA_LINE_BREAK);
        }
      } else if (qName.equals("director")) {
        addToList(ProgramFieldType.DIRECTOR_TYPE, text, COMMA_SPACE);
      } else if (qName.equals("writer")) {
        addToList(ProgramFieldType.SCRIPT_TYPE, text, COMMA_SPACE);
      } else if (qName.equals("presenter")) {
        addToList(ProgramFieldType.MODERATION_TYPE, text, COMMA_SPACE);
      } else if (qName.equals("music")) {
        addToList(ProgramFieldType.MUSIC_TYPE, text, COMMA_SPACE);
      } else if (qName.equals("producer")) {
        addToList(ProgramFieldType.PRODUCER_TYPE, text, COMMA_SPACE);
      } else if (qName.equals("colour")) {
        if (text.equals("no")) {
          setInfoBit(Program.INFO_VISION_BLACK_AND_WHITE);
        } else if (!text.equals("yes")) {
          logMessage("WARNING: value of colour tag must be 'yes' or 'no'," + " but it is '" + text + '\'');
        }
      } else if (qName.equals("quality")) {
        if (text.equals("HDTV")) {
          setInfoBit(Program.INFO_VISION_HD);
        } else {
          logMessage("WARNING: value of quality tag must be 'HDTV' but it is '" + text + '\'');
        }
      } else if (qName.equals("aspect")) {
        if (text.equals("4:3")) {
          setInfoBit(Program.INFO_VISION_4_TO_3);
        } else if (text.equals("16:9")) {
          setInfoBit(Program.INFO_VISION_16_TO_9);
        } else {
          logMessage("WARNING: value of aspect tag must be '4:3' or '16:9'," + " but it is '" + text + '\'');
        }
      } else if (qName.equals("stereo")) {
        if (text.equals("mono")) {
          setInfoBit(Program.INFO_AUDIO_MONO);
        } else if (text.equals("stereo")) {
          setInfoBit(Program.INFO_AUDIO_STEREO);
        } else if (text.equals("surround")) {
          setInfoBit(Program.INFO_AUDIO_DOLBY_SURROUND);
        } else if (text.equals("5.1")) {
          setInfoBit(Program.INFO_AUDIO_DOLBY_DIGITAL_5_1);
        } else if (text.equals("two channel tone")) {
          setInfoBit(Program.INFO_AUDIO_TWO_CHANNEL_TONE);
        } else if (text.equals("audio description")) {
          setInfoBit(Program.INFO_AUDIO_DESCRIPTION);
        } else {
          logMessage("WARNING: value of stereo tag must be one of 'mono', "
              + "'stereo', 'surround', '5.1' or 'two channel tone' but it is '" + text + '\'');
        }
      } else if (qName.equals("picture")) {
        File file = new File(text);
        if (file.exists() && file.isFile()) {
          try {
            addField(ProgramFieldType.PICTURE_TYPE, IOUtilities.getBytesFromFile(file));
          } catch (IOException e) {
            logException(e);
          }
        } else {
          logMessage("Warning: File does not exist: " + text);
        }
      } else if (qName.equals("picture-copyright")) {
        addField(ProgramFieldType.PICTURE_COPYRIGHT_TYPE, text);
      } else if (qName.equals("picture-description")) {
        addField(ProgramFieldType.PICTURE_DESCRIPTION_TYPE, text);
      } else if (qName.equals("value")) {
        mValue = text;
      } else if (qName.equals("star-rating")) {
        if (mValue != null) {

          if (mValue.contains("/")) {
            try {
              int num = Integer.valueOf(mValue.substring(0, mValue.indexOf('/')).trim());
              int max = Integer.valueOf(mValue.substring(mValue.indexOf('/') + 1).trim());
              addField(ProgramFieldType.RATING_TYPE, num * 100 / max);
            } catch (NumberFormatException ex) {
              logException(ex);
            }
          } else {
            logMessage("Star-rating must be in form 8/10");
          }

        }

        mValue = null;
      } else if (qName.equals("new")) {
        setInfoBit(Program.INFO_NEW);
      } else if (qName.equals("programme")) {
        // if we only set the original title, then we still need to set the title
        if (!mSetTitle && mTitle != null) {
          addField(ProgramFieldType.TITLE_TYPE, mTitle);
        }
        endProgram();
      } else if ("episode-num".equals(qName)) {
        if ("onscreen".equals(mEpisodeType)) {
          addField(ProgramFieldType.EPISODE_TYPE, text);
        } else if ("xmltv_ns".equals(mEpisodeType)) {
          // format is
          // season/totalseasons.episodenum/totalepisode.part/totalparts
          // where current numbers start at 0, while total numbers start at 1
          if (text.length() > 0) {
            String[] ep = text.split("\\.");
            if (ep.length > 0 && ep[0].length() > 0) {
              String[] seasons = ep[0].trim().split("/");
              if (seasons.length > 0 && seasons[0].trim().length() > 0) {
                int season = Integer.parseInt(seasons[0].trim()) + 1;
                if (season > 0) {
                  addField(ProgramFieldType.SEASON_NUMBER_TYPE, season);
                }
              }
            }
            if (ep.length > 1 && ep[1].length() > 0) {
              String[] parts = ep[1].trim().split("/");
              if (parts.length == 2) {
                String currentString = parts[0].trim();
                if (currentString.length() > 0) {
                  int current = Integer.parseInt(currentString) + 1;
                  if (current > 0) {
                    addField(ProgramFieldType.EPISODE_NUMBER_TYPE, current);
                  }
                }
                String totalString = parts[1].trim();
                if (totalString.length() > 0) {
                  int total = Integer.parseInt(totalString);
                  if (total > 0) {
                    addField(ProgramFieldType.EPISODE_TOTAL_NUMBER_TYPE, total);
                  }
                }
              }
            }
          }
        }

      }
    }
    // Clear lang
    mLang = null;
  }

  private String getCountries(final String language) {
    if (language == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    for (Locale locale : Locale.getAvailableLocales()) {
      if (locale.getLanguage().equalsIgnoreCase(language)) {
        String country = locale.getCountry();
        if (country != null && country.length() > 0) {
          result.append(',').append(country);
        }
      }
    }
    return result.toString().toLowerCase();
  }

  protected abstract String getChannelCountry();

  /**
   * program parsing finishes
   */
  protected abstract void endProgram();

  /**
   * whether or not the currently parsed program is valid
   *
   * @return
   */
  protected boolean isValid() {
    return mIsValid;
  }

  /**
   * sets a bit in the info field
   *
   * @param bit
   *          The bit to add
   */
  protected abstract void setInfoBit(final int bit);

  /**
   * Adds a binary field to the current program
   */
  protected abstract void addField(final ProgramFieldType fieldType, final byte[] value);

  /**
   * adds a String field to the current program
   *
   * @param fieldType
   * @param value
   */
  protected abstract void addField(final ProgramFieldType fieldType, final String value);

  /**
   * adds an Integer field to the current program
   *
   * @param fieldType
   * @param value
   */
  protected abstract void addField(final ProgramFieldType fieldType, final int value);

  protected abstract void logMessage(final String message);

  protected abstract void logException(final Exception exc);

  /**
   * Extracts the time from a XMLTV time value.
   *
   * @param dateTime
   *          The value to extract the time from.
   * @return The time in minutes after midnight
   * @throws java.io.IOException
   *           If the value has the wrong format.
   */
  private int extractTime(final String dateTime) throws IOException {
    Calendar cal = parseDateTime(dateTime);
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int minute = cal.get(Calendar.MINUTE);
    return hour * 60 + minute;
  }

  /**
   * Extracts the date from a XMLTV time value.
   *
   * @param dateTime
   *          The value to extract the date from.
   * @return The date.
   * @throws java.io.IOException
   *           If the value has the wrong format.
   */
  private Date extractDate(final String dateTime) throws IOException {
    Calendar cal = parseDateTime(dateTime);
    return new devplugin.Date(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
  }

  synchronized private Calendar parseDateTime(final String time) {
    try {
      Calendar calendar = Calendar.getInstance();
      calendar.setTime(DATE_FORMAT.parse(time.substring(0, 20)));
      calendar.setTimeInMillis(calendar.getTimeInMillis() - calendar.getTimeZone().getRawOffset());
      return calendar;
    } catch (ParseException e) {
      logMessage("invalid time format: " + time);
      return null;
    }
  }

  /**
   * XMLTV channel id of currently parsed program
   *
   * @return
   */
  protected final String getChannelId() {
    return mChannelId;
  }

  /**
   * Adds a text to a field that builds a comma separated value (e.g. the actor
   * list).
   *
   * @param type
   *          The type of the field to add the text to.
   * @param text
   *          The text to add.
   * @param separator
   *          separator to add after each new entry
   */
  abstract protected void addToList(final ProgramFieldType fieldType, String value, final String separator);

}
