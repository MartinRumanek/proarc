/*
 * Copyright (C) 2014 Jan Pokorsky
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
package cz.cas.lib.proarc.common.export.desa;

import cz.cas.lib.proarc.common.object.ValueMap;
import cz.cas.lib.proarc.common.object.model.MetaModel;
import cz.cas.lib.proarc.desa.DesaClient;
import cz.cas.lib.proarc.desa.SIP2DESATransporter;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.ACollects.ACollect;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.AFunds.AFund;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.CreCntrls.CreCntrl;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.DocTypes.DocType;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.Locs.Loc;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.Pronoms.Pronom;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.RdCntrls.RdCntrl;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.RecCls.RecCl;
import cz.cas.lib.proarc.desa.nomenclature.Nomenclatures.RecTypes.RecType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import javax.xml.bind.JAXB;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;

/**
 * Manages DESA service clients and their configurations.
 *
 * @author Jan Pokorsky
 */
public final class DesaServices {

    /**
     * The property name of list of all active DESA service IDs.
     */
    public static final String PROPERTY_DESASERVICES = "desa.services";
    /**
     * The prefix of properties of the given service.
     */
    public static final String PREFIX_DESA = "desa";

    private static final Logger LOG = Logger.getLogger(DesaServices.class.getName());
    private final Configuration config;


    public DesaServices(Configuration config) {
        this.config = config;
    }

    /**
     * Gets value maps of nomenclatures.
     * @param n nomenclature
     * @param pluginId prefix for each added value map
     * @return list of value maps
     */
    public List<ValueMap> getValueMap(Nomenclatures n, String pluginId) {
        ArrayList<ValueMap> maps = new ArrayList<ValueMap>();
        if (n == null) {
            return maps;
        }
        if (n.getACollects() != null) {
            maps.add(new ValueMap<ACollect>(pluginId + ".a-collect", n.getACollects().getACollect()));
        }
        if (n.getAFunds() != null) {
            maps.add(new ValueMap<AFund>(pluginId + ".a-fund", n.getAFunds().getAFund()));
        }
        if (n.getCreCntrls() != null) {
            maps.add(new ValueMap<CreCntrl>(pluginId + ".cre-cntrl", n.getCreCntrls().getCreCntrl()));
        }
        if (n.getDocTypes() != null) {
            maps.add(new ValueMap<DocType>(pluginId + ".doc-type", n.getDocTypes().getDocType()));
        }
        if (n.getLocs() != null) {
            maps.add(new ValueMap<Loc>(pluginId + ".loc", n.getLocs().getLoc()));
        }
        if (n.getPronoms() != null) {
            maps.add(new ValueMap<Pronom>(pluginId + ".pronom", n.getPronoms().getPronom()));
        }
        if (n.getRdCntrls() != null) {
            maps.add(new ValueMap<RdCntrl>(pluginId + ".rd-cntrl", n.getRdCntrls().getRdCntrl()));
        }
        if (n.getRecCls() != null) {
            maps.add(new ValueMap<RecCl>(pluginId + ".rec-cl", n.getRecCls().getRecCl()));
        }
        if (n.getRecTypes() != null) {
            maps.add(new ValueMap<RecType>(pluginId + ".rec-type", n.getRecTypes().getRecType()));
        }
        return maps;
    }

    /**
     * Gets nomenclatures.
     * @param dc configuration to query
     * @return nomenclatures or {@code null}
     */
    public Nomenclatures getNomenclatures(DesaConfiguration dc) {
        List<String> acronyms = dc.getNomenclatureAcronyms();
        if (acronyms.isEmpty()) {
            return null;
        } else {
            return getNomenclaturesCache(dc);
        }
    }

    private Nomenclatures getNomenclaturesCache(DesaConfiguration dc) {
        File tmpFolder = FileUtils.getTempDirectory();
        File cache = null;
        if (dc.getNomenclatureExpiration() > 0) {
            synchronized(DesaServices.this) {
                cache = new File(tmpFolder, String.format("%s.nomenclatures.cache", dc.getServiceId()));
                int expiration = dc.getNomenclatureExpiration();
                if (cache.exists() && (System.currentTimeMillis() - cache.lastModified() < expiration)) {
                    return JAXB.unmarshal(cache, Nomenclatures.class);
                }
            }
        }
        DesaClient desaClient = getDesaClient(dc);
        Nomenclatures nomenclatures = desaClient.getNomenclatures(
                // XXX replace with real producer code from http request
                dc.toTransporterConfig().get("desa." + DesaConfiguration.PROPERTY_PRODUCER),
                dc.getNomenclatureAcronyms());
        if (cache != null) {
            synchronized (DesaServices.this) {
                JAXB.marshal(nomenclatures, cache);
            }
        }
        return nomenclatures;
    }

    public DesaClient getDesaClient(DesaConfiguration dc) {
        DesaClient desaClient = new DesaClient(dc.getSoapServiceUrl(),
                dc.getRestServiceUrl(), dc.getUsername(), dc.getPassword());
        return desaClient;
    }

    /**
     * Finds a service configuration that accepts the passes model.
     * The configuration must be listed in {@link #PROPERTY_DESASERVICES}.
     *
     * @param model digital object model
     * @return the service configuration or {@code null}
     */
    public DesaConfiguration findConfiguration(MetaModel model) {
        if (model == null) {
            return null;
        }
        for (String sid : config.getStringArray(PROPERTY_DESASERVICES)) {
            DesaConfiguration dc = readConfiguration(config, sid);
            if (dc.getExportModels().contains(model.getPid())) {
                return dc;
            }
        }
        return null;
    }

    /**
     * Finds a service configuration. The configuration must be listed in
     * {@link #PROPERTY_DESASERVICES}.
     *
     * @param serviceId service ID
     * @return the service configuration or {@code null}
     */
    public DesaConfiguration findConfiguration(String serviceId) {
        for (String sid : config.getStringArray(PROPERTY_DESASERVICES)) {
            if (sid.equals(serviceId)) {
                return readConfiguration(config, sid);
            }
        }
        return null;
    }

    /**
     * Finds a configuration that declares any of the passed model IDs.
     * @param modelIds mode IDs to query
     * @return the service configuration or {@code null}
     */
    public DesaConfiguration findConfigurationWithModel(String... modelIds) {
        for (String sid : config.getStringArray(PROPERTY_DESASERVICES)) {
            DesaConfiguration dc = readConfiguration(config, sid);
            List<String> exportModels = dc.getExportModels();
            for (String modelId : modelIds) {
                if (exportModels.contains(modelId)) {
                    return dc;
                }
            }
        }
        return null;
    }

    /**
     * Gets all configurations.
     * @return the list of service configurations
     */
    public List<DesaConfiguration> getConfigurations() {
        return readConfigurations(config);
    }

    private List<DesaConfiguration> readConfigurations(Configuration config) {
        ArrayList<DesaConfiguration> configs = new ArrayList<DesaConfiguration>();
        for (String serviceId : config.getStringArray(PROPERTY_DESASERVICES)) {
            configs.add(readConfiguration(config, serviceId));
        }
        return configs;
    }

    private DesaConfiguration readConfiguration(Configuration config, String serviceId) {
        String servicePrefix = PREFIX_DESA + '.' + serviceId;
        Configuration serviceConfig = config.subset(servicePrefix);
        return new DesaConfiguration(serviceId, servicePrefix, serviceConfig);
    }

    /**
     * The configuration for access to the DESA registry.
     */
    public static final class DesaConfiguration {

        public static final String PROPERTY_USER = "user";
        public static final String PROPERTY_PASSWD = "password";
        public static final String PROPERTY_PRODUCER = "producer";
        public static final String PROPERTY_OPERATOR = "operator";
        public static final String PROPERTY_RESTAPI = "restapi";
        public static final String PROPERTY_WEBSERVICE = "webservice";
        public static final String PROPERTY_EXPORTMODELS = "exportModels";
        public static final String PROPERTY_NOMENCLATUREACRONYMS = "nomenclatureAcronyms";
        public static final String PROPERTY_NOMENCLATUREEXPIRATION = "nomenclatureCacheExpiration";

        private final String serviceId;
        private final String prefix;
        private final Configuration properties;

        public DesaConfiguration(String serviceId, String prefix, Configuration properties) {
            this.serviceId = serviceId;
            this.prefix = prefix;
            this.properties = properties;
        }

        public String getServiceId() {
            return serviceId;
        }

        public String getSoapServiceUrl() {
            return properties.getString(PROPERTY_WEBSERVICE);
        }

        public String getRestServiceUrl() {
            return properties.getString(PROPERTY_RESTAPI);
        }

        public String getUsername() {
            return properties.getString(PROPERTY_USER);
        }

        public String getPassword() {
            return properties.getString(PROPERTY_PASSWD);
        }

        /**
         * Digital object model IDs accepted by the registry.
         * @return list of IDs
         */
        public List<String> getExportModels() {
            return Arrays.asList(properties.getStringArray(PROPERTY_EXPORTMODELS));
        }

        /**
         * Gets nomenclature acronyms to query {@link Nomenclatures} for value maps from the registry.
         * @return list of acronyms
         */
        public List<String> getNomenclatureAcronyms() {
            return Arrays.asList(properties.getStringArray(PROPERTY_NOMENCLATUREACRONYMS));
        }

        /**
         * Gets time to hold caches.
         * @return time in milliseconds
         */
        public int getNomenclatureExpiration() {
            return properties.getInt(PROPERTY_NOMENCLATUREEXPIRATION, 0) * 60 * 1000;
        }

        /**
         * Properties required by the {@link SIP2DESATransporter}.
         * @return map of properties
         */
        @Deprecated
        public HashMap<String, String> toTransporterConfig() {
            HashMap<String, String> tcfg = new HashMap<String, String>();
            putProperty(tcfg, PROPERTY_USER, PROPERTY_PASSWD, PROPERTY_PRODUCER,
                    PROPERTY_OPERATOR, PROPERTY_RESTAPI, PROPERTY_WEBSERVICE);
            // enforce REST file upload
            tcfg.put("desa.rest", Boolean.TRUE.toString());
            tcfg.put("checkMTDPSP", "false");
            return tcfg;
        }

        
        /**
         * Properties required by the {@link DESAAuthenticator}
         * @return
         */
        public HashMap<String, String> toDESAAuthenticationConfig() {
            HashMap<String, String> tcfg = new HashMap<String, String>();
            tcfg.put("desa." + PROPERTY_USER, properties.getString(PROPERTY_USER));
            putProperty(tcfg, PROPERTY_USER, PROPERTY_PASSWD, PROPERTY_PRODUCER,
                    PROPERTY_OPERATOR, PROPERTY_RESTAPI, PROPERTY_WEBSERVICE);
            return tcfg;
        }
        
        private void putProperty(HashMap<String, String> tcfg, String... names) {
            for (String name : names) {
                tcfg.put("desa." + name, properties.getString(name));
            }
        }

        @Override
        public String toString() {
            return "DesaConfiguration{" + "serviceId=" + serviceId + ", models=" + getExportModels()
                    + ", acronyms=" + getNomenclatureAcronyms()
                    + ", properties=" + toTransporterConfig() + '}';
        }

    }

}
