package us.red.dmsm;

import org.dynmap.markers.Marker;
import org.slf4j.Logger;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.world.World;

import java.util.List;
import java.util.Optional;

/**
 * Cleans up sign markers that don't have associated signs
 */
public class SignMarkerCleaner implements Runnable {
    private final List<SignMarkerSet> signMarkerSets;
    private final Logger logger;
    public SignMarkerCleaner(List<SignMarkerSet> signMarkerSets, Logger logger) {
        this.signMarkerSets = signMarkerSets;
        this.logger = logger;
    }

    @Override
    public void run() {
        logger.info("Cleanup task running...");
        int count = 0;

        // begin cleanup
        for (SignMarkerSet signMarkerSet : signMarkerSets) {
            for (Marker marker : signMarkerSet.getMarkerSet().getMarkers()) {
                World markerWorld = Sponge.getServer().getWorld(marker.getWorld()).get();
                Optional<TileEntity> optionalTileEntity = markerWorld.getTileEntity(
                        (int) marker.getX(),
                        (int) marker.getY(),
                        (int) marker.getZ()
                );
                if (optionalTileEntity.isPresent()) {
                    TileEntity tileEntity = optionalTileEntity.get();
                    if (tileEntity instanceof Sign) {
                        Sign sign = (Sign) tileEntity;
                        SignData signData = sign.getSignData();
                        // ensure we have the correct keyword
                        String keyword = signMarkerSet.getKeyword();
                        if (!signData.lines().get(0).toPlain().equals(keyword)) {
                            // wrong keyword! delete
                            marker.deleteMarker();
                            count++;
                        }
                    }
                    else {
                        // not a sign! delete!
                        marker.deleteMarker();
                        count++;
                    }
                }
                else {
                    // no tile entity! delete!
                    marker.deleteMarker();
                    count++;
                }
            }
        }

        logger.info("Cleanup removed " + count + " dangling sign markers");
    }
}
