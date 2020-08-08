package us.red.dmsm;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import org.apache.commons.lang3.ObjectUtils;
import org.dynmap.markers.MarkerAPI;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.asset.Asset;
import org.spongepowered.api.plugin.PluginContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Creates user-defined SignMarkerSets from config files
 */
public class ConfigManager {

    public static final String DEFAULT_CONFIG_FILE_NAME = "default.conf";
    private ArrayList<SignMarkerSet> signMarkerSets = new ArrayList<>();

    /**
     * Creates a new config loader
     * Will use default config locations
     * @param configPath A Path to a config file
     * @param markerAPI A reference to the dynmap MarkerAPI
     * @param logger A Logger for logging purposes
     */
    public ConfigManager(Path configPath, MarkerAPI markerAPI, Logger logger) {
        // create a default config if none exists
        if (Files.notExists(configPath)) {
            PluginContainer pluginContainer = Sponge.getPluginManager().getPlugin("dmsm").get();
            Optional<Asset> optAsset = pluginContainer.getAsset(DEFAULT_CONFIG_FILE_NAME);
            if (optAsset.isPresent()) {
                Asset defaultConfigFile = optAsset.get();
                try {
                    defaultConfigFile.copyToFile(configPath);
                    logger.info("Created a default config file since none was found");
                } catch (IOException e) {
                    logger.error("Could not find a config file and could not create a default");
                    logger.error(e.getMessage());
                }
            } else {
                logger.error("Could not find a default config file in its jar. This is bad");
                return;
            }
        }

        // start loading the config
        HoconConfigurationLoader configLoader = HoconConfigurationLoader.builder()
                .setPath(configPath)
                .build();
        CommentedConfigurationNode rootNode;
        try {
            rootNode = configLoader.load();
            logger.info(rootNode.getChildrenMap().size() + " SignMarkerSets found");
            for (ConfigurationNode node : rootNode.getChildrenMap().values()) {
                // get all values needed for a SignMarkerSet
                String keyword = node.getNode("keyword").getString();
                String id = node.getNode("id").getString();
                String label = node.getNode("label").getString();
                String icon = node.getNode("icon").getString();
                if (ObjectUtils.allNotNull(keyword, id, label, icon)) {
                    signMarkerSets.add(new SignMarkerSet(
                            markerAPI,
                            keyword,
                            id,
                            label,
                            icon
                    ));
                    logger.info("Loaded a SignMarkerSet with id " + id);
                }
                else {
                    logger.error("Error loading SignMarkerSet. Check config syntax");
                }
            }
        }
        catch (IOException e) {
            logger.error("Could not load data from its config file");
        }

        logger.info("Finished loading config");
    }

    /**
     * Gets all user-defined SignMarkerSets
     * @return A Set of all user-defined SignMarkerSets
     */
    public List<SignMarkerSet> getSignMarkerSets() {
        return signMarkerSets;
    }
}
