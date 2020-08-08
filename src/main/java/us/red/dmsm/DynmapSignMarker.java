package us.red.dmsm;

import com.flowpowered.math.vector.Vector3d;
import com.flowpowered.math.vector.Vector3i;
import com.google.inject.Inject;
import org.slf4j.Logger;
import java.util.Optional;

import org.dynmap.DynmapCommonAPI;
import org.dynmap.DynmapCommonAPIListener;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.tileentity.Sign;
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
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@Plugin(
        id = "dmsm",
        name = "DynmapSignMarker",
        description = "Make dynmap markers with signs.",
        url = "https://westeroscraft.com/",
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

    private MarkerAPI markerAPI;

    // TODO: Load this from config
    public static final String OPEN_PLOT_MARKER_SET_ID = "open_plots";
    public static final String OPEN_PLOT_MARKER_SET_LABEL = "Open Plots";
    public static final String OPEN_PLOT_MARKER_ICON_ID = "greenflag";
    private MarkerSet openPlotMarkerSet;

    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        logger.info("DynmapSignMarker is waiting for Dynmap API activation");
        // register ourselves with the dynmap bureau of registered mods
        DynmapCommonAPIListener.register(new DynmapCommonAPIListener() {
            @Override
            public void apiEnabled(DynmapCommonAPI dynmapCommonAPI) {
                // grab the fun and cuddly markerAPI
                markerAPI = dynmapCommonAPI.getMarkerAPI();
                // get or create our fun marker set
                openPlotMarkerSet = markerAPI.getMarkerSet(OPEN_PLOT_MARKER_SET_ID);
                if (openPlotMarkerSet == null) {
                    // if it doesn't exist, we make it
                    openPlotMarkerSet = markerAPI.createMarkerSet(
                            OPEN_PLOT_MARKER_SET_ID,
                            OPEN_PLOT_MARKER_SET_LABEL,
                            null,
                            false // not persistent - we will do that ourselves
                    );
                }
                // if we now have a valid MarkerSet, we can continue. Otherwise fail
                if (openPlotMarkerSet != null) {
                    activate();
                }
                else {
                    logger.error("DynmapSignMarker could not load a MarkerSet!");
                    logger.error("This is very bad!");
                }
            }
        });
    }

    // cool stuff activated
    private void activate() {
        logger.info("DynmapSignMarker has been successfully activated");
        // do sync stuff
    }

    /*
    Create new signs
     */
    @Listener
    public void onChangeSignEvent(ChangeSignEvent event) {
        if (openPlotMarkerSet != null) {
            Optional<Player> optPlayer = event.getCause().first(Player.class);
            // we only do anything if a player caused the event
            if (optPlayer.isPresent()) {
                Player player = optPlayer.get();
                Sign sign = event.getTargetTile();
                SignData signData = event.getText();

                // check if the sign is formatted to request a plot
                // TODO: Make this support different kinds of sign markers
                ListValue<Text> signText = signData.getListValue();
                // continue if the first line is [plot]
                if (signText.get(0).toPlain().equals("[plot]")) {
                    // ensure the player has perms
                    if (!player.hasPermission("dmsm.createmarker.plot")) {
                        player.sendMessage(Text.builder("You do not have permission to create plot markers")
                        .color(TextColors.RED).build());
                        logger.info("Unauthorized player " + player.getName() + " attempted to create a marker");
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

                    MarkerIcon icon = markerAPI.getMarkerIcon(OPEN_PLOT_MARKER_ICON_ID);

                    // create the marker
                    Marker createdMarker = openPlotMarkerSet.createMarker(
                            null,
                            markerDesc,
                            world.getName(),
                            signLocation.getX(),
                            signLocation.getY(),
                            signLocation.getZ(),
                            icon,
                            false
                    );

                    if (createdMarker != null) {
                        player.sendMessage(Text.builder("Plot marker created").color(TextColors.GREEN).build());
                        String markerPosAsString = signLocation.getPosition().toString();
                        logger.info(player.getName() + " created a plot marker at " + markerPosAsString);
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
                        player.playSound(SoundTypes.BLOCK_NOTE_GUITAR, effectPos,1.3);
                        ParticleEffect effect = ParticleEffect.builder()
                                .type(ParticleTypes.FIREWORKS)
                                .quantity(50)
                                .build();
                        player.spawnParticles(effect, effectPos);
                    }
                    else {
                        player.sendMessage(Text.builder("Could not create a plot marker")
                        .color(TextColors.RED).build());
                        logger.error(player.getName() + " experienced an error creating a marker");
                    }
                }
            }
        }
    }

    /*
    Destroy signs
     */
    @Listener
    public void onBreakBlockEvent(ChangeBlockEvent.Break event) {
        if (openPlotMarkerSet != null) {
            // if the event is not player caused, we do nothing
            Optional<Player> optPlayer = event.getCause().first(Player.class);
            if (!optPlayer.isPresent()) {
                return;
            }
            Player player = optPlayer.get();
            // check if this block is related to a marker
            for (Transaction<BlockSnapshot> transaction : event.getTransactions()) {
                Vector3i blockPos = transaction.getFinal().getPosition();
                for (Marker marker : openPlotMarkerSet.getMarkers()) {
                    Vector3i markerPos = new Vector3i(
                            marker.getX(),
                            marker.getY(),
                            marker.getZ()
                    );
                    // if we find a marker
                    if (blockPos.equals(markerPos)) {
                        if (player.hasPermission("dmsm.removemarker.plot")) {
                            marker.deleteMarker();
                            player.sendMessage(Text.builder("Successfully removed marker")
                                    .color(TextColors.GREEN)
                                    .build()
                            );
                            logger.info(
                                    "Player "
                                    + player.getName()
                                    + " removed a marker at "
                                    + markerPos.toString()
                            );
                            // play a fun sound
                            player.playSound(SoundTypes.BLOCK_NOTE_BASS, markerPos.toDouble(),1.3);
                        }
                        else {
                            player.sendMessage(Text.builder("You do not have permission to remove plot markers")
                                    .color(TextColors.RED)
                                    .build()
                            );
                            logger.error(
                                    "Unauthorized player "
                                    + player.getName()
                                    + " attempted to remove a marker at "
                                    + markerPos.toString()
                            );
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
