import java.util.*;

public class SearchEngine {
    HashMap<String, ArrayList<Vendor>> registry = new HashMap<>();

    void addToSearch(Vendor v) {
        for (String item : v.menu.keySet()) {
            registry.computeIfAbsent(item.toLowerCase(), k -> new ArrayList<>()).add(v);
        }
        // also index by vendor name
        registry.computeIfAbsent(v.vendor_name.toLowerCase(), k -> new ArrayList<>()).add(v);
    }

    ArrayList<Vendor> search(String keyword) {
        return registry.getOrDefault(keyword.toLowerCase(), new ArrayList<>());
    }

    ArrayList<Vendor> getAllVendors() {
        Set<Vendor> unique = new LinkedHashSet<>();
        for (ArrayList<Vendor> list : registry.values()) {
            unique.addAll(list);
        }
        return new ArrayList<>(unique);
    }
}
