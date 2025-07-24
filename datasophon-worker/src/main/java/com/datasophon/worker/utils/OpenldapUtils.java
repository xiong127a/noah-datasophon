package com.datasophon.worker.utils;

import com.datasophon.common.model.LdapUser;

import javax.naming.Context;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

public class OpenldapUtils {

    public static LdapUser buildBaseLdapUser(String username, String mail, String description, String uidNumber, String gidNumber, String password) {
        LdapUser lu = new LdapUser();
        lu.setCn(username);
        lu.setSn(username);
        lu.setUid(username);
        lu.setUserPassword(password);
        lu.setDisplayName(username);
        lu.setMail(mail);
        lu.setDescription(description);
        lu.setUidNumber(uidNumber);
        lu.setGidNumber(gidNumber);
        return lu;
    }

    /**
     * 添加用户
     *
     */
    public static boolean addUser(LdapUser lu, String userRootDn, LdapContext ctx) {
        BasicAttributes attrsbu = new BasicAttributes();
        BasicAttribute objclassSet = new BasicAttribute("objectClass");
        objclassSet.add("top");
        objclassSet.add("person");
        objclassSet.add("organizationalPerson");
        objclassSet.add("inetOrgPerson");
        objclassSet.add("shadowAccount");
        objclassSet.add("posixAccount");
        attrsbu.put(objclassSet);
        attrsbu.put("uid", lu.getUid());//显示账号
        attrsbu.put("sn", lu.getSn());//显示姓名
        attrsbu.put("cn", lu.getCn());//显示账号
        attrsbu.put("gecos", lu.getCn());//显示账号
        attrsbu.put("userPassword", lu.getUserPassword());//显示密码
        attrsbu.put("displayName", lu.getDisplayName());//显示描述
        attrsbu.put("mail", lu.getMail());//显示邮箱
        attrsbu.put("homeDirectory", "/home/" + lu.getCn());//显示home地址
        attrsbu.put("loginShell", "/bin/bash");//显示shell方式
        attrsbu.put("uidNumber", lu.getUidNumber());/*显示id */
        attrsbu.put("gidNumber", lu.getGidNumber());/*显示组id */

        try {
            ctx.createSubcontext(getUserDn(lu.getCn(), userRootDn), attrsbu);
            System.out.println("添加用户成功");
            return true;
        } catch (Exception e) {
            System.out.println("添加用户失败");
            e.printStackTrace();
            return false;
        }
    }

    public static String getUserDn(String uid, String userRootDn) {
        return "uid=" + uid + "," + userRootDn;
    }

    /**
     * 删除
     */
    public static boolean delete(String username, String userRootDn, LdapContext ctx) {
        try {
            ctx.destroySubcontext(getUserDn(username, userRootDn));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取ldap认证
     *
     */
    public static LdapContext ldapConnect(String url, String root, String pwd) {
        String factory = "com.sun.jndi.ldap.LdapCtxFactory";
        String simple = "simple";
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, factory);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, simple);
        env.put(Context.SECURITY_PRINCIPAL, root);
        env.put(Context.SECURITY_CREDENTIALS, pwd);
        LdapContext ctx = null;
        Control[] connCtls = null;
        try {
            ctx = new InitialLdapContext(env, connCtls);
        } catch (javax.naming.AuthenticationException e) {
            System.out.println("认证失败：");
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("认证出错：");
            e.printStackTrace();
        }
        return ctx;
    }

}