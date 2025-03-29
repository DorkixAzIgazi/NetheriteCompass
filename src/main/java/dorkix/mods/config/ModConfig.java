package dorkix.mods.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/netherite_compass.json");

    public int chunkRadius = 1;

    public static ModConfig load() {
        if (!CONFIG_FILE.exists()) {
            ModConfig defaultConfig = new ModConfig();
            defaultConfig.save();
            return defaultConfig;
        }

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ModConfig config = GSON.fromJson(reader, ModConfig.class);
            config.chunkRadius = Math.clamp(config.chunkRadius, 1, 16);
            return config;
        } catch (IOException e) {
            e.printStackTrace();
            return new ModConfig();
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
