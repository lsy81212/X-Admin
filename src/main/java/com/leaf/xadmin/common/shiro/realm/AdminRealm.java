package com.leaf.xadmin.common.shiro.realm;

import com.leaf.xadmin.entity.bg.Admin;
import com.leaf.xadmin.entity.auth.Permission;
import com.leaf.xadmin.entity.auth.Role;
import com.leaf.xadmin.vo.enums.AdminStatus;
import com.leaf.xadmin.vo.enums.ErrorStatus;
import com.leaf.xadmin.vo.enums.LoginType;
import com.leaf.xadmin.vo.exception.GlobalException;
import com.leaf.xadmin.service.bg.IAdminService;
import com.leaf.xadmin.service.auth.IPermissionService;
import com.leaf.xadmin.service.auth.IRoleService;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

import java.util.Iterator;
import java.util.List;

/**
 * 自定义管理员权限域 AdminRealm
 *
 * @author leaf
 */
public class AdminRealm extends AbstractCustomRealm {
    @Autowired
    @Lazy
    private IRoleService roleService;
    @Autowired
    @Lazy
    private IPermissionService permissionService;
    @Autowired
    @Lazy
    private IAdminService adminService;

    public AdminRealm() {
        this.loginType = LoginType.ADMIN;
    }

    /**
     * 授权
     *
     * @param principalCollection
     * @return
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        Iterator<String> iterator = principalCollection.getRealmNames().iterator();
        boolean contains = false;
        while (iterator.hasNext()) {
            contains = iterator.next().contains(this.getClass().getName());
            if (contains) {
                break;
            }
        }
        if (principalCollection.isEmpty() || !contains) {
            return null;
        }
        // 获取登录用户名
        String name = (String) principalCollection.getPrimaryPrincipal();
        SimpleAuthorizationInfo authorizationInfo = new SimpleAuthorizationInfo();
        Admin admin = adminService.queryOneByName(name);
        if (admin == null) {
            return null;
        }
        // 获取用户权限和角色列表
        List<Role> roles = roleService.queryAdminRoles(admin.getId());
        List<Permission> permissions = permissionService.queryAdminPermissions(admin.getId());

        // 加载用户角色列表
        for (Role role : roles) {
            authorizationInfo.addRole(role.getName());
        }

        // 加载用户权限列表
        for (Permission permission : permissions) {
            authorizationInfo.addStringPermission(permission.getName());
        }

        return authorizationInfo;

    }

    /**
     * 认证
     *
     * @param authenticationToken
     * @return
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) {
        // 获取管理员名
        String name = (String) authenticationToken.getPrincipal();
        // 查找管理员信息
        Admin admin = adminService.queryOneByName(name);
        if (admin != null) {
            // 判断管理员帐户是否被锁定
            if (!admin.getStatus().equals(AdminStatus.NORMAL.getCode())) {
                throw new GlobalException(ErrorStatus.ACCOUNT_LOCK_ERROR);
            }
            // 封装管理员认证信息
            AuthenticationInfo authInfo = new SimpleAuthenticationInfo(admin.getName(),
                    admin.getPass(), getName());
            return authInfo;
        }

        return null;
    }

    /**
     * 清理全部权限缓存信息
     *
     */
    public void clearAllCachedAuthors() {
        this.clearCachedAuthorizationInfo(SecurityUtils.getSubject().getPrincipals());
    }
}
