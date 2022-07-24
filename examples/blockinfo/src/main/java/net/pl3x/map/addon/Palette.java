package net.pl3x.map.addon;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Palette<T> {
    private final Map<T, Entry> map = new LinkedHashMap<>();

    public void add(T type, String name) {
        int index = this.map.size();
        Entry hmm = new Entry(index, name);
        this.map.put(type, hmm);
    }

    public Entry get(T type) {
        return this.map.get(type);
    }

    public Map<Integer, String> getMap() {
        Map<Integer, String> result = new HashMap<>();
        this.map.values().forEach(v -> result.put(v.getIndex(), v.getName()));
        return result;
    }

    public static class Entry {
        private final int index;
        private final String name;

        public Entry(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public int getIndex() {
            return index;
        }

        public String getName() {
            return name;
        }
    }
}