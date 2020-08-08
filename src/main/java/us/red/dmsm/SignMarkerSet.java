package us.red.dmsm;

import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;

/**
 * Represents a type of sign marker that authorized players can create
 */
public class SignMarkerSet {
    // What needs to be on a sign for it to create a marker of this type? e.g. [plot]
    private final String keyword;
    // icon for use on Markers
    private final MarkerIcon icon;
    // underlying MarkerSet that this SignMarkerSet represents
    private MarkerSet markerSet;

    /**
     * Creates a new SignMarkerSet
     *
     * @param keyword         Keyword to look for on signs. e.g. [plot]
     * @param markerSetID     Unique ID of the underlying MarkerSet
     * @param markerSetLabel  Display label of the underlying MarkerSet
     * @param markerSetIconID ID of the icon the underlying MarkerSet will use
     */
    public SignMarkerSet(
            MarkerAPI markerAPI,
            String keyword,
            String markerSetID,
            String markerSetLabel,
            String markerSetIconID
    ) {
        this.keyword = keyword;
        this.icon = markerAPI.getMarkerIcon(markerSetIconID);
        // attempt to grab an existing MarkerSet
        markerSet = markerAPI.getMarkerSet(markerSetID);
        // create a new set if we get null
        if (markerSet == null) {
            markerSet = markerAPI.createMarkerSet(markerSetID, markerSetLabel, null, true);
        }
    }

    /**
     * Get the keyword String of this SignMarkerSet
     *
     * @return The keyword String of this SignMarkerSet
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Get the underlying MarkerSet of this SignMarkerSet
     *
     * @return The underlying MarkerSet of this SignMarkerSet
     */
    public MarkerSet getMarkerSet() {
        return markerSet;
    }

    /**
     * Gets the MarkerIcon associated with this SignMarkerSet
     *
     * @return A MarkerIcon associated with this SignMarkerSet
     */
    public MarkerIcon getIcon() {
        return icon;
    }
}
