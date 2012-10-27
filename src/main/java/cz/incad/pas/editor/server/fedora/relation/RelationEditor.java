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
package cz.incad.pas.editor.server.fedora.relation;

import cz.incad.pas.editor.server.fedora.DigitalObjectException;
import cz.incad.pas.editor.server.fedora.FedoraObject;
import cz.incad.pas.editor.server.fedora.LocalStorage.LocalObject;
import cz.incad.pas.editor.server.fedora.LocalStorage.LocalXmlStreamEditor;
import cz.incad.pas.editor.server.fedora.RemoteStorage.RemoteObject;
import cz.incad.pas.editor.server.fedora.RemoteStorage.RemoteXmlStreamEditor;
import cz.incad.pas.editor.server.fedora.XmlStreamEditor;
import cz.incad.pas.editor.server.fedora.XmlStreamEditor.EditorResult;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import org.w3c.dom.Element;

/**
 * RDF relations editor.
 *
 * @see <a href='https://wiki.duraspace.org/display/FEDORA35/Digital+Object+Relationships'>
 *      Digital Object Relationships</a>
 * @see <a href='https://wiki.duraspace.org/display/FEDORA35/Resource+Index'>
 *      Resource Index</a>
 * @author Jan Pokorsky
 */
public final class RelationEditor {

    public static final String DATASTREAM_ID = "RELS-EXT";
    public static final String DATASTREAM_FORMAT_URI = "info:fedora/fedora-system:FedoraRELSExt-1.0";
    public static final String DATASTREAM_LABEL = "RDF Statements about this object";

    private final XmlStreamEditor editor;
    private final FedoraObject fobject;
    private Rdf relsExt;

    public RelationEditor(FedoraObject fobject) {
        this.fobject = fobject;
        this.editor = createEditor(fobject);
    }

    public long getLastModified() throws DigitalObjectException {
        return editor.getLastModified();
    }

    /**
     * @param model PID of the model
     */
    public void setModel(String model) throws DigitalObjectException {
        Rdf rdf = getRdf();
        rdf.getDescription().setModel(model);
    }

    /**
     * @return PID of the model
     */
    public String getModel() throws DigitalObjectException {
        Rdf rdf = getRdf();
        return rdf.getDescription().getModel().getModelPid();
    }

    /**
     * Relations defining object hierarchy graph.
     *
     * @return list of PIDs or {@code null} if Fedora object not found.
     */
    public List<String> getMembers() throws DigitalObjectException {
        Rdf rdf = getRdf();
        List<HasMemberRelation> members = rdf.getDescription().getMemberRelations();
        ArrayList<String> result = new ArrayList<String>(members.size());
        for (HasMemberRelation hasMember : members) {
            result.add(hasMember.getMember());
        }
        return result;
    }

    /**
     * Sets relations defining object hierarchy graph.
     * Relations should be ordered for each model.
     *
     * @param members list of PIDs
     */
    public void setMembers(List<String> members) throws DigitalObjectException {
        Rdf rdf = getRdf();
        ArrayList<HasMemberRelation> relations = new ArrayList<HasMemberRelation>(members.size());
        for (String member : members) {
            relations.add(new HasMemberRelation(member));
        }
        List<HasMemberRelation> oldies = rdf.getDescription().getMemberRelations();
        oldies.clear();
        oldies.addAll(relations);
    }

    /**
     * Gets unrecognized relations.
     *
     * @return list of relations
     */
    public List<Element> getRelations() throws DigitalObjectException {
        Rdf rdf = getRdf();
        List<Element> elms = rdf.getDescription().getRelations();
        return new ArrayList<Element>(elms);
    }

    /**
     * Sets relations unrecognized by RelationEditor.
     * <b>Do not use for members, model, ...</b>
     * 
     * @param elms list of custom relations
     */
    public void setRelations(List<Element> elms) throws DigitalObjectException {
        Rdf rdf = getRdf();
        List<Element> relations = rdf.getDescription().getRelations();
        relations.clear();
        relations.addAll(elms);
    }

    /**
     * Prepares updates for {@link FedoraObject#flush() }
     * @param timestamp timestamp
     */
    public void write(long timestamp) throws DigitalObjectException {
        EditorResult result = editor.createResult();
        Relations.marshal(result, relsExt, false);
        editor.write(result, timestamp);
    }

    private Rdf getRdf() throws DigitalObjectException {
        if (relsExt != null) {
            return relsExt;
        }
        Source source = editor.read();
        if (source == null) {
            if (fobject instanceof RemoteObject) {
                // it should never arise; broken Fedora?
                throw new DigitalObjectException(fobject.getPid(), "missing RELS-EXT!");
            }
            relsExt = new Rdf(fobject.getPid());
        } else {
            relsExt = Relations.unmarshal(source, Rdf.class);
        }

        return relsExt;
    }

    private static XmlStreamEditor createEditor(FedoraObject object) {
        XmlStreamEditor editor;
        if (object instanceof LocalObject) {
            editor = new LocalXmlStreamEditor((LocalObject) object, DATASTREAM_ID, DATASTREAM_FORMAT_URI, DATASTREAM_LABEL);
        } else if (object instanceof RemoteObject) {
            editor = new RemoteXmlStreamEditor(
                    (RemoteObject) object,
                    RemoteXmlStreamEditor.inlineProfile(DATASTREAM_ID, DATASTREAM_FORMAT_URI, DATASTREAM_LABEL));
        } else {
            throw new IllegalArgumentException("Unsupported fedora object: " + object.getClass());
        }
        return editor;
    }
}
