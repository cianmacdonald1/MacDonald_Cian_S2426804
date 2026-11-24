package org.me.gcu.macdonald_cian_s2426804.viewmodel;

import org.me.gcu.macdonald_cian_s2426804.model.Situation;

import java.util.Collections;
import java.util.List;

/** What the map should currently draw. */
public class MapContent {

    public enum Mode { ALL, INCIDENTS, ROADWORKS, SELECTION }

    public final Mode mode;
    public final String title;
    public final List<Situation> situations;
    /** Optional search centre + radius (shown when the selection came from "nearby"). */
    public final double[] centre;
    public final double radiusKm;

    public MapContent(Mode mode, String title, List<Situation> situations, double[] centre, double radiusKm) {
        this.mode = mode;
        this.title = title;
        this.situations = Collections.unmodifiableList(situations);
        this.centre = centre;
        this.radiusKm = radiusKm;
    }
}
