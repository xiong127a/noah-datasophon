package com.datasophon.common.command;

import lombok.Data;

import java.io.Serializable;

@Data
public class LdapCommand implements Serializable {

    private String operation;
    private String ldapUrl;
    private String username;
    private String mail;
    private String description;
    private String uidNumber;
    private String gidNumber;
    private String rootDn;
    private String userRootDn;
    private String ldapPwd;
    private String userPwd;

}
