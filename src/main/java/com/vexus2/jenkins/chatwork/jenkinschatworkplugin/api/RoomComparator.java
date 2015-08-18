package com.vexus2.jenkins.chatwork.jenkinschatworkplugin.api;

import org.apache.commons.lang.StringUtils;

import java.util.Comparator;

public class RoomComparator implements Comparator<Room> {
  @Override
  public int compare(Room room1, Room room2) {
    if(!StringUtils.equals(room1.type, room2.type)){
      return room1.type.compareTo(room2.type);
    }

    return room1.name.compareTo(room2.name);
  }
}
