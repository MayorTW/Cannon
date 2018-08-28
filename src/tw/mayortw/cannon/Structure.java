package tw.mayortw.cannon;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.io.File;

public class Structure {

    private static final String FILE_PATH = "canon.yml";

    private File file;
    private YamlConfiguration data;

    public Structure(Plugin plugin) {

        file = new File(plugin.getDataFolder(), FILE_PATH);

        try {
            data = YamlConfiguration.loadConfiguration(file);
        } catch(IllegalArgumentException e) { }

        if(data == null)
            plugin.getLogger().severe("Cannot load cannon data");
        else {
        }
    }

    public void setBlocks(Location pos, Vector dir) {
    }

    public void clearBlocks(Location pos) {
    }
}
