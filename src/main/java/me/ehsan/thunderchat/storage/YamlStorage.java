package me.ehsan.thunderchat.storage;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.StringReader;

/** Small adapter used only to serialize legacy YAML-shaped state into MySQL records. */
public final class YamlStorage {
    private YamlStorage() {}
    public static YamlConfiguration parse(String value) { return YamlConfiguration.loadConfiguration(new StringReader(value == null ? "" : value)); }
    public static String serialize(YamlConfiguration yaml) { return yaml.saveToString(); }
}
