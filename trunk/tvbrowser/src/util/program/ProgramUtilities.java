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
 */
package util.program;

import util.io.IOUtilities;
import devplugin.Date;
import devplugin.Program;
import devplugin.ProgramFieldType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import tvbrowser.core.ChannelList;

/**
 * Provides utilities for program stuff.
 * 
 * @author Ren� Mach
 *
 */
public class ProgramUtilities {

  /**
   * Helper method to check if a program runs.
   * 
   * @param p
   *          The program to check.
   * @return True if the program runs.
   */
  public static boolean isOnAir(Program p) {
    int time = IOUtilities.getMinutesAfterMidnight();
    Date currentDate = Date.getCurrentDate();
    if (currentDate.addDays(-1).compareTo(p.getDate()) == 0) {
      time += 24 * 60;
    }
    if (currentDate.compareTo(p.getDate()) < 0) {
      return false;
    }

    if (p.getStartTime() <= time && (p.getStartTime() + p.getLength()) > time) {
      return true;
    }
    return false;
  }

  public static Comparator<Program> getProgramComparator() {
    return sProgramComparator;
  }

  /**
   * Comparator to sort programs by date, time and channel 
   */
  private static Comparator<Program> sProgramComparator = new Comparator<Program>(){
    public int compare(Program p1, Program p2) {
      int res=p1.getDate().compareTo(p2.getDate());
      if (res!=0) {
        return res;
      }

      int minThis=p1.getStartTime();
      int minOther=p2.getStartTime();

      if (minThis<minOther) {
        return -1;
      }else if (minThis>minOther) {
        return 1;
      }

      int pos1 = ChannelList.getPos(p1.getChannel());
      int pos2 = ChannelList.getPos(p2.getChannel());
      if (pos1 < pos2) {
        return -1;
      }
      else if (pos1 > pos2) {
        return 1;
      }

      return 0;

    }
  };

  /**
   * A helper method to get if a program is not in a time range.
   * 
   * @param timeFrom The beginning of the time range to check
   * @param timeTo The ending of the time range
   * @param p The program to check
   * @return If the program is not in the given time range.
   * @since 2.2.2
   */
  public static boolean isNotInTimeRange(int timeFrom, int timeTo, Program p) {
    int timeFromParsed = timeFrom;

    if(timeFrom > timeTo) {
      timeFromParsed -= 60*24;
    }
    
    int startTime = p.getStartTime(); 
    
    if(timeFrom > timeTo && startTime >= timeFrom) {
      startTime -= 60*24;
    }
    
    return (startTime < timeFromParsed || startTime > timeTo);
  }
  
  /**
   * extract the actor names from the actor field
   * 
   * @param program the program to work on
   * @return list fo real actor names or null (if it can not be decided)
   */
  public static String[] getActorsFromActorsField(Program program) {
    String actorsField = program.getTextField(ProgramFieldType.ACTOR_LIST_TYPE);
    if (actorsField != null) {
      String[] actors = new String[0];
      // actor list separated by newlines
      if (actorsField.contains("\n")) {
        actors = actorsField.split("\n");
      }
      // actor list separated by colon
      else if (actorsField.contains(",")) {
        actors = actorsField.split(",");
      }
      ArrayList<String> listFirst = new ArrayList<String>();
      ArrayList<String> listSecond = new ArrayList<String>();
      for (String actor : actors) {
        // actor and role separated by brackets
        if (actor.contains("(") && actor.contains(")")) {
          listFirst.add(actor.substring(0, actor.indexOf("(")).trim());
          listSecond.add(actor.substring(actor.indexOf("(")+1,actor.lastIndexOf(")")).trim());
        }
        // actor and role separated by tab
        else if (actor.contains("\t")) {
          listFirst.add(actor.substring(0, actor.indexOf("\t")).trim());
          listSecond.add(actor.substring(actor.indexOf("\t")+1).trim());
        }
        else {
          listFirst.add(actor.trim());
        }
      }
      ArrayList<String>[] lists = new ArrayList[2];
      lists[0] = listFirst;
      lists[1] = listSecond;
      ArrayList<String> result = separateRolesAndActors(lists, program);
      if (result != null) {
        String[] array = new String[result.size()];
        result.toArray(array);
        return array;
      }
    }
    return null;
  }

  /**
   * decide which of the 2 lists contains the real actor names and which the role names
   * @param program 
   * @param listFirst first list of names
   * @param listSecond second list of names
   */
  private static ArrayList<String> separateRolesAndActors(ArrayList<String>[] list, Program program) {
    // return first list, if only one name per actor is available
    if (list[1].size() == 0) {
      return list[0];
    }
    // get directors names
    String[] directors = new String[0];
    String directorField = program.getTextField(ProgramFieldType.DIRECTOR_TYPE);
    if (directorField != null) {
      directors = directorField.split(",");
    }
    // get script writers
    String[] scripts = new String[0];
    String scriptField = program.getTextField(ProgramFieldType.SCRIPT_TYPE);
    if (scriptField != null) {
      scripts = scriptField.split(",");
    }
    boolean use[] = new boolean[2];
    String lowerTitle = program.getTitle().toLowerCase();
    for (int i = 0; i < use.length; i++) {
      use[i] = false;
    }
    for (int i = 0; i < list.length; i++) {
      // search for director in actors list
      for (String director : directors) {
        use[i] = use[i] || list[i].contains(director);
      }
      // search for script in actors list
      for (String script : scripts) {
        use[i] = use[i] || list[i].contains(script);
      }
      // search for role in program title
      for (int j = 0; j < list[i].size(); j++) {
        use[1-i] = use[1-i] || lowerTitle.contains(list[i].get(j).toLowerCase());
      }
      if (use[i]) {
        return list[i];
      }
    }
    // which list contains more names with one part only (i.e. no family name) -> role names
    int singleName[] = new int[list.length];
    // which list contains more abbreviations at the beginning -> role names
    int abbreviation[] = new int[list.length];
    // which list contains more slashes -> double roles for a single actor
    int slashes[] = new int[2];
    // which list has duplicate family names -> roles
    HashMap<String,Integer>[] familyNames = new HashMap[list.length];
    int[] maxNames = new int[2];
    for (int i = 0; i < list.length; i++) {
      familyNames[i] = new HashMap<String, Integer>();
      for (String name : list[i]) {
        if (!name.contains(" ")) {
          singleName[i]++;
        }
        else {
          String familyName = name.substring(name.lastIndexOf(" ")+1);
          Integer count = new Integer(1);
          if (familyNames[i].containsKey(familyName)) {
            count = familyNames[i].get(familyName);
            count = new Integer(count.intValue()+1);
          }
          familyNames[i].put(familyName, count);
        }
        // only count abbreviations at the beginning, so we do not count a middle initial like in "Jon M. Doe"
        if (name.contains(".") && (name.indexOf(".") < name.indexOf(" "))) {
          abbreviation[i]++;
        }
        if (name.contains("/")) {
          slashes[i]++;
        }
      }
      for (Integer familyCount : familyNames[i].values()) {
        if (familyCount.intValue() > maxNames[i]) {
          maxNames[i] = familyCount.intValue();
        }
      }
    }
    if (slashes[0] < slashes[1]) {
      return list[0];
    }
    else if (slashes[1] < slashes[0]) {
      return list[1];
    }
    else if (singleName[0] < singleName[1]) {
      return list[0];
    }
    else if (singleName[1] < singleName[0]) {
      return list[1];
    }
    else if (abbreviation[0] < abbreviation[1]) {
      return list[0];
    }
    else if (abbreviation[1] < abbreviation[0]) {
      return list[1];
    }
    else if (maxNames[0] < maxNames[1]) {
      return list[0];
    }
    else if (maxNames[1] < maxNames[0]) {
      return list[1];
    }
    else {
      return null;
    }
  }
}
