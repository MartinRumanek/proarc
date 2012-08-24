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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.incad.pas.editor.server.config;

import cz.incad.pas.editor.server.catalog.AlephXServer;
import cz.incad.pas.editor.server.catalog.BibliographicCatalog;
import cz.incad.pas.editor.server.catalog.DigitizationRegistryCatalog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.Configuration;

/**
 * Bibliographic catalog configurations.
 *
 * @author Jan Pokorsky
 */
public final class CatalogConfiguration {

    private static final Logger LOG = Logger.getLogger(CatalogConfiguration.class.getName());
    static final String CATALOG_PREFIX = "catalog";
    static final String PROPERTY_CATALOGS = "catalogs";

    private final Configuration config;

    public CatalogConfiguration(Configuration config) {
        this.config = config;
    }

    public List<CatalogProperties> getCatalogs() {
        ArrayList<CatalogProperties> catalogs = new ArrayList<CatalogProperties>();
        for (String catalogId : config.getStringArray(PROPERTY_CATALOGS)) {
            String catalogPrefix = CATALOG_PREFIX + '.' + catalogId;
            HashMap<String, String> properties = new HashMap<String, String>();
            CatalogProperties catalog = new CatalogProperties(catalogId, properties);
            for (Iterator<String> it = config.getKeys(catalogPrefix); it.hasNext();) {
                String key = it.next();
                properties.put(key, config.getString(key));
            }
            if (!isValidProperty(catalogPrefix, CatalogProperties.PROPERTY_URL, catalog.getUrl())) {
                continue;
            }
            isValidProperty(catalogPrefix, CatalogProperties.PROPERTY_NAME, catalog.getName());
            catalogs.add(catalog);
        }
        return catalogs;
    }

    private static boolean isValidProperty(String prefix, String name, String value) {
        if (value == null || value.isEmpty()) {
            LOG.log(Level.WARNING, "Missing {0}.{1} property!", new Object[]{prefix, name});
            return false;
        }
        return true;
    }

    public BibliographicCatalog findCatalog(String id) {
        CatalogProperties props = findConfig(id);
        if (props == null) {
            return null;
        }
        BibliographicCatalog catalog = DigitizationRegistryCatalog.get(props);
        if (catalog != null) {
            return catalog;
        }
        catalog = AlephXServer.get(props);
        return catalog;
    }

    public CatalogProperties findConfig(String id) {
        for (CatalogProperties catalog : getCatalogs()) {
            if (catalog.getId().equals(id)) {
                return catalog;
            }
        }
        return null;
    }

    /**
     * Configuration of the particular catalog.
     */
    public static final class CatalogProperties {

        public static final String PROPERTY_NAME = "name";
        public static final String PROPERTY_USER = "user";
        public static final String PROPERTY_PASSWD = "password";
        public static final String PROPERTY_URL = "url";
        public static final String PROPERTY_TYPE = "type";

        private final String id;
        private final String prefix;
        private final Map<String, String> properties;

        private CatalogProperties(String id, Map<String, String> properties) {
            this.id = id;
            this.properties = properties;
            this.prefix = CATALOG_PREFIX + '.' + id;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return properties.get(prefix + '.' + PROPERTY_NAME);
        }

        public String getType() {
            return properties.get(prefix + '.' + PROPERTY_TYPE);
        }

        public String getUrl() {
            return properties.get(prefix + '.' + PROPERTY_URL);
        }

        public String getPrefix() {
            return prefix;
        }

        public Map<String, String> getProperties() {
            return properties;
        }

        public String getProperty(String name) {
            return properties.get(prefix + '.' + name);
        }

    }

}
