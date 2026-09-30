package org.fentanylsolutions.thaumicdabblery.feature.scanall;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A compact wildcard in Thaumcraft's saved scan history. Iteration and serialization retain only real entries;
 * membership checks additionally recognize all native numeric scan keys while the completion marker exists.
 */
public final class AllScanHistory extends ArrayList<String> {

    private static final long serialVersionUID = 1L;
    public static final String MARKER = "#THAUMICDABBLERY.ALL_SCANS";

    public AllScanHistory(Collection<String> entries) {
        super(entries);
    }

    @Override
    public boolean contains(Object key) {
        if (super.contains(key)) return true;
        return numericScanKey(key) && super.contains(MARKER);
    }

    private static boolean numericScanKey(Object key) {
        if (!(key instanceof String)) return false;
        String text = (String) key;
        if (text.length() < 2 || (text.charAt(0) != '@' && text.charAt(0) != '#')) return false;
        try {
            Integer.parseInt(text.substring(1));
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
