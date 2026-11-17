package com.parcels.inbox;

import com.parcels.domain.Category;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolves category names written by hand. Besides the full name (case-insensitive), a category can be
 * referenced by its name without the parenthesised part, or by either side of a "/":
 * "Utilities (Electricity, Water, Gas)" matches "utilities"; "Dining/Restaurants" matches "Dining" and "Restaurants".
 * An alias that would match more than one category is not used.
 */
class CategoryLookup {

    private final Map<String, Category> byFullName = new HashMap<>();
    private final Map<String, Category> byAlias = new HashMap<>();
    private final Set<String> ambiguousAliases = new HashSet<>();

    CategoryLookup(List<Category> categories) {
        categories.forEach(this::add);
    }

    void add(Category category) {
        byFullName.put(key(category.getName()), category);
        for (String alias : aliases(category.getName())) {
            if (ambiguousAliases.contains(alias)) {
                continue;
            }
            Category existing = byAlias.putIfAbsent(alias, category);
            if (existing != null && !existing.equals(category)) {
                byAlias.remove(alias);
                ambiguousAliases.add(alias);
            }
        }
    }

    Optional<Category> find(String name) {
        String key = key(name);
        Category exact = byFullName.get(key);
        return exact != null ? Optional.of(exact) : Optional.ofNullable(byAlias.get(key));
    }

    List<String> knownNames() {
        return byFullName.values().stream().map(Category::getName).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static Set<String> aliases(String name) {
        Set<String> aliases = new HashSet<>();
        String base = name.replaceFirst("\\s*\\(.*\\)\\s*$", "");
        aliases.add(key(base));
        if (base.contains("/")) {
            for (String part : base.split("/")) {
                if (!part.isBlank()) {
                    aliases.add(key(part));
                }
            }
        }
        aliases.remove(key(name));
        return aliases;
    }

    private static String key(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
