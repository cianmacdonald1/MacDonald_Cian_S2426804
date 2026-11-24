package org.me.gcu.macdonald_cian_s2426804.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.macdonald_cian_s2426804.R;
import org.me.gcu.macdonald_cian_s2426804.model.DelayLevel;
import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.GeoUtil;
import org.me.gcu.macdonald_cian_s2426804.model.PointLocation;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.me.gcu.macdonald_cian_s2426804.model.Validity;

import java.util.Objects;

/**
 * RecyclerView adapter for situation cards, shared by the incident list, roadworks list,
 * road search results and nearby results. Each card shows the category icon, the type,
 * the generalPublicComment text, where it is, and the delay / duration colour codes.
 */
public class SituationAdapter extends ListAdapter<Situation, SituationAdapter.Holder> {

    public interface OnSituationClick {
        void onClick(Situation situation);
    }

    /** Supplies an optional distance to show on each card (used by the nearby search). */
    public interface DistanceProvider {
        @Nullable Double distanceKm(Situation situation);
    }

    private final OnSituationClick clickListener;
    private DistanceProvider distanceProvider;
    private String selectedRecordId;

    public SituationAdapter(OnSituationClick clickListener) {
        super(DIFF);
        this.clickListener = clickListener;
    }

    public void setDistanceProvider(DistanceProvider provider) {
        this.distanceProvider = provider;
    }

    /** Highlights the selected card (useful in the landscape two-pane layout). */
    public void setSelected(@Nullable Situation s) {
        String id = s == null ? null : s.getRecordId();
        if (!Objects.equals(id, selectedRecordId)) {
            selectedRecordId = id;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_situation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        Situation s = getItem(position);
        Context c = h.itemView.getContext();

        SituationStyle.applyIcon(h.icon, s.getCategory());
        h.typeLabel.setText(s.getTypeLabel());
        h.description.setText(s.getDescription() != null ? s.getDescription() : "(no description)");
        h.location.setText(locationLine(s));

        // delay colour code
        DelayLevel delay = s.getDelayLevel();
        int delayColour = SituationStyle.delayColor(c, delay);
        h.stripe.setBackgroundColor(delayColour);
        h.delayPill.setText(delay.getLabel());
        h.delayPill.setBackgroundTintList(ColorStateList.valueOf(delayColour));

        // duration colour code (roadworks have an end date, incidents don't)
        if (s.getFeedType() == FeedType.ROADWORKS && s.getValidity().getDuration() != null) {
            h.durationPill.setText("Lasts " + Validity.formatDuration(s.getValidity().getDuration()));
        } else {
            h.durationPill.setText("Since " + SituationStyle.formatShort(s.getValidity().getOverallStart()));
        }
        h.durationPill.setBackgroundTintList(ColorStateList.valueOf(
                SituationStyle.durationColor(c, s.getValidity().getDurationLevel())));

        Double km = distanceProvider != null ? distanceProvider.distanceKm(s) : null;
        if (km != null) {
            h.distance.setVisibility(View.VISIBLE);
            h.distance.setText(GeoUtil.formatDistance(km));
        } else {
            h.distance.setVisibility(View.GONE);
        }

        boolean isSelected = s.getRecordId() != null && s.getRecordId().equals(selectedRecordId);
        h.card.setChecked(false);
        h.card.setStrokeWidth(isSelected ? 4 : 0);
        h.card.setStrokeColor(SituationStyle.colorFor(c, s.getCategory()));

        h.itemView.setOnClickListener(v -> clickListener.onClick(s));
        h.itemView.setContentDescription(s.getTypeLabel() + ". " + s.getDescription());
    }

    /** e.g. "M8 · Eastbound · J19 Kingston Bridge (East) → J18 Charing Cross" */
    static String locationLine(Situation s) {
        StringBuilder sb = new StringBuilder();
        String road = s.getPrimaryRoad();
        if (road != null) sb.append(road);
        String dir = s.getLocation().getDirectionLabel();
        if (!dir.isEmpty()) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(dir);
        }
        PointLocation from = s.getLocation().getFrom();
        PointLocation to = s.getLocation().getTo();
        if (from != null && to != null) {
            String a = from.getDisplayName(), b = to.getDisplayName();
            if (sb.length() > 0) sb.append(" · ");
            sb.append(a.equals(b) ? a : a + " → " + b);
        }
        return sb.toString();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final com.google.android.material.card.MaterialCardView card;
        final View stripe;
        final ImageView icon;
        final TextView typeLabel, description, location, delayPill, durationPill, distance;

        Holder(View v) {
            super(v);
            card = (com.google.android.material.card.MaterialCardView) v;
            stripe = v.findViewById(R.id.delay_stripe);
            icon = v.findViewById(R.id.icon);
            typeLabel = v.findViewById(R.id.type_label);
            description = v.findViewById(R.id.description);
            location = v.findViewById(R.id.location_label);
            delayPill = v.findViewById(R.id.delay_pill);
            durationPill = v.findViewById(R.id.duration_pill);
            distance = v.findViewById(R.id.distance_label);
        }
    }

    private static final DiffUtil.ItemCallback<Situation> DIFF = new DiffUtil.ItemCallback<Situation>() {
        @Override
        public boolean areItemsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return Objects.equals(a.getRecordId(), b.getRecordId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Situation a, @NonNull Situation b) {
            return a.getRecordVersion() == b.getRecordVersion()
                    && Objects.equals(a.getDescription(), b.getDescription())
                    && Objects.equals(a.getVersionTime(), b.getVersionTime());
        }
    };
}
