package com.vexus2.jenkins.chatwork.jenkinschatworkplugin.api;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Room {
  @JsonProperty("room_id")
  public String roomId;

  @JsonProperty("name")
  public String name;

  @Override
  public int hashCode(){
    return new HashCodeBuilder()
        .append(name)
        .append(roomId)
        .toHashCode();
  }

  @Override
  public boolean equals(final Object obj){
    if(obj instanceof Room){
      final Room other = (Room) obj;
      return new EqualsBuilder()
          .append(name, other.name)
          .append(roomId, other.roomId)
          .isEquals();
    } else{
      return false;
    }
  }
}
