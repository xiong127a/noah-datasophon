package com.datasophon.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@Getter
public enum UserEnum {

    HDFS("hdfs", "hadoop", 2001, 2001),
    YARN("yarn", "hadoop", 2002, 2001),
    HIVE("hive", "hadoop", 2003, 2001),
    MAPRED("mapred", "hadoop", 2004, 2001),
    HBASE("hbase", "hadoop", 2005, 2001),
    KYUUBI("kyuubi", "hadoop", 2006, 2001),
    ELASTIC("elastic", "elastic", 2007, 2007),
    HUE("hue", "hue", 2008, 2008),
    POSTGRES("postgres", "postgres", 2009, 2009),
    ADMIN("admin", "hadoop", 2010, 2001),
    RANGER("ranger", "ranger", 2011, 2002),
    RANGER_USER_SYNC("rangerusersync", "ranger", 2012, 2002),
    RANGER_KMS("rangerkms", "ranger", 2013, 2002);

    private final String username;
    private final String groupname;
    private final int userId;
    private final int groupId;

    private static final Map<String, UserEnum> USERNAME_MAP = new HashMap<>();
    private static final Map<String, UserEnum> GROUPNAME_MAP = new HashMap<>();

    static {
        for (UserEnum userEnum : values()) {
            USERNAME_MAP.put(userEnum.getUsername(), userEnum);
            GROUPNAME_MAP.put(userEnum.getGroupname(), userEnum);
        }
    }

    public static Integer getUserIdByUsername(String username) {
        UserEnum userEnum = USERNAME_MAP.get(username);
        return userEnum != null ? userEnum.getUserId() : null;
    }

    public static Integer getGroupIdByUsername(String username) {
        UserEnum userEnum = USERNAME_MAP.get(username);
        return userEnum != null ? userEnum.getGroupId() : null;
    }

    public static Integer getGroupIdByGroupName(String groupname) {
        UserEnum userEnum = GROUPNAME_MAP.get(groupname);
        return userEnum != null ? userEnum.getGroupId() : null;
    }

}