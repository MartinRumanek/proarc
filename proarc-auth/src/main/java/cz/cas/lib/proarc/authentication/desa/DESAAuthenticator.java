/*
 * Copyright (C) 2013 Pavel Stastny
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cas.lib.proarc.authentication.desa;

import com.sun.xml.ws.client.ClientTransportException;
import cz.cas.lib.proarc.authentication.AbstractAuthenticator;
import cz.cas.lib.proarc.authentication.ProarcPrincipal;
import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.config.AppConfigurationException;
import cz.cas.lib.proarc.common.config.AppConfigurationFactory;
import cz.cas.lib.proarc.common.export.desa.DesaServices;
import cz.cas.lib.proarc.common.export.desa.DesaServices.DesaConfiguration;
import cz.cas.lib.proarc.common.user.UserManager;
import cz.cas.lib.proarc.common.user.UserProfile;
import cz.cas.lib.proarc.common.user.UserUtil;
import cz.cas.lib.proarc.desa.DesaClient;
import cz.cas.lib.proarc.desa.soap.AuthenticateUserFault;
import cz.cas.lib.proarc.desa.soap.AuthenticateUserResponse;
import cz.cas.lib.proarc.desa.soap.Role;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * DESA authentication
 * @author pavels
 */
public class DESAAuthenticator extends AbstractAuthenticator {
    
    public static Logger LOGGER = Logger.getLogger(DESAAuthenticator.class.getName());
    
    public static final String KOD_PUVODCE = "kod";

    public DESAAuthenticator() {
    }

    DesaClient getDesaClient() {
        try {
            AppConfiguration appConfig = AppConfigurationFactory.getInstance().defaultInstance();
            DesaServices desaServices = appConfig.getDesaServices();
            List<DesaConfiguration> configurations = desaServices.getConfigurations();
            if (!configurations.isEmpty()) {
                DesaConfiguration desConf = configurations.get(0);
                return desaServices.getDesaClient(desConf);
            } else {
                throw new IllegalStateException("Missing DESA configuration!");
            }
        } catch (AppConfigurationException e) {
            LOGGER.log(Level.SEVERE,e.getMessage(),e);
            throw new IllegalStateException("Cannot initialize configuration! See server log.");
        }
    }

    UserProfile authenticateReq(String tUser, String tPass, String code) {
        try {
            AuthenticateUserResponse desaUser = getDesaClient().authenticateUser(tUser, tPass, code);
            if (desaUser != null && desaUser.getRoles() != null) {
                for (Role role : desaUser.getRoles().getItem()) {
                    if ("producer_submit".equals(role.getRoleAcr())) {
                        UserProfile proarcUser = new UserProfile();
                        proarcUser.setEmail(desaUser.getEmail());
                        proarcUser.setForename(desaUser.getName());
                        proarcUser.setProarcuser(false);
                        proarcUser.setSurname(desaUser.getSurname());
                        proarcUser.setUserName(tUser);
                        return proarcUser;
                    }
                }
            }
        } catch (AuthenticateUserFault e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        } catch (ClientTransportException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    @Override
    public boolean authenticate(Map<String, String> loginProperties,
            HttpServletRequest request, HttpServletResponse response,
            ProarcPrincipal principal) {

        String user = loginProperties.get(LOGINNAME);
        String pswd = loginProperties.get(PASSWORD);
        String kod = loginProperties.get(KOD_PUVODCE);
        if (isNullString(kod) || isNullString(user) || isNullString(pswd)) {
            return false;
        }
        UserProfile authenticated = authenticateReq(user, pswd, kod);
        if (authenticated != null) {
            associateUserProfile(principal, authenticated, kod);
        }
        
        return authenticated != null;
    }

    boolean isNullString(String str) {
        return str == null || str.trim().equals("");
    }

    public void associateUserProfile(ProarcPrincipal principal, UserProfile user, String producerCode) {
        UserManager userManager = UserUtil.getDefaultManger();
        // NOTE: UserProfile.validateAsNew(UserProfile.java:197) only lower
        // case supports but ws
        String proarcValidUserName = user.getUserName();
        UserProfile userProfile = userManager.find(proarcValidUserName);
        if (userProfile == null) {
            user.setCreated(new Date());
            user.setProarcuser(false);
            userProfile = userManager.add(user);
        }
        // XXX create group with producerCode as group name and set is as a user default group
        principal.associateUserProfile(userProfile);
    }

}
