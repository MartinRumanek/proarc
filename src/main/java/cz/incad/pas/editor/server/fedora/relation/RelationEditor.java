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

    public long getLastModified() {
        return editor.getLastModified();
    }

    /**
     * @param model PID of the model
     */
    public void setModel(String model) {
        Rdf rdf = getRdf();
        if (rdf == null) {
            relsExt = rdf = new Rdf(fobject.getPid());
        }
        rdf.getDescription().setModel(model);
    }

    /**
     * @return PID of the model
     */
    public String getModel() {
        Rdf rdf = getRdf();
        return rdf.getDescription().getModel().getModelPid();
    }

    /**
     * Relations defining object hierarchy graph.
     *
     * @return list of PIDs or {@code null} if Fedora object not found.
     */
    public List<String> getMembers() {
        Rdf rdf = getRdf();
        if (rdf != null) {
            List<HasMemberRelation> members = rdf.getDescription().getMemberRelations();
            ArrayList<String> result = new ArrayList<String>(members.size());
            for (HasMemberRelation hasMember : members) {
                result.add(hasMember.getMember());
            }
            return result;
        }
        return null;
    }

    /**
     * Sets relations defining object hierarchy graph.
     * Relations should be ordered for each model.
     *
     * @param members list of PIDs
     */
    public void setMembers(List<String> members) {
        Rdf rdf = getRdf();
        if (rdf == null) {
            relsExt = rdf = new Rdf(fobject.getPid());
        }
        ArrayList<HasMemberRelation> relations = new ArrayList<HasMemberRelation>(members.size());
        for (String member : members) {
            relations.add(new HasMemberRelation(member));
        }
        List<HasMemberRelation> oldies = rdf.getDescription().getMemberRelations();
        oldies.clear();
        oldies.addAll(relations);
    }

    /**
     * Prepares updates for {@link FedoraObject#flush() }
     * @param timestamp timestamp
     */
    public void write(long timestamp) {
        EditorResult result = editor.createResult();
        Relations.marshal(result, relsExt, false);
        editor.write(result, timestamp);
    }

//    public void getRelations() {
//        relsExt.getDescription().getRelations();
//    }

    private Rdf getRdf() {
        if (relsExt != null) {
            return relsExt;
        }
        Source source = editor.read();
        if (source == null) {
            if (fobject instanceof RemoteObject) {
                throw new IllegalStateException("missing RELS-EXT!");
            }
            return null;
        }

        relsExt = Relations.unmarshal(source, Rdf.class);
        return relsExt;
    }

    private static XmlStreamEditor createEditor(FedoraObject object) {
        XmlStreamEditor editor;
        if (object instanceof LocalObject) {
            editor = new LocalXmlStreamEditor((LocalObject) object, DATASTREAM_ID, DATASTREAM_FORMAT_URI, DATASTREAM_LABEL);
        } else if (object instanceof RemoteObject) {
            editor = new RemoteXmlStreamEditor((RemoteObject) object, DATASTREAM_ID);
        } else {
            throw new IllegalArgumentException("Unsupported fedora object: " + object.getClass());
        }
        return editor;
    }
}
