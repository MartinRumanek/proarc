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
package cz.cas.lib.proarc.webapp.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import cz.cas.lib.proarc.common.config.AppConfiguration;
import cz.cas.lib.proarc.common.config.AppConfigurationException;
import cz.cas.lib.proarc.common.config.AppConfigurationFactory;
import cz.cas.lib.proarc.common.dao.Batch;
import cz.cas.lib.proarc.common.fedora.DigitalObjectException;
import cz.cas.lib.proarc.common.fedora.FedoraObject;
import cz.cas.lib.proarc.common.fedora.LocalStorage;
import cz.cas.lib.proarc.common.fedora.RemoteStorage;
import cz.cas.lib.proarc.common.imports.ImportBatchManager;
import cz.cas.lib.proarc.common.imports.ImportBatchManager.BatchItemObject;
import cz.cas.lib.proarc.common.mods.Mods33Utils;
import cz.cas.lib.proarc.common.object.DescriptionMetadata;
import cz.cas.lib.proarc.common.object.DigitalObjectHandler;
import cz.cas.lib.proarc.common.object.DigitalObjectManager;
import cz.cas.lib.proarc.common.object.MetadataHandler;
import cz.cas.lib.proarc.webapp.client.rpc.ModsGwtRecord;
import cz.cas.lib.proarc.webapp.client.rpc.ModsGwtService;
import cz.cas.lib.proarc.webapp.server.rest.ImportResource;
import cz.cas.lib.proarc.webapp.server.rest.RestException;
import cz.cas.lib.proarc.webapp.server.rest.SessionContext;
import cz.cas.lib.proarc.webapp.shared.rest.DigitalObjectResourceApi;
import cz.fi.muni.xkremser.editor.client.mods.ModsCollectionClient;
import cz.fi.muni.xkremser.editor.server.mods.ModsCollection;
import cz.fi.muni.xkremser.editor.server.mods.ModsType;
import cz.fi.muni.xkremser.editor.server.util.BiblioModsUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

/**
 * Simple MODS provider.
 *
 * TODO: Exceptions? (IAE is handled as server error 500 now).
 * TODO: restrict user access
 *
 * @author Jan Pokorsky
 */
public class ModsGwtServiceProvider extends RemoteServiceServlet implements ModsGwtService {

    private static final Logger LOG = Logger.getLogger(ModsGwtServiceProvider.class.getName());

    private AppConfiguration appConfig;
    private RemoteStorage repository;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.appConfig = AppConfigurationFactory.getInstance().defaultInstance();
            this.repository = RemoteStorage.getInstance(appConfig);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            throw new UnavailableException("Cannot access Fedora");
        } catch (AppConfigurationException ex) {
            LOG.log(Level.SEVERE, "Invalid editor configuration.", ex);
            throw new UnavailableException("Invalid editor configuration.");
        }
    }

    @Override
    public ModsGwtRecord read(String id, Integer batchId) {
        try {
            FedoraObject fobject = findFedoraObject(id, batchId, false);
            DigitalObjectHandler doHandler = DigitalObjectManager.getDefault().createHandler(fobject);
            DescriptionMetadata<String> metadataAsXml = doHandler.metadata().getMetadataAsXml();
            ModsType mods = Mods33Utils.unmarshal(metadataAsXml.getData(), ModsType.class);;
            String xml = Mods33Utils.toXml(mods, true);
            int xmlHash = xml.hashCode();
            LOG.log(Level.FINE, "id: {0}, hash: {2}, MODS: {1}", new Object[]{id, xml, xmlHash});
            ModsCollection modsCollection = new ModsCollection();
            modsCollection.setMods(Arrays.asList(mods));
            ModsCollectionClient modsClient = BiblioModsUtils.toModsClient(modsCollection);
            return new ModsGwtRecord(modsClient, metadataAsXml.getTimestamp(), xmlHash);
        } catch (DigitalObjectException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Writes MODS to storage.
     *
     * XXX implement null id handling if necessary
     * XXX implement modification recognition; we could send XML hash in read response and check it here; if unmodified ignore write.
     *     As ModsCollectionClient does not support modification status it would probably require to introduce method isModified(ModsCollectionClient, xmlHash):boolean
     *
     * @param id digital object id
     * @param modsClient MODS
     * @return
     */
    @Override
    public String write(String id, Integer batchId, ModsGwtRecord record) {
        SessionContext session = SessionContext.from(getThreadLocalRequest());
        String oldId = id;
        LOG.log(Level.FINE, "id: {0}, modsClient: {1}, hash: {2}", new Object[]{id, record.getMods(), record.getXmlHash()});
        ModsCollection mods = BiblioModsUtils.toMods(record.getMods());
        ModsType modsType = mods.getMods().get(0);
        String xml = Mods33Utils.toXml(modsType, true);
        int xmlHash = xml.hashCode();
        LOG.log(Level.FINE, "id: {0}, hash: {2}, MODS: {1}", new Object[]{id, xml, xmlHash});
        if (xmlHash == record.getXmlHash()) {
            return id;
        }

        try {
            FedoraObject fObject = findFedoraObject(id, batchId, false);
            DigitalObjectHandler doHandler = DigitalObjectManager.getDefault().createHandler(fObject);
            MetadataHandler<Object> mHandler = doHandler.metadata();
            DescriptionMetadata<String> metadata = doHandler.metadata().getMetadataAsXml();
            metadata.setPid(id);
            metadata.setBatchId(batchId);
            metadata.setData(xml);
            metadata.setTimestamp(record.getTimestamp());
            mHandler.setMetadataAsXml(metadata, session.asFedoraLog());
            doHandler.commit();
        } catch (DigitalObjectException ex) {
            throw new IllegalStateException(ex);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        LOG.log(Level.FINE, "written id: {0}, old id: {1}", new Object[]{id, oldId});

        return id;
    }

    @Override
    @Deprecated
    public String getXml(ModsCollectionClient modsCollection) {
        LOG.log(Level.FINE, "modsClient: {0}", new Object[]{modsCollection});
        ModsCollection mods = BiblioModsUtils.toMods(modsCollection);
        String xml = BiblioModsUtils.toXML(mods);
        return xml;
    }

    // XXX temorary; unify with DigitalObjectResource.findFedoraObject to DigitalObject API
    private FedoraObject findFedoraObject(String pid, Integer batchId, boolean readonly) throws IOException {
        FedoraObject fobject;
        if (batchId != null) {
            ImportBatchManager importManager = ImportBatchManager.getInstance();
            BatchItemObject item = importManager.findBatchObject(batchId, pid);
            if (item == null) {
                throw RestException.plainNotFound(DigitalObjectResourceApi.DIGITALOBJECT_PID, pid);
            }
            if (!readonly) {
                Batch batch = importManager.get(batchId);
                ImportResource.checkBatchState(batch);
            }
            fobject = new LocalStorage().load(pid, item.getFile());
        } else {
            fobject = RemoteStorage.getInstance(appConfig).find(pid);
        }
        return fobject;
    }

}
