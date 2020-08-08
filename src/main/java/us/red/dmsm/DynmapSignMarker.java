package us.red.dmsm;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.inject.Inject;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.effect.particle.ParticleEffect;
import org.spongepowered.api.effect.particle.ParticleTypes;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Dependency;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "dmsm",
        name = "DynmapSignMarker",
        description = "Make dynmap markers with signs.",
        url = "https://github.com/yvantitov/dmsm",
        authors = {
                "he_who_is_red"
        },
        dependencies = {
                @Dependency(id = "dynmap")
        }
)
public class DynmapSignMarker {
    @Inject
    private Logger logger;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private Path configPath;

    public static PluginContainer dmsm;

    private MarkerAPI markerAPI;
    private ConfigManager configManager;

    // interval in minutes between each run of the marker cleanup task
    public static final long CLEANUP_INTERVAL = 5;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("Waiting for Dynmap API activation");
        // register ourselves with the dynmap bureau of registered mods
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
                // grab the fun and cuddly markerAPI
                markerAPI = dynmapCommonAPI.getMarkerAPI();
                // load our config
                configManager = new ConfigManager(configPath, markerAPI, logger);
                // get a PluginContainer for this plugin
                dmsm = Sponge.getPluginManager().getPlugin("dmsm").get();
                // activate
                activate();
            }
        });
    }

    // cool stuff activated
    private void activate() {
        logger.info("Running...");

        // build a cleanup task
        Task.Builder taskBuilder = Task.builder();
        taskBuilder.interval(CLEANUP_INTERVAL, TimeUnit.MINUTES);
        taskBuilder.execute(new SignMarkerCleaner(configManager.getSignMarkerSets(), logger));
        taskBuilder.submit(dmsm);
        logger.info("Started cleanup routine");
    }

    /*
    Create new signs
     */
    @Listener
    public void onChangeSignEvent(ChangeSignEvent event) {
        Optional<Player> optPlayer = event.getCause().first(Player.class);
        // we only do anything if a player caused the event
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();
            Sign sign = event.getTargetTile();
            SignData signData = event.getText();

            // get the first line of text on the sign
            ListValue<Text> signText = signData.getListValue();
            String keyword = signText.get(0).toPlain();

            // try to match it to a SignMarkerSet
            MarkerSet markerSet = null;
            MarkerIcon markerIcon = null;
            for (SignMarkerSet s : configManager.getSignMarkerSets()) {
                if (s.getKeyword().equals(keyword)) {
                    markerSet = s.getMarkerSet();
                    markerIcon = s.getIcon();
                }
            }
            if (markerSet == null) {
                // if the keyword doesn't match a valid marker set, we simply ignore it
                return;
            }

            // ensure the player has perms
            if (!player.hasPermission("dmsm." + markerSet.getMarkerSetID())) {
                player.sendMessage(Text.builder("You do not have permission to create sign markers of that kind")
                        .color(TextColors.RED).build());
                logger.info(
                        "Unauthorized player "
                                + player.getName()
                                + " attempted to create a sign marker of type "
                                + markerSet.getMarkerSetID()
                );
                event.setCancelled(true);
                return;
            }

            // turn the rest of the text into a plot description
            String markerDesc = "";
            for (int i = 1; i < signText.size(); i++) {
                markerDesc = markerDesc.concat(signText.get(i).toPlain() + " ");
            }

            Location<World> signLocation = sign.getLocation();
            World world = signLocation.getExtent();

            // create the marker
            Marker createdMarker = markerSet.createMarker(
                    null,
                    markerDesc,
                    world.getName(),
                    signLocation.getX(),
                    signLocation.getY(),
                    signLocation.getZ(),
                    markerIcon,
                    true
            );

            if (createdMarker != null) {
                String lbl = markerSet.getMarkerSetLabel();
                player.sendMessage(Text.builder(lbl)
                        .color(TextColors.AQUA)
                        .append(Text.builder(" sign marker created").color(TextColors.GREEN).build())
                        .build());
                String markerPosAsString = signLocation.getPosition().toString();
                logger.info(player.getName() + " created a " + lbl + " sign marker at " + markerPosAsString);
                // make the sign fancy-colored
                for (int i = 0; i < signText.size(); i++) {
                    Text newText = signText.get(i).toBuilder()
                            .style(TextStyles.BOLD)
                            .color(TextColors.DARK_RED)
                            .build();
                    signData.setElement(i, newText);
                }
                // play some funky fresh effects
                Vector3d effectPos = signLocation.getPosition().add(0.5, 0.5, 0.5);
                player.playSound(SoundTypes.BLOCK_NOTE_GUITAR, effectPos, 1.3);
                ParticleEffect effect = ParticleEffect.builder()
                        .type(ParticleTypes.EXPLOSION)
                        .quantity(50)
                        .build();
                player.spawnParticles(effect, effectPos);
            } else {
                player.sendMessage(Text.builder("Could not create a sign marker")
                        .color(TextColors.RED).build());
                logger.error(player.getName() + " ran into an error creating a sign marker");
            }
        }
    }

    /*
    Destroy signs
     */
    @Listener
    public void onBreakBlockEvent(ChangeBlockEvent.Break event) {
        // check if this block is related to a marker
        for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
            Vector3i blockPos = transaction.getFinal().getPosition();
            for (SignMarkerSet signMarkerSet : configManager.getSignMarkerSets()) {
                MarkerSet markerSet = signMarkerSet.getMarkerSet();
                for (Marker marker : markerSet.getMarkers()) {
                    Vector3i markerPos = new Vector3i(
                            marker.getX(),
                            marker.getY(),
                            marker.getZ()
                    );
                    // if we find a marker
                    if (blockPos.equals(markerPos)) {
                        String lbl = markerSet.getMarkerSetLabel();
                        // check if a player did it
                        Optional<Player> optPlayer = event.getCause().first(Player.class);
                        if (optPlayer.isPresent()) {
                            Player player = optPlayer.get();
                            if (player.hasPermission("dmsm." + markerSet.getMarkerSetID())) {
                                marker.deleteMarker();
                                player.sendMessage(Text.builder("Successfully removed sign marker")
                                        .color(TextColors.GREEN)
                                        .build()
                                );
                                logger.info(
                                        "Player "
                                                + player.getName()
                                                + " removed a "
                                                + lbl
                                                + " sign marker at "
                                                + markerPos.toString()
                                );
                                // play a fun sound
                                player.playSound(SoundTypes.BLOCK_NOTE_BASS, markerPos.toDouble(), 1.3);
                            } else {
                                String msg = "You do not have permission to remove " + lbl + " sign markers";
                                player.sendMessage(Text.builder(msg).color(TextColors.RED).build());
                                logger.info(
                                        "Unauthorized player "
                                                + player.getName()
                                                + " attempted to remove a marker at "
                                                + markerPos.toString()
                                );
                                event.setCancelled(true);
                            }
                        } else {
                            // only players can destroy signs
                            logger.info(lbl + " sign marker at " + markerPos.toString() + " avoided destruction");
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
