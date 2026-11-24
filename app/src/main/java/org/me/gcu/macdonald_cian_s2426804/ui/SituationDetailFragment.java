package org.me.gcu.macdonald_cian_s2426804.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.me.gcu.macdonald_cian_s2426804.R;
import org.me.gcu.macdonald_cian_s2426804.model.CommentDetails;
import org.me.gcu.macdonald_cian_s2426804.model.DelayLevel;
import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.GeoUtil;
import org.me.gcu.macdonald_cian_s2426804.model.Impact;
import org.me.gcu.macdonald_cian_s2426804.model.LinearLocation;
import org.me.gcu.macdonald_cian_s2426804.model.PointLocation;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.me.gcu.macdonald_cian_s2426804.model.TextUtil;
import org.me.gcu.macdonald_cian_s2426804.model.Validity;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.NearbyQuery;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.TrafficViewModel;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Locale;

/**
 * Shows everything the feed gives about the selected situation: the description and its
 * breakdown, impact, validity, both ends of the location and the record metadata.
 * Lives in the right-hand pane in landscape, and over the list in portrait.
 */
public class SituationDetailFragment extends Fragment {

    private TrafficViewModel viewModel;
    private TextView emptyText, typeLabel, recordType, description;
    private NestedScrollView content;
    private ImageView icon;
    private ChipGroup chips;
    private LinearLayout sections;
    private Situation shown;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        emptyText = view.findViewById(R.id.empty_text);
        content = view.findViewById(R.id.content);
        icon = view.findViewById(R.id.icon);
        typeLabel = view.findViewById(R.id.type_label);
        recordType = view.findViewById(R.id.record_type);
        description = view.findViewById(R.id.description);
        chips = view.findViewById(R.id.chips);
        sections = view.findViewById(R.id.sections);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> viewModel.clearSelection());
        view.findViewById(R.id.btn_map).setOnClickListener(v -> {
            if (shown == null) return;
            viewModel.showOnMap(Collections.singletonList(shown), shown.getTypeLabel() + ": "
                    + (shown.getPrimaryRoad() != null ? shown.getPrimaryRoad() : ""), false);
            // in portrait the details cover the map, so close them
            if (!getResources().getBoolean(R.bool.two_pane)) viewModel.clearSelection();
        });

        viewModel.getSelected().observe(getViewLifecycleOwner(), this::show);
    }

    private void show(@Nullable Situation s) {
        boolean sameAsBefore = s != null && shown != null && s.getRecordId() != null
                && s.getRecordId().equals(shown.getRecordId());
        shown = s;
        if (s == null) {
            content.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            return;
        }
        emptyText.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);
        if (!sameAsBefore) content.scrollTo(0, 0);

        SituationStyle.applyIcon(icon, s.getCategory());
        typeLabel.setText(s.getTypeLabel());
        String sub = s.getSubTypeLabel();
        recordType.setText(s.getFeedType().getLabel() + " · " + s.getRecordTypeLabel()
                + (sub.isEmpty() ? "" : " · " + sub));
        description.setText(s.getDescription());

        buildChips(s);
        buildSections(s);
    }

    // ------------------------------------------------------------------ chips

    private void buildChips(Situation s) {
        Context c = requireContext();
        chips.removeAllViews();

        DelayLevel delay = s.getDelayLevel();
        addChip(delay.getLabel(), SituationStyle.delayColor(c, delay));

        Validity v = s.getValidity();
        if (s.getFeedType() == FeedType.ROADWORKS && v.getDuration() != null) {
            addChip(v.getDurationLevel().getLabel(), SituationStyle.durationColor(c, v.getDurationLevel()));
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (v.isUpcomingAt(now)) addChip("Planned – starts " + SituationStyle.formatShort(v.getOverallStart()), 0xFF455A64);
        else if (v.isActiveAt(now)) addChip("In force now", 0xFF0B5394);
        else addChip("Finished", 0xFF757575);

        String lanes = s.getImpact().getLanesSummary();
        if (lanes != null) addChip(lanes, 0xFF5D4037);
    }

    private void addChip(String text, int colour) {
        Chip chip = new Chip(requireContext());
        chip.setText(text);
        chip.setClickable(false);
        chip.setChipBackgroundColor(ColorStateList.valueOf(colour));
        chip.setTextColor(0xFFFFFFFF);
        chip.setChipStrokeWidth(0);
        chips.addView(chip);
    }

    // ------------------------------------------------------------------ sections

    private void buildSections(Situation s) {
        sections.removeAllViews();

        // what the description text says, broken into parts
        CommentDetails d = s.getCommentDetails();
        if (d != null && d.hasAnyDetail()) {
            section(R.string.detail_section_summary, R.drawable.ic_info);
            row("Road", d.getRoad());
            row("Direction", d.getDirection());
            row("Location", d.getPlace());
            row("Between", d.getBetween());
            row("Works", d.getWorksDescription());
            row("Cause", d.getCause() != null ? capitalise(d.getCause()) : null);
            row("Lanes affected", d.getLanesAffected() != null ? String.valueOf(d.getLanesAffected()) : null);
        }

        // impact
        Impact impact = s.getImpact();
        section(R.string.detail_section_impact, R.drawable.ic_traffic_jam);
        row("Delays", s.getDelayLevel().getLabel()
                + (impact.getDelaysType() != null ? " (" + impact.getDelaysType() + ")" : ""));
        row("Lanes restricted", impact.getNumberOfLanesRestricted() != null ? String.valueOf(impact.getNumberOfLanesRestricted()) : null);
        row("Lanes open", impact.getNumberOfOperationalLanes() != null ? String.valueOf(impact.getNumberOfOperationalLanes()) : null);
        if (impact.getDelayTimeSeconds() != null) row("Expected delay", (impact.getDelayTimeSeconds() / 60) + " min");
        if (impact.isEmpty()) row("Details", "No impact information in the feed");

        // when
        Validity v = s.getValidity();
        section(R.string.detail_section_when, R.drawable.ic_schedule);
        row("Starts", SituationStyle.formatFull(v.getOverallStart()));
        row("Ends", v.getOverallEnd() != null ? SituationStyle.formatFull(v.getOverallEnd()) : "No end time given");
        if (v.getDuration() != null) {
            row("Duration", Validity.formatDuration(v.getDuration()) + " – " + v.getDurationLevel().getLabel());
        }
        row("Validity", TextUtil.humanise(v.getValidityStatus()));
        if (s.getFeedType() == FeedType.ROADWORKS && s.getUrgentRoadworks() != null) {
            row("Urgent roadworks", s.getUrgentRoadworks() ? "Yes" : "No");
        }

        // where
        LinearLocation loc = s.getLocation();
        section(R.string.detail_section_where, R.drawable.ic_location_on);
        row("Roads", s.getRoadList().isEmpty() ? null : String.join(", ", s.getRoadList()));
        row("Direction", loc.getDirectionLabel().isEmpty() ? null : loc.getDirectionLabel());
        row("Location type", TextUtil.humanise(loc.getLocationGroupType()) +
                (loc.getLinearType() != null ? " (" + loc.getLinearType() + ")" : ""));
        if (loc.getLengthKm() > 0) row("Length", GeoUtil.formatDistance(loc.getLengthKm()));
        NearbyQuery q = viewModel.getNearbyQuery().getValue();
        if (q != null && s.hasCoordinates()) {
            row("Distance", GeoUtil.formatDistance(s.distanceKmFrom(q.latitude, q.longitude)) + " from " + q.placeLabel);
        }
        point(R.string.detail_section_from, loc.getFrom());
        point(R.string.detail_section_to, loc.getTo());
        if (loc.getSinglePoint() != null) point(R.string.detail_section_where, loc.getSinglePoint());

        // record
        section(R.string.detail_section_record, R.drawable.ic_format_list_bulleted);
        row("Situation id", s.getSituationId());
        row("Record id", s.getRecordId() + " (version " + s.getRecordVersion() + ")");
        row("Created", SituationStyle.formatFull(s.getCreationTime()));
        row("Last updated", SituationStyle.formatFull(s.getVersionTime()));
        row("Source", s.getSourceName() != null ? s.getSourceName() + " (" + TextUtil.humanise(s.getSourceType()) + ")" : null);
        row("Probability", TextUtil.humanise(s.getProbabilityOfOccurrence()));
        row("Compliance", TextUtil.humanise(s.getComplianceOption()));
        row("Status", TextUtil.humanise(s.getInformationStatus()) + " · " + TextUtil.humanise(s.getConfidentiality()));
    }

    private void point(int titleRes, @Nullable PointLocation p) {
        if (p == null) return;
        TextView title = (TextView) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_detail_section, sections, false);
        title.setText(getString(titleRes) + ": " + p.getDisplayName());
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_LabelLarge);
        sections.addView(title);
        row("Point type", p.getPointTypeLabel());
        row("Junction", p.getJunctionName());
        row("Road numbers", p.getRoadNumbers().isEmpty() ? null : String.join(", ", p.getRoadNumbers()));
        row("Road link", p.getLinkName());
        row("Place", p.getPointName());
        if (p.hasCoordinates()) {
            row("Coordinates", String.format(Locale.UK, "%.5f, %.5f", p.getLatitude(), p.getLongitude()));
        }
    }

    private void section(int titleRes, int iconRes) {
        TextView title = (TextView) LayoutInflater.from(requireContext())
                .inflate(R.layout.item_detail_section, sections, false);
        title.setText(titleRes);
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
        title.setCompoundDrawableTintList(title.getTextColors());
        sections.addView(title);
    }

    private void row(String label, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) return;
        View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_detail_row, sections, false);
        ((TextView) row.findViewById(R.id.row_label)).setText(label);
        ((TextView) row.findViewById(R.id.row_value)).setText(value);
        sections.addView(row);
    }

    private static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
