package org.fentanylsolutions.thaumicdabblery.compat.modtweaker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategoryList;

public final class ResearchTabOrderRegistry {

    private static List<String> activeOrder = Collections.emptyList();

    private ResearchTabOrderRegistry() {}

    public static synchronized Change set(List<String> categoryOrder) {
        List<String> previousOrder = activeOrder;
        List<String> previousConcreteOrder = getConcreteOrder();
        List<String> appliedOrder = immutableCopy(categoryOrder);

        activeOrder = appliedOrder;
        applyOrder(appliedOrder);
        return new OrderChange(appliedOrder, previousOrder, previousConcreteOrder);
    }

    public static synchronized void reapply() {
        if (!activeOrder.isEmpty()) {
            applyOrder(activeOrder);
        }
    }

    public static synchronized void onCategoryRegistered(String categoryKey) {
        if (activeOrder.contains(categoryKey)) {
            applyOrder(activeOrder);
        }
    }

    private static List<String> getConcreteOrder() {
        return new ArrayList<>(ResearchCategories.researchCategories.keySet());
    }

    private static List<String> immutableCopy(List<String> categoryOrder) {
        return Collections.unmodifiableList(new ArrayList<>(categoryOrder));
    }

    private static void applyOrder(List<String> categoryOrder) {
        LinkedHashMap<String, ResearchCategoryList> categories = ResearchCategories.researchCategories;
        LinkedHashMap<String, ResearchCategoryList> reordered = new LinkedHashMap<>();

        for (String categoryKey : categoryOrder) {
            ResearchCategoryList category = categories.get(categoryKey);
            if (category != null) {
                reordered.put(categoryKey, category);
            }
        }
        for (Map.Entry<String, ResearchCategoryList> category : categories.entrySet()) {
            if (!reordered.containsKey(category.getKey())) {
                reordered.put(category.getKey(), category.getValue());
            }
        }

        ResearchCategories.researchCategories = reordered;
    }

    public interface Change {

        void undo();
    }

    private static final class OrderChange implements Change {

        private final List<String> appliedOrder;
        private final List<String> previousOrder;
        private final List<String> previousConcreteOrder;

        private boolean undone;

        private OrderChange(List<String> appliedOrder, List<String> previousOrder, List<String> previousConcreteOrder) {
            this.appliedOrder = appliedOrder;
            this.previousOrder = previousOrder;
            this.previousConcreteOrder = previousConcreteOrder;
        }

        @Override
        public void undo() {
            synchronized (ResearchTabOrderRegistry.class) {
                if (undone) {
                    return;
                }
                if (!activeOrder.equals(appliedOrder)) {
                    throw new IllegalStateException(
                        "Cannot restore Thaumcraft research tab order because it changed unexpectedly");
                }

                activeOrder = previousOrder;
                applyOrder(previousConcreteOrder);
                undone = true;
            }
        }
    }
}
