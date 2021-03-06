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
package cz.cas.lib.proarc.common.fedora;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yourmediashelf.fedora.client.FedoraClient;
import com.yourmediashelf.fedora.client.FedoraClientException;
import com.yourmediashelf.fedora.client.request.RiSearch;
import com.yourmediashelf.fedora.client.response.FindObjectsResponse;
import com.yourmediashelf.fedora.client.response.RiSearchResponse;
import cz.cas.lib.proarc.common.fedora.RemoteStorage.RemoteObject;
import cz.cas.lib.proarc.common.fedora.relation.RelationEditor;
import cz.cas.lib.proarc.common.fedora.relation.RelationResource;
import cz.cas.lib.proarc.common.json.JsonUtils;
import cz.cas.lib.proarc.common.object.HasDataHandler;
import cz.cas.lib.proarc.common.object.model.MetaModel;
import cz.cas.lib.proarc.common.object.model.MetaModelRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implements search queries with ITQL.
 *
 * <p>It will require an interface to implement an alternative search that
 * does not support ITQL.
 *
 * @author Jan Pokorsky
 */
public final class SearchView {

    private static final Logger LOG = Logger.getLogger(SearchView.class.getName());

    private static final String QUERY_LAST_CREATED = readQuery("lastCreated.itql");
    private static final String QUERY_FIND_BY_MODEL = readQuery("findByModel.itql");
    private static final String QUERY_FIND_MEMBERS = readQuery("findMembers.itql");
    private static final String QUERY_FIND_MEMBER_HIERARCHY = readQuery("findMemberHierarchy.itql");
    private static final String QUERY_FIND_PIDS = readQuery("findPids.itql");
    private static final String QUERY_FIND_REFERRERS = readQuery("findReferrers.itql");
    private static final String QUERY_FIND_DEVICE_REFERRERS = readQuery("findDeviceReferrers.itql");

    private final FedoraClient fedora;
    private final int maxLimit;
    private final RemoteStorage storage;
    private Locale locale = Locale.ENGLISH;
    private ObjectMapper mapper;

    SearchView(RemoteStorage storage) {
        this(storage, 100);
    }

    SearchView(RemoteStorage storage, int maxLimit) {
        this.storage = storage;
        this.fedora = storage.getClient();
        this.maxLimit = maxLimit;
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            throw new NullPointerException("locale");
        }
        this.locale = locale;
    }

    /**
     * @see #findQuery(cz.cas.lib.proarc.common.fedora.SearchView.Query)
     */
    public List<Item> findQuery(String title, String label, String identifier, String owner, String model, Collection<String> hasOwners)
            throws FedoraClientException, IOException {

        return findQuery(new Query().setTitle(title).setLabel(label)
                .setIdentifier(identifier).setOwner(owner).setModel(model)
                .setHasOwners(hasOwners));
    }

    /**
     * Finds objects matching passed fields using the Fedora Basic Search.
     * Matching objects are filtered with {@link #find(java.lang.String[]) }
     * to return only ProArc objects.
     *
     * @return limited list of objects.
     * @see <a href='https://wiki.duraspace.org/display/FEDORA35/Basic+Search'>Fedora Basic Search</a>
     */
    public List<Item> findQuery(Query q)
            throws FedoraClientException, IOException {

        final int objectsLimit = 80;
        StringBuilder query = new StringBuilder();
        if (q.getModel() != null && !q.getModel().isEmpty()) {
            query.append("type~").append(q.getModel());
        }
        // FedoraClient.findObjects() does not support OR operator!
        if (!q.getHasOwners().isEmpty()) {
            query.append(" rights~").append(q.getHasOwners().iterator().next());
        }
        buildQuery(query, "title", q.getTitle());
        buildQuery(query, "label", q.getLabel());
        buildQuery(query, "identifier", q.getIdentifier());
        buildQuery(query, "ownerId", q.getOwner());
        buildQuery(query, "creator", q.getCreator());
        final String queryString = query.toString().trim();
        LOG.fine(queryString);
        FindObjectsResponse response = FedoraClient.findObjects().query(queryString).resultFormat("xml")
                .pid()
                .maxResults(objectsLimit)
                .execute(fedora);
        List<String> pids = response.getPids();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("pids count: " + pids.size() + ", token: " + response.getToken() + ", pids: " + pids.toString());
        }

        List<Item> result = new ArrayList<Item>(maxLimit);
        while (!pids.isEmpty()) {
            List<Item> items = find(pids.toArray(new String[pids.size()]));
            result.addAll(items);
            String token = response.getToken();
            if (token == null || result.size() + objectsLimit > maxLimit) {
                break;
            }
            response = FedoraClient.findObjects().query(queryString).resultFormat("xml").pid()
                    .maxResults(objectsLimit).sessionToken(token)
                    .execute(fedora);
            pids = response.getPids();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("resumed: pids count: " + pids.size() + ", token: " + response.getToken() + ", pids: " + pids.toString());
            }
        }
        return result;
    }

    /**
     * Finds objects matching passed phrase using the Fedora Basic Search.
     * Matching objects are filtered with {@link #find(java.lang.String[]) }
     * to return only ProArc objects.
     *
     * @param phrase phrase to search in any field of the object
     * @return limited list of objects.
     * @see <a href='https://wiki.duraspace.org/display/FEDORA35/Basic+Search'>Fedora Basic Search</a>
     */
    public List<Item> findPhrase(String phrase) throws FedoraClientException, IOException {
        final int objectsLimit = 80;
        phrase = normalizePhrase(phrase);
        FindObjectsResponse response = FedoraClient.findObjects().terms(phrase).resultFormat("xml")
                .pid()
                .maxResults(objectsLimit)
                .execute(fedora);
        List<String> pids = response.getPids();
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("pids count: " + pids.size() + ", token: " + response.getToken() + ", pids: " + pids.toString());
        }
        List<Item> result = new ArrayList<Item>(maxLimit);
        while (!pids.isEmpty()) {
            List<Item> items = find(true, pids.toArray(new String[pids.size()]));
            result.addAll(items);
            String token = response.getToken();
            if (token == null || result.size() + objectsLimit > maxLimit) {
                break;
            }
            response = FedoraClient.findObjects().terms(phrase).resultFormat("xml").pid()
                    .maxResults(objectsLimit).sessionToken(token)
                    .execute(fedora);
            pids = response.getPids();
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("resumed: pids count: " + pids.size() + ", token: " + response.getToken() + ", pids: " + pids.toString());
            }
        }
        return result;
    }

    static StringBuilder buildQuery(StringBuilder builder, String field, String value) {
        if (value == null || value.isEmpty()) {
            return builder;
        }
        // remove leading and trailing white spaces and asterisks
        value = value.replaceAll("^[\\s\\*]+|[\\s\\*]+$", "");
        // Fedora query does not accept "'" char and does not allow to escape special chars *, ?
        value = value.replaceAll("['*]", "?");
        if (!value.isEmpty() && !"*".equals(value)) {
            value = "'*" + value + "*'";
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(field).append('~').append(value);
        }
        return builder;
    }

    /**
     * Removes superfluous chars a and optimizes phrase to match the most relevant records.
     * <p/>For ITQL it trims leading and trailing whitespaces and asterisks
     * and wraps the result with asterisks.
     */
    static String normalizePhrase(String phrase) {
        phrase = phrase == null ? "" : phrase;
        phrase = phrase.replaceAll("^[\\s\\*]+|[\\s\\*]+$", "");
        phrase = phrase.isEmpty() ? "*" : "*" + phrase + "*";
        return phrase;
    }

    public List<Item> find(String... pids) throws FedoraClientException, IOException {
        return find(Arrays.asList(pids), true);
    }

    public List<Item> find(boolean onlyActive, String... pids) throws FedoraClientException, IOException {
        return find(Arrays.asList(pids), onlyActive);
    }

    /**
     * Finds active descriptors of passed PIDs.
     *
     * @param pids PIDs of digital objects
     * @return list of descriptors
     * @throws FedoraClientException
     * @throws IOException
     */
    public List<Item> find(List<String> pids) throws FedoraClientException, IOException {
        return find(pids, true);
    }

    /**
     * Finds descriptors of passed PIDs.
     *
     * @param pids PIDs of digital objects
     * @param onlyActive {@code true} includes only active objects
     * @return list of descriptors
     * @throws FedoraClientException
     * @throws IOException
     */
    public List<Item> find(List<String> pids, boolean onlyActive) throws FedoraClientException, IOException {
        // issue 85: reasonable count of PIDs per query to prevent StackOverflowError.
        // Greater query page sizes (>1000, <2000) are acceptable but Mulgara responses are really slow.
        // It does not make sence to add paging to API as load on demand of SmartGWT Tree
        // does not support paging and it is not expected to have monograph or
        // issue page counts grater than 10000.
        final int queryPageSize = 100;
        final int size = pids.size();
        ArrayList<Item> result = new ArrayList<Item>(size);
        for (int startOffset = 0; startOffset < size; ) {
            int endOffset = Math.min(size, startOffset + queryPageSize);
            List<String> subList = pids.subList(startOffset, endOffset);
            List<Item> members = findImpl(subList, onlyActive);
            startOffset = endOffset;
            result.addAll(members);
        }
        return result;
    }

    List<Item> findImpl(List<String> pids, boolean onlyActive) throws FedoraClientException, IOException {
        if (pids.isEmpty()) {
            return Collections.emptyList();
        }
        StringBuilder expr = new StringBuilder(256);
        for (String pid : pids) {
            if (expr.length() > 0) {
                expr.append("\n  or ");
            }
            expr.append(String.format(
                    "$pid <http://mulgara.org/mulgara#is> <info:fedora/%s>",
                    pid));
        }
        String query = QUERY_FIND_PIDS.replace("${pids.expression}", expr);

        String onlyActiveExpr = onlyActive
                ? "and        $pid     <info:fedora/fedora-system:def/model#state>"
                        + "           <info:fedora/fedora-system:def/model#Active>"
                : "";
        query = query.replace("${includeActive}", onlyActiveExpr);

        LOG.fine(query);
        RiSearch search = buildSearch(query);
        return consumeSearch(search.execute(fedora));
    }

    /**
     * Finds children of the passed remote object. The result list is sorted
     * using RELS-EXT stream.
     * 
     * @param parent PID of parent to query
     * @return the sorted list
     * @throws FedoraClientException
     * @throws IOException
     */
    public List<Item> findSortedChildren(String parentPid)
            throws FedoraClientException, IOException, DigitalObjectException {
        
        RemoteObject parent = storage.find(parentPid);
        List<String> memberPids = new RelationEditor(parent).getMembers();
        List<Item> items = find(memberPids, true);
        ArrayList<Item> sortedItems = new ArrayList<Item>(memberPids.size());
        for (String memberPid : memberPids) {
            for (Iterator<Item> it = items.iterator(); it.hasNext();) {
                Item item = it.next();
                if (memberPid.equals(item.getPid())) {
                    sortedItems.add(item);
                    it.remove();
                    break;
                }
            }
        }
        return sortedItems;
    }

    public List<Item> findChildren(String pid) throws FedoraClientException, IOException {
        String query = QUERY_FIND_MEMBERS.replace("${parent}", RelationResource.fromPid(pid).getResource());
        RiSearch search = buildSearch(query);
        return consumeSearch(search.execute(fedora));
    }

    /**
     * Traverses a graph of PID's members.
     *
     * @param pid PID to traverse
     * @return list of all PID's members
     */
    public List<Item> findChildrenHierarchy(String pid) throws FedoraClientException, IOException {
        String query = QUERY_FIND_MEMBER_HIERARCHY.replace("${ROOT}", RelationResource.fromPid(pid).getResource());
        RiSearch search = buildSearch(query);
        return consumeSearch(search.execute(fedora));
    }

    public List<Item> findLastCreated(int offset, String model, String user) throws FedoraClientException, IOException {
        return findLastCreated(offset, model, user, 100);
    }
    
    public List<Item> findLastCreated(int offset, String model, String user, int limit) throws FedoraClientException, IOException {
        return findLast(offset, model, user, limit, "$created desc");
    }

    public List<Item> findLastModified(int offset, String model, String user, int limit) throws FedoraClientException, IOException {
        return findLast(offset, model, user, limit, "$modified desc");
    }

    private List<Item> findLast(int offset, String model, String user, int limit, String orderBy) throws FedoraClientException, IOException {
        String modelFilter = "";
        String ownerFilter = "";
        if (model != null && !model.isEmpty()) {
            modelFilter = String.format("and        $pid     <info:fedora/fedora-system:def/model#hasModel>        <info:fedora/%s>", model);
        }
        if (user != null) {
            ownerFilter = String.format("and        $pid     <http://proarc.lib.cas.cz/relations#hasOwner>        $group\n"
                    + "and        <info:fedora/%s>     <info:fedora/fedora-system:def/relations-external#isMemberOf>        $group", user);
        }
        String query = QUERY_LAST_CREATED.replace("${OFFSET}", String.valueOf(offset));
        query = query.replace("${MODEL_FILTER}", modelFilter);
        query = query.replace("${OWNER_FILTER}", ownerFilter);
        query = query.replace("${ORDERBY}", orderBy);
        LOG.fine(query);
        RiSearch search = buildSearch(query);

        if (limit > 0) {
            limit = Math.min(limit, maxLimit);
            search.limit(limit);
        }
        return consumeSearch(search.execute(fedora));
    }

    public List<Item> findReferrers(String pid) throws IOException, FedoraClientException {
        String query = QUERY_FIND_REFERRERS.replace("${PID}", pid);
        RiSearch search = buildSearch(query);
        return consumeSearch(search.execute(fedora));
    }

    /**
     * Find objects that have the given model.
     * @param modelId model PID to query
     * @return list of objects
     * @throws IOException
     * @throws FedoraClientException
     */
    public List<Item> findByModel(String modelId) throws IOException, FedoraClientException {
        String query = QUERY_FIND_BY_MODEL.replace("${metaModelPid}", modelId);
        RiSearch search = buildSearch(query);
        search.limit(1000);
        return consumeSearch(search.execute(fedora));
    }

    /**
     * Is the device referred with {@code hasDevice} relation by any digital object?
     * @param deviceId device PID
     * @return {@code true} if it is connected
     * @throws IOException
     * @throws FedoraClientException
     */
    public boolean isDeviceInUse(String deviceId) throws IOException, FedoraClientException {
        String query = QUERY_FIND_DEVICE_REFERRERS.replace("${devicePid}", deviceId);
        RiSearch search = buildSearch(query);
        search.limit(1);
        search.stream(true);
        List<Item> result = consumeSearch(search.execute(fedora));
        return !result.isEmpty();
    }

    private List<Item> consumeSearch(RiSearchResponse response) throws IOException {
        String json = response.getEntity(String.class);
        Result result = readResponse(json);
        return consumeSearch(result.results);
    }

    Result readResponse(String json) throws IOException {
        if (mapper == null) {
            // requires mapper without mix in annotation of Item
            mapper = JsonUtils.createObjectMapper();
        }
        return  mapper.readValue(json, Result.class);
    }

    private List<Item> consumeSearch(List<Item> items) {
        for (Item item : items) {
            replaceUriWithPid(item);
            resolveObjectLabel(item);
        }
        return items;
    }
    
    private static String replaceUriWithPid(String uri) {
        return uri == null ? uri : RelationResource.toPid(uri);
    }

    private static Item replaceUriWithPid(Item item) {
        item.pid = replaceUriWithPid(item.pid);
        item.model = replaceUriWithPid(item.model);
        item.state = replaceUriWithPid(item.state);
        return item;
    }
    void resolveObjectLabel(Item item) {
        // XXX implement a plugin cache
        MetaModel model = MetaModelRepository.getInstance().find(item.getModel());
        if (model == null) {
            // other than digital object model (device, ...)
            return ;
        }
        HasSearchViewHandler hasHandler = model.getPlugin().getHandlerProvider(HasSearchViewHandler.class);
        if (hasHandler != null) {
            String label = hasHandler.createSearchViewHandler().getObjectLabel(item, locale);
            item.setLabel(label);
        }
    }

    private static RiSearch buildSearch(String query) {
        RiSearch search = FedoraClient.riSearch(query).distinct(true)
                .type("tuples").lang("itql")
                .flush(true) // required to get reliable responses
//                .format("json")
                .xParam("format", "json");
        return search;
    }

    private static String readQuery(String file) {
        InputStream is = SearchView.class.getResourceAsStream(file);
        if (is == null) {
            throw new IllegalStateException(file + " not found!");
        }
        Scanner scanner = new Scanner(is, "UTF-8").useDelimiter("\\A");
        String content = scanner.next();
        scanner.close();
        return content;
    }

    /**
     * A plug-in capability.
     */
    public interface HasSearchViewHandler extends HasDataHandler {
        SearchViewHandler createSearchViewHandler();
    }

    /**
     * Implement to customize a result label of a search.
     */
    public interface SearchViewHandler {
        String getObjectLabel(Item item, Locale locale);
    }

    public static class Item {

        private String pid;
        private String model;
        private String owner;
        private String label;
        private String state;
        private String created;
        private String modified;
        /** Parent PID. Optional for some queries */
        private String parent;
        /** batch import ID. Optional for some queries */
        private Integer batchId;
        /**
         * Synthetic name of count query. count(hasExport)
         * @see <a href='http://docs.mulgara.org/itqlcommands/select.html#o194'>
         *      Count Function</a>
         */
        private String k0;

        public Item() {
        }

        public Item(String pid) {
            this.pid = pid;
        }

        public String getCreated() {
            return created;
        }

        public void setCreated(String created) {
            this.created = created;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getModified() {
            return modified;
        }

        public void setModified(String modified) {
            this.modified = modified;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getParentPid() {
            return parent;
        }

        public void setParentPid(String parentPid) {
            this.parent = parentPid;
        }

        public Integer getBatchId() {
            return batchId;
        }

        public void setBatchId(Integer batchId) {
            this.batchId = batchId;
        }

        public String getK0() {
            return k0;
        }

        public void setK0(String k0) {
            this.k0 = k0;
        }

        public Integer getHasExport() {
            if (k0 != null && !k0.isEmpty()) {
                try {
                    return Integer.parseInt(k0);
                } catch (NumberFormatException ex) {
                    // ignore
                }
            }
            return null;
        }

    }

    static class Result {

        private List<Item> results;

        public List<Item> getResults() {
            return results;
        }

        public void setResults(List<Item> results) {
            this.results = results;
        }

    }

    public static class Query {

        private String title;
        private String creator;
        private String label;
        private String identifier;
        private String owner;
        private String model;
        private Collection<String> hasOwners;

        public String getTitle() {
            return title;
        }

        public Query setTitle(String title) {
            this.title = title;
            return this;
        }

        public String getCreator() {
            return creator;
        }

        public Query setCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public String getLabel() {
            return label;
        }

        public Query setLabel(String label) {
            this.label = label;
            return this;
        }

        public String getIdentifier() {
            return identifier;
        }

        public Query setIdentifier(String identifier) {
            this.identifier = identifier;
            return this;
        }

        public String getOwner() {
            return owner;
        }

        public Query setOwner(String owner) {
            this.owner = owner;
            return this;
        }

        public String getModel() {
            return model;
        }

        public Query setModel(String model) {
            this.model = model;
            return this;
        }

        public Collection<String> getHasOwners() {
            return hasOwners != null ? hasOwners : Collections.<String>emptyList();
        }

        public Query setHasOwners(Collection<String> hasOwners) {
            this.hasOwners = hasOwners;
            return this;
        }

    }

}
