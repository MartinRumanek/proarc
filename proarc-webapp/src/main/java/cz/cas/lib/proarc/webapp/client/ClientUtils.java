/*
 * Copyright (C) 2011 Jan Pokorsky
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
package cz.cas.lib.proarc.webapp.client;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.regexp.shared.RegExp;
import com.google.gwt.regexp.shared.SplitResult;
import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criterion;
import com.smartgwt.client.data.DataSourceField;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.data.RecordList;
import com.smartgwt.client.data.ResultSet;
import com.smartgwt.client.i18n.SmartGwtMessages;
import com.smartgwt.client.types.OperatorId;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.JSONEncoder;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.Layout;
import com.smartgwt.client.widgets.tile.TileGrid;
import cz.cas.lib.proarc.webapp.client.widget.mods.NdkFormGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GWT client helper utilities.
 *
 * @author Jan Pokorsky
 */
public final class ClientUtils {

    private static final Logger LOG = Logger.getLogger(ClientUtils.class.getName());
    private static final EnumMap<OperatorId, String> OPERATORS;
    static {
        OPERATORS = new EnumMap<OperatorId, String>(OperatorId.class);
        OPERATORS.put(OperatorId.LESS_OR_EQUAL, "<=");
        OPERATORS.put(OperatorId.LESS_THAN, "<");
        OPERATORS.put(OperatorId.GREATER_OR_EQUAL, ">=");
        OPERATORS.put(OperatorId.GREATER_THAN, ">");
        OPERATORS.put(OperatorId.EQUALS, "=");
    }

    /**
     * Helper to get modified {@link SmartGwtMessages }.
     * <p> It should be used instead of {@code GWT.create(SmartGwtMessages.class)}.
     */
    public static SmartGwtMessages createSmartGwtMessages() {
        ModifiedSmartGwtMessages i18nSmartGwt = GWT.create(ModifiedSmartGwtMessages.class);
        return i18nSmartGwt;
    }

    /**
     * Simplified version of {@link String#format(java.lang.String, java.lang.Object[]) String.format}
     * For now it supports only {@code %s} format specifier.
     */
    public static String format(String format, Object... args) {
        RegExp re = RegExp.compile("%s");
        SplitResult split = re.split(format);
        StringBuilder sb = new StringBuilder();
        sb.append(split.get(0));
        for (int i = 1; i < split.length(); i++) {
            sb.append(args[i - 1]);
            sb.append(split.get(i));
        }
        return sb.toString();
    }

    /**
     * Logs a formatted message.
     * @param logger logger
     * @param level logging level
     * @param format see {@link #format(java.lang.String, java.lang.Object[]) format} doc
     * @param args arguments referenced by the format specifiers
     */
    public static void log(Logger logger, Level level, String format, Object... args) {
        if (logger.isLoggable(level)) {
            logger.log(level, format(format, args));
        }
    }

    /** Info {@link #log(java.util.logging.Logger, java.util.logging.Level, java.lang.String, java.lang.Object[]) log}. */
    public static void info(Logger logger, String format, Object... args) {
        log(logger, Level.INFO, format, args);
    }

    /** Fine {@link #log(java.util.logging.Logger, java.util.logging.Level, java.lang.String, java.lang.Object[]) log}. */
    public static void fine(Logger logger, String format, Object... args) {
        log(logger, Level.FINE, format, args);
    }

    /** Severe {@link #log(java.util.logging.Logger, java.util.logging.Level, java.lang.String, java.lang.Object[]) log}. */
    public static void severe(Logger logger, String format, Object... args) {
        log(logger, Level.SEVERE, format, args);
    }

    /** Warning {@link #log(java.util.logging.Logger, java.util.logging.Level, java.lang.String, java.lang.Object[]) log}. */
    public static void warning(Logger logger, String format, Object... args) {
        log(logger, Level.WARNING, format, args);
    }

    /**
     * Dumps Element content and traverse its children.
     * <p/><b>WARNING:</b> it is com.google.gwt.dom.client.Element not com.google.gwt.xml.client.Element!!!
     * 
     * @param elm an element to dump
     * @param indent row indentation for current level
     * @param indentIncrement increment for next level
     * @param sb dumped content
     * @return dumped content
     */
    public static StringBuilder dump(Element elm, String indent, String indentIncrement, StringBuilder sb) {
        int childCount = elm.getChildCount();
        String innerText = elm.getInnerText();
        String lang = elm.getLang();
        String nodeName = elm.getNodeName();
        short nodeType = elm.getNodeType();
        String getString = elm.getString();
        String tagNameWithNS = elm.getTagName();
        String xmlLang = elm.getAttribute("xml:lang");

        sb.append(ClientUtils.format("%sElement {nodeName: %s, nodeType: %s, tagNameWithNS: %s, lang: %s,"
                + " childCount: %s, getString: %s, xmlLang: %s}\n",
                indent, nodeName, nodeType, tagNameWithNS, lang, childCount, getString, xmlLang));
        NodeList<Node> childNodes = elm.getChildNodes();
        indent += indentIncrement;
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.getItem(i);
            if (Element.is(child)) {
                dump(Element.as(child), indent, indentIncrement, sb);
            } else {
                sb.append(ClientUtils.format("%sNode: nodeType: %s, nodeName: %s, childCount: %s, nodeValue: %s\n",
                        indent, child.getNodeType(), child.getNodeName(), child.getChildCount(), child.getNodeValue()));
            }
        }
        return sb;
    }
    
    public static String dump(Record r, String msg) {
        StringBuilder sb = new StringBuilder();
        if (r == null) {
            return ClientUtils.format("%s, record is NULL", msg);
        }
        sb.append(ClientUtils.format("%s, getAttributes:\n", msg));
        for (String attr : r.getAttributes()) {
            try {
                Object value = r.getAttributeAsObject(attr);
                sb.append(ClientUtils.format("  attr: %s, value: %s, class: %s\n", attr, value, safeGetClass(value)));
            } catch (Exception ex) {
                String value = r.getAttribute(attr);
                sb.append(ClientUtils.format("  !FAILED: attr: %s, value: %s, class: %s\n", attr, value, safeGetClass(value)));
//                Logger.getLogger("").log(Level.SEVERE, attr, ex);
            }
        }

        sb.append("-- toMap:\n");
        Map<?, ?> m;
        try {
            m = r.toMap();
        } catch (Exception ex) {
//            Logger.getLogger("").log(Level.SEVERE, "Record.toMap", ex);
            sb.append("Record.toMap FAILED");
            return sb.toString();
        }
        dump(m, "  ", "  ", sb);

        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object value = e.getValue();
            sb.append(ClientUtils.format("  map.key: %s, value: %s, value class %s\n", e.getKey(), value, safeGetClass(value)));
            if (value instanceof List) {
                List<?> l = (List) value;
                for (Object valItem : l) {
                    sb.append(ClientUtils.format("    item.value: %s, value class %s\n", valItem, safeGetClass(valItem)));
                }
            }
        }

        return sb.toString();
    }

    public static StringBuilder dump(List<?> l, String indent, String indentIncrement, StringBuilder sb) {
        for (Object valItem : l) {
            sb.append(ClientUtils.format("%sitem.value: %s, value class %s\n", indent, valItem, safeGetClass(valItem)));
        }
        return sb;
    }

    public static StringBuilder dump(Map<?, ?> m, String indent, String indentIncrement, StringBuilder sb) {
        for (Map.Entry<?, ?> e : m.entrySet()) {
            Object value = e.getValue();
            sb.append(ClientUtils.format("%smap.key: %s, value: %s, value class %s\n", indent, e.getKey(), value, safeGetClass(value)));
            if (value instanceof List) {
                dump((List) value, indent, indentIncrement, sb);
            } else if (value instanceof Map) {
                dump((Map) value, indent + indentIncrement, indentIncrement, sb);
            }
        }
        return sb;
    }

    /** dumps object in JSON */
    public static String dump(Object jso) {
        String dump;
        if (jso != null) {
            if (jso instanceof Record) {
                jso = ((Record) jso).getJsObj();
            } else if (jso instanceof RecordList) {
                jso = ((RecordList) jso).getJsObj();
            }
            try {
                dump = new JSONEncoder().encode(jso);
            } catch (Exception ex) {
                // this occurs in development mode sometimes; log it silently
                dump = String.valueOf(jso) + ", NPE: raise log level for details.";
                LOG.log(Level.FINE, dump, ex);
            }
        } else {
            dump = String.valueOf(jso);
        }
        return dump;
    }

    /**
     * Same like {@code value.geClass()} but prevents {@link NullPointerException}.
     *
     * @return class or {@code null}
     */
    public static Class<?> safeGetClass(Object value) {
        return value != null ? value.getClass() : null;
    }

    /**
     * Helper to scroll to a particular tile.
     * @param grid a tile grid
     * @param tileIndex index of the tile
     */
    public static void scrollToTile(TileGrid grid, int tileIndex) {
        int tileHeight = grid.getTileHeight();
        int tilesPerLine = grid.getTilesPerLine();
        int tileHMargin = grid.getTileHMargin();

        int tileRow = tileIndex / tilesPerLine - 1;
        int tileColumn = tileIndex % tilesPerLine;
        int top = tileHMargin / 2 + (tileRow * (tileHeight + tileHMargin));
        grid.scrollTo(1, top);
    }

    /**
     * Sets layout members just in case they differ from current members.
     */
    public static void setMembers(Layout l, Canvas... members) {
        Canvas[] oldies = l.getMembers();
        if (!Arrays.equals(oldies, members)) {
            l.setMembers(members);
        }
    }

    /**
     * Removes all attributes with {@code null} value.
     *
     * Useful before passing record to {@link DataSource} that encodes {@code null}
     * attributes as {@code "name":"null"} in JSON.
     * @param r record to process
     * @return copy of the record without {@code null} attributes
     */
    public static Record removeNulls(Record r) {
        boolean hasNull = false;
        HashMap<Object, Object> nonNunlls = new HashMap<Object, Object>();
        Map<?, ?> recordMap = r.toMap();
        for (Map.Entry<?, ?> entry : recordMap.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Collection && ((Collection) value).isEmpty()) {
                hasNull = true;
            } else if (value instanceof String && ((String) value).isEmpty()) {
                hasNull = true;
            } else if ("__ref".equals(entry.getKey())) {
                // ignore GWT attributes
                hasNull = true;
            } else if (value != null) {
                nonNunlls.put(entry.getKey(), value);
            } else {
                hasNull = true;
            }
        }
        return hasNull ? new Record(nonNunlls) : r;
    }

    /**
     * Removes empty and unrelated nodes from the data tree.
     * It skips empty collections, strings, synthetic GWT/SmartGWT/ProArc attributes.
     * @param r data to normalize
     * @return the reduced data tree or {@code null} if none value remains
     */
    public static Record normalizeData(Record r) {
        Map<?,?> m = r.toMap();
        m = normalizeData(m);
        return m == null ? null : new Record(m);
    }

    /**
     * Normalizes a map of values. It skips empty collections, strings, synthetic
     * GWT/SmartGWT/ProArc attributes.
     * @param m the map to normalize
     * @return the normalized map or {@code null} if none value remains
     */
    private static Map<?,?> normalizeData(Map m) {
        for (Iterator<Map.Entry> it = m.entrySet().iterator(); it.hasNext();) {
            Entry entry = it.next();
            Object value = entry.getValue();
            if ("__ref".equals(entry.getKey())) {
                // ignore GWT attributes
                it.remove();
            } else if (NdkFormGenerator.HIDDEN_FIELDS_NAME.equals(entry.getKey())) {
                it.remove();
            } else if (value instanceof List) {
                List list = (List) value;
                Object data = normalizeData(list);
                if (data == null || value instanceof Collection && ((Collection) value).isEmpty()) {
                    it.remove();
                } else {
                    entry.setValue(data);
                }
            } else if (value instanceof Collection || value instanceof Map) {
                // GWT 2.5.1: fix instanceof to handle Map together with Collection here
                value = normalizeObjectData(value);
                if (value instanceof Collection && ((Collection) value).isEmpty()) {
                    it.remove();
                }
            } else if (value instanceof String && ((String) value).isEmpty()) {
                it.remove();
            } else if (value != null) {
                // no-op
            } else {
                it.remove();
            }
        }
        if (m.isEmpty()) {
            return null;
        }
        return m;
    }

    /**
     * Normalizes a list of items. It it skips empty collections, strings, ....
     * @param l the list to normalize
     * @return the list of normalized items or the normalized item
     *      or {@code null} if none value remains
     */
    private static Object normalizeData(List l) {
        for (Iterator it = l.iterator(); it.hasNext();) {
            Object value = it.next();
            value = normalizeObjectData(value);
            if (value instanceof Collection && ((Collection) value).isEmpty()) {
                it.remove();
            } else if (value instanceof String && ((String) value).isEmpty()) {
                it.remove();
            } else if (value != null) {
                // no-op
            } else {
                it.remove();
            }
        }
        if (l.isEmpty()) {
            return null;
        } else if (l.size() == 1) {
            return l.get(0);
        }
        return l;
    }

    private static Object normalizeObjectData(Object obj) {
        Object result;
        if (obj instanceof Record) {
            result = normalizeData((Record) obj);
        } else if (obj instanceof Map) {
            result = normalizeData((Map) obj);
        } else if (obj instanceof List) {
            result = normalizeData((List) obj);
        } else {
            result = obj;
        }
        return result;
    }

    /**
     * Replacement for {@link ResultSet#getRange(int, int) } to work around
     * {@link ArrayStoreException} thrown in production mode.
     * 
     * @see <a href='http://forums.smartclient.com/showpost.php?p=85402&postcount=34'>proposed workaround</a>
     * @see <a href='http://forums.smartclient.com/showpost.php?p=85402&postcount=38'>Fixed in SmartGWT 3.1</a>
     * @since SmartGWT 3.0
     */
    public static void getRangeWorkAround(ResultSet resultSet, int start, int end) {
        try {
            resultSet.getRange(start, end);
        } catch (ArrayStoreException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Helper to get value map from the result set as a linked hash map.
     * It specifies origin contract of {@link ResultSet#getValueMap(String, String) ResultSet.getValueMap}
     * to make the map usable for e.g.
     * {@link com.smartgwt.client.widgets.form.fields.FormItem#setValueMap(java.util.LinkedHashMap) FormItem}.
     *
     * @param rs result set
     * @param idField field name holding IDs
     * @param displayField field name holding display values
     * @return linked map
     */
    public static LinkedHashMap<?, ?> getValueMap(ResultSet rs, String idField, String displayField) {
        Map<?, ?> valueMap = rs.getValueMap(idField, displayField);
        // ResultSet implementation (SmartGWT 3.0) returns the map as LinkedHashMap,
        // no extra processing required for now
        return (LinkedHashMap<?, ?>) valueMap;
    }

    public static final BooleanCallback EMPTY_BOOLEAN_CALLBACK = new BooleanCallback() {

        @Override
        public void execute(Boolean value) {
            // no op
        }
    };

    private static final Callback<?,?> EMPTY_CALLBACK = new Callback<Object, Object>() {

        @Override
        public void onFailure(Object reason) {
        }

        @Override
        public void onSuccess(Object result) {
        }
    };

    @SuppressWarnings("unchecked")
    public static <T,F> Callback<T,F> emptyCallback() {
        return (Callback<T, F>) EMPTY_CALLBACK;
    }

    /**
     * Copies given attribute from each record to standalone array.
     */
    public static String[] toFieldValues(Record[] records, String attributeName) {
        String[] items = new String[records.length];
        for (int i = 0; i < records.length; i++) {
            items[i] = records[i].getAttribute(attributeName);
        }
        return items;
    }

    /**
     * Gets advanced criteria as HTTP GET params.
     * @param ac advanced criteria
     * @param map map of params
     */
    public static void advanceCriteriaAsParams(AdvancedCriteria ac, HashMap<String, Object> map, HashMap<String, String> dateFields) {
        Criterion[] criteria = ac.getCriteria();
        if (criteria == null) {
            return ;
        }
        for (Criterion criterion : criteria) {
            String fieldName = criterion.getFieldName();
            if (criterion.isAdvanced() && fieldName == null) {
                advanceCriteriaAsParams(criterion.asAdvancedCriteria(), map, dateFields);
            } else {
                String filterName = dateFields.get(fieldName);
                if (filterName != null) {
                    ArrayList<Object> dates = (ArrayList<Object>) map.get(filterName);
                    if (dates == null) {
                        dates = new ArrayList<Object>();
                        map.put(filterName, dates);
                    }
                    String operatorStr = OPERATORS.get(criterion.getOperator());
                    if (operatorStr != null) {
                        dates.add(operatorStr);
                    }
                    dates.add(criterion.getValueAsDate());
                } else {
                    // skip null values
                    String cval = criterion.getValueAsString();
                    if (cval != null) {
                        map.put(fieldName, cval);
                    }
                }
            }
        }
    }

    public static final class DataSourceFieldBuilder<T extends DataSourceField> {

        public static final OperatorId[] TEXT_OPERATIONS = {
            OperatorId.EQUALS, OperatorId.NOT_EQUAL,
            OperatorId.ICONTAINS, OperatorId.INOT_CONTAINS,
            OperatorId.ISTARTS_WITH, OperatorId.IENDS_WITH};

        private final T field;

        public static <T extends DataSourceField> DataSourceFieldBuilder<T> field(T field) {
            return new DataSourceFieldBuilder<T>(field);
        }

        public DataSourceFieldBuilder(T field) {
            this.field = field;
        }

        public DataSourceFieldBuilder<T> required() {
            field.setRequired(true);
            return this;
        }

        public DataSourceFieldBuilder<T> primaryKey() {
            field.setPrimaryKey(true);
            return this;
        }

        public DataSourceFieldBuilder<T> hidden() {
            field.setHidden(true);
            return this;
        }

        public DataSourceFieldBuilder<T> filter(boolean filter) {
            field.setCanFilter(filter);
            return this;
        }

        public DataSourceFieldBuilder<T> validOperators(OperatorId... ids) {
            field.setValidOperators(ids);
            return this;
        }
        
        public T build() {
            return field;
        }

    }

    /**
     * Helper to wait on running asynchronous tasks.
     * It runs as soon as all expectations are released.
     */
    public static abstract class SweepTask implements Runnable, BooleanCallback {

        private int semaphore = 0;

        /**
         * Provides task implementation.
         */
        protected abstract void processing();

        /**
         * Call to expect further async task.
         */
        public SweepTask expect() {
            semaphore++;
            return this;
        }

        /**
         * Call when some async task is done.
         */
        public void release() {
            semaphore--;
            if (semaphore == 0) {
                processing();
            }
        }

        /**
         * Async call of release in case the value is true.
         */
        @Override
        public void execute(Boolean value) {
            if (Boolean.TRUE.equals(value)) {
                release();
            }
        }

        /**
         * Async call of release.
         * @param value
         */
        @Override
        public void run() {
            release();
        }


    }

}
