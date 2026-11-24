package org.me.gcu.macdonald_cian_s2426804.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import org.me.gcu.macdonald_cian_s2426804.R;
import org.me.gcu.macdonald_cian_s2426804.model.DelayLevel;
import org.me.gcu.macdonald_cian_s2426804.model.DurationLevel;
import org.me.gcu.macdonald_cian_s2426804.model.SituationCategory;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Maps model values to how they look: an icon and colour per situation category,
 * and the delay / duration colour codes. Keeps Android resources out of the model.
 *
 * Icons: Font Awesome Free (Fonticons, Inc.), CC BY 4.0 - https://fontawesome.com/license/free
 */
public final class SituationStyle {

    private static final ZoneId UK = ZoneId.of("Europe/London");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.UK);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.UK);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.UK);

    private SituationStyle() { }

    @DrawableRes
    public static int iconFor(SituationCategory category) {
        switch (category) {
            case ROAD_CLOSED: return R.drawable.ic_block;
            case LANE_RESTRICTION: return R.drawable.ic_merge;
            case QUEUING_TRAFFIC: return R.drawable.ic_traffic_jam;
            case BROKEN_DOWN_VEHICLE: return R.drawable.ic_car_repair;
            case ACCIDENT: return R.drawable.ic_car_crash;
            case OBSTRUCTION: return R.drawable.ic_warning;
            case WEATHER: return R.drawable.ic_cloud;
            case ROADWORKS: return R.drawable.ic_construction;
            case ROADWORKS_CLOSURE: return R.drawable.ic_do_not_disturb_on;
            case ROADWORKS_LANE_CLOSURE: return R.drawable.ic_remove_road;
            case ROADWORKS_TRAFFIC_LIGHTS: return R.drawable.ic_traffic;
            default: return R.drawable.ic_info;
        }
    }

    @ColorRes
    public static int colorResFor(SituationCategory category) {
        switch (category) {
            case ROAD_CLOSED: return R.color.cat_road_closed;
            case LANE_RESTRICTION: return R.color.cat_lane_restriction;
            case QUEUING_TRAFFIC: return R.color.cat_queuing;
            case BROKEN_DOWN_VEHICLE: return R.color.cat_breakdown;
            case ACCIDENT: return R.color.cat_accident;
            case OBSTRUCTION: return R.color.cat_obstruction;
            case WEATHER: return R.color.cat_weather;
            case ROADWORKS: return R.color.cat_roadworks;
            case ROADWORKS_CLOSURE: return R.color.cat_roadworks_closure;
            case ROADWORKS_LANE_CLOSURE: return R.color.cat_roadworks_lane;
            case ROADWORKS_TRAFFIC_LIGHTS: return R.color.cat_roadworks_lights;
            default: return R.color.cat_other;
        }
    }

    @ColorInt
    public static int colorFor(Context c, SituationCategory category) {
        return ContextCompat.getColor(c, colorResFor(category));
    }

    @ColorInt
    public static int delayColor(Context c, DelayLevel level) {
        switch (level) {
            case SHORT: return ContextCompat.getColor(c, R.color.delay_short);
            case LONG: return ContextCompat.getColor(c, R.color.delay_long);
            case VERY_LONG: return ContextCompat.getColor(c, R.color.delay_very_long);
            default: return ContextCompat.getColor(c, R.color.delay_none);
        }
    }

    @ColorInt
    public static int durationColor(Context c, DurationLevel level) {
        switch (level) {
            case SHORT: return ContextCompat.getColor(c, R.color.duration_short);
            case MEDIUM: return ContextCompat.getColor(c, R.color.duration_medium);
            case LONG: return ContextCompat.getColor(c, R.color.duration_long);
            default: return ContextCompat.getColor(c, R.color.duration_unknown);
        }
    }

    /** White icon on a circle in the category colour. */
    public static void applyIcon(ImageView view, SituationCategory category) {
        Context c = view.getContext();
        view.setImageResource(iconFor(category));
        view.setImageTintList(ColorStateList.valueOf(0xFFFFFFFF));
        view.setBackgroundTintList(ColorStateList.valueOf(colorFor(c, category)));
        view.setContentDescription(category.getLabel());
    }

    // ---- date formatting (always shown in UK time) ----

    public static String formatFull(OffsetDateTime t) {
        return t == null ? "–" : t.atZoneSameInstant(UK).format(FULL);
    }

    public static String formatShort(OffsetDateTime t) {
        return t == null ? "–" : t.atZoneSameInstant(UK).format(SHORT);
    }

    public static String formatMillis(long millis, boolean timeOnly) {
        if (millis <= 0) return "–";
        java.time.ZonedDateTime z = java.time.Instant.ofEpochMilli(millis).atZone(UK);
        boolean today = z.toLocalDate().equals(java.time.LocalDate.now(UK));
        return (timeOnly || today) ? z.format(TIME) : z.format(SHORT);
    }
}
