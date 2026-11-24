package org.me.gcu.macdonald_cian_s2426804.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;

import org.me.gcu.macdonald_cian_s2426804.R;
import org.me.gcu.macdonald_cian_s2426804.data.TrafficSnapshot;
import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.TrafficViewModel;

import java.util.List;

/**
 * Selectable list of every current incident or every roadworks (one fragment class,
 * two instances). Tapping a card selects it; the activity shows the details.
 */
public class SituationListFragment extends Fragment {

    private static final String ARG_FEED = "feed";

    private TrafficViewModel viewModel;
    private FeedType feed;
    private SituationAdapter adapter;
    private TextView header, sourceLine, empty;

    public static SituationListFragment newInstance(FeedType feed) {
        SituationListFragment f = new SituationListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FEED, feed.name());
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_situation_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        feed = FeedType.valueOf(requireArguments().getString(ARG_FEED, FeedType.INCIDENT.name()));

        header = view.findViewById(R.id.header);
        sourceLine = view.findViewById(R.id.source_line);
        empty = view.findViewById(R.id.empty);

        RecyclerView list = view.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SituationAdapter(viewModel::select);
        list.setAdapter(adapter);

        setUpRoadworksFilter(view);

        LiveData<List<Situation>> source = feed == FeedType.INCIDENT
                ? viewModel.getIncidents() : viewModel.getRoadworks();
        source.observe(getViewLifecycleOwner(), this::showList);
        viewModel.getSnapshot().observe(getViewLifecycleOwner(), this::showSource);
        viewModel.getSelected().observe(getViewLifecycleOwner(), adapter::setSelected);
    }

    private void setUpRoadworksFilter(View view) {
        ChipGroup filters = view.findViewById(R.id.filter_group);
        if (feed != FeedType.ROADWORKS) {
            filters.setVisibility(View.GONE);
            return;
        }
        filters.setVisibility(View.VISIBLE);
        TrafficViewModel.RoadworksFilter current = viewModel.getRoadworksFilter().getValue();
        if (current == TrafficViewModel.RoadworksFilter.UPCOMING) filters.check(R.id.chip_upcoming);
        else if (current == TrafficViewModel.RoadworksFilter.ALL) filters.check(R.id.chip_all);
        else filters.check(R.id.chip_active);

        filters.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_upcoming) viewModel.setRoadworksFilter(TrafficViewModel.RoadworksFilter.UPCOMING);
            else if (id == R.id.chip_all) viewModel.setRoadworksFilter(TrafficViewModel.RoadworksFilter.ALL);
            else viewModel.setRoadworksFilter(TrafficViewModel.RoadworksFilter.ACTIVE_NOW);
        });
    }

    private void showList(List<Situation> situations) {
        adapter.submitList(situations);
        int n = situations == null ? 0 : situations.size();
        header.setText(getString(feed == FeedType.INCIDENT ? R.string.incidents_header : R.string.roadworks_header, n));

        boolean loading = viewModel.getSnapshot().getValue() == null;
        empty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        empty.setText(loading ? R.string.loading
                : feed == FeedType.INCIDENT ? R.string.empty_incidents : R.string.empty_roadworks);
    }

    private void showSource(TrafficSnapshot snapshot) {
        if (snapshot == null) {
            sourceLine.setText(R.string.loading);
            return;
        }
        String source = (feed == FeedType.INCIDENT ? snapshot.getIncidentsSource() : snapshot.getRoadworksSource()).getLabel();
        String published = SituationStyle.formatShort(feed == FeedType.INCIDENT
                ? snapshot.getIncidentsPublished() : snapshot.getRoadworksPublished());
        sourceLine.setText(source + " · published by Traffic Scotland " + published);
    }
}
