/*
 * Copyright (C) 2012 Jan Pokorsky
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
package cz.incad.pas.editor.server.config;

import java.io.File;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side configurations.
 *
 * @author Jan Pokorsky
 */
public final class PasConfiguration {
    
    public static final String USER_HOME = "user.home";
    public static final String CONFIG_FOLDER = "cz.incad.pas.editor.server.config.home";
    public static final String CONFIG_FOLDER_NAME = ".pas";
    
    private static final Logger LOG = Logger.getLogger(PasConfiguration.class.getName());

//    private static final PasConfiguration INSTANCE = new PasConfiguration();

    private String homePath;
    private File configHome;
    private final Map<String, String> environment;

    PasConfiguration(Map<String, String> environment) {
        this.environment = environment;
        init();
    }
//
//    public static PasConfiguration getInstance() {
//        return INSTANCE;
//    }

    public void reload() {
        init();
    }

    public File getConfigHome() {
        return configHome;
    }

    private void init() {
        File home = initHome(environment.get(USER_HOME));
        this.homePath = home.getPath();
        this.configHome = initConfigFolder(home, environment.get(CONFIG_FOLDER));
    }

    private static File initHome(String home) {
        home = (home == null) ? "" : home;
        File homeFile = new File(home);
        if (!homeFile.exists()) {
            throw new IllegalStateException(String.format("Cannot find home folder: '%s'!", home));
        }
        if (!homeFile.isDirectory()) {
            throw new IllegalStateException(String.format("Not a folder: '%s'!", home));
        }
        if (!(homeFile.canRead() && homeFile.canWrite())) {
            throw new IllegalStateException(String.format("Invalid access permissions for: '%s'!", home));
        }

        return homeFile;
    }

    private static File initConfigFolder(File home, String configPath) {
        File config;
        if (configPath != null) {
            config = new File(configPath);
        } else {
            config = new File(home, CONFIG_FOLDER_NAME);
        }
        if (config.exists()) {
            if (!config.isDirectory()) {
                throw new IllegalStateException(String.format("Not a folder: '%s'!", home));
            }
            if (!(config.canRead() && config.canWrite())) {
                throw new IllegalStateException(String.format("Invalid access permissions for: '%s'!", home));
            }
        } else {
            if (!config.mkdir()) {
                throw new IllegalStateException(String.format("Cannot create folder: '%s'!", home));
            }
        }
        LOG.log(Level.INFO, "config folder: {0}", config);
        return config;
    }

}