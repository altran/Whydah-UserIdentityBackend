package net.whydah.identity.user.resource;

import net.whydah.identity.user.UIBUserAggregate;
import net.whydah.identity.user.identity.UIBUserIdentity;
import net.whydah.identity.user.role.UserPropertyAndRole;
import net.whydah.sso.user.types.UserAggregate;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:erik-dev@fjas.no">Erik Drolshammer</a> 12/04/14
 */
@Deprecated  // Use UserApplicationRoleEntry in TypeLib
public class UserAggregateRepresentation extends UserAggregate {
    private String password;    //TODO include this in response?

    private List<UIBRoleRepresentation> roles;

    private UserAggregateRepresentation() {
    }

    public static UserAggregateRepresentation fromUserAggregate(UIBUserAggregate userAggregate) {
        UserAggregateRepresentation dto = new UserAggregateRepresentation();

        UIBUserIdentity id = userAggregate.getIdentity();
        dto.setUid(id.getUid());
        dto.setUsername(id.getUsername());
        dto.setFirstName(id.getFirstName());
        dto.setLastName(id.getLastName());
        dto.setPersonRef(id.getPersonRef());
        dto.setEmail(id.getEmail());
        dto.setCellPhone(id.getCellPhone());
        dto.setPassword(id.getPassword());

        List<UserPropertyAndRole> userPropertyAndRoles = userAggregate.getRoles();
        List<UIBRoleRepresentation> roleRepresentations = new ArrayList<>(userPropertyAndRoles.size());
        for (UserPropertyAndRole role : userPropertyAndRoles) {
            roleRepresentations.add(UIBRoleRepresentation.fromUserPropertyAndRole(role));
        }
        dto.setRoles(roleRepresentations);
        return dto;
    }

    public UIBUserAggregate buildUserAggregate() {
        List<UserPropertyAndRole> userPropertyAndRoles = buildUserPropertyAndRoles();

        UIBUserIdentity userIdentity = buildUserIdentity();
        UIBUserAggregate userAggregate = new UIBUserAggregate(userIdentity, userPropertyAndRoles);
        return userAggregate;
    }

    private UIBUserIdentity buildUserIdentity() {
        UIBUserIdentity userIdentity = new UIBUserIdentity(getUid(), getUsername(), getFirstName(), getLastName(), getEmail(), getPassword(), getCellPhone(), getPersonRef());
        return userIdentity;
    }

    public List<UserPropertyAndRole> buildUserPropertyAndRoles() {
        List<UserPropertyAndRole>  userRoles = new ArrayList<>();
        if (roles != null && roles.size() > 0) {
            for (UIBRoleRepresentation role : roles) {
                UserPropertyAndRole userRole = new UserPropertyAndRole();
                userRole.setUid(getUid());
                userRole.setRoleId(role.getId());
                userRole.setApplicationId(role.getApplicationId());
                userRole.setApplicationName(role.getApplicationName());
                userRole.setOrganizationName(role.getOrganizationName());
                userRole.setApplicationRoleName(role.getApplicationRoleName());
                userRole.setApplicationRoleValue(role.getApplicationRoleValue());
                userRoles.add(userRole);
            }
        }
        return userRoles;
    }


    public void setPassword(String password) {
        this.password = password;
    }

    public void setRoles(List<UIBRoleRepresentation> roles) {
        this.roles = roles;
    }

    public String getPassword() {
        return password;
    }

    public List<UIBRoleRepresentation> getRoles() {
        return roles;
    }
}
