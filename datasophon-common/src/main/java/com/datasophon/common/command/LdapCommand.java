package com.datasophon.common.command;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = false)
public class LdapCommand extends BaseCommand {

    @Serial
    private static final long serialVersionUID = 1L;
    
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
