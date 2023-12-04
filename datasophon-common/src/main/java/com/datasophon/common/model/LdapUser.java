package com.datasophon.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class LdapUser {
    public String cn;
    public String sn;
    public String uid;
    public String userPassword;
    public String displayName;
    public String mail;
    public String description;
    public String uidNumber;
    public String gidNumber;
}
