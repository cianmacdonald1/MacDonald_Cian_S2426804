package org.me.gcu.macdonald_cian_s2426804.ui;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;

import org.me.gcu.macdonald_cian_s2426804.R;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.NearbyQuery;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.TrafficViewModel;

import java.util.List;

/**
 * Search by distance: every current situation within a chosen radius of either the
 * phone's coarse location or a fixed point (GCU campus). Sorted nearest first.
 */
public class NearbyFragment extends Fragment {

    private TrafficViewModel viewModel;
    private SituationAdapter adapter;
    private TextView radiusLabel, resultCount, empty;
    private ChipGroup sourceGroup;
    private Slider slider;
    private MaterialButton showOnMap;
    private boolean ignoreChipChange;

    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    locate();
                } else {
                    useGcu();
                    snack(R.string.nearby_permission_denied);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nearby, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        radiusLabel = view.findViewById(R.id.radius_label);
        resultCount = view.findViewById(R.id.result_count);
        empty = view.findViewById(R.id.empty);
        sourceGroup = view.findViewById(R.id.source_group);
        slider = view.findViewById(R.id.radius_slider);
        showOnMap = view.findViewById(R.id.show_on_map);

        RecyclerView results = view.findViewById(R.id.results);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SituationAdapter(viewModel::select);
        adapter.setDistanceProvider(viewModel::getNearbyDistance);
        results.setAdapter(adapter);

        NearbyQuery q = viewModel.getNearbyQuery().getValue();
        if (q != null) {
            slider.setValue(Math.max(1, Math.min(100, q.radiusKm)));
            ignoreChipChange = true;
            sourceGroup.check(q.fromDevice ? R.id.chip_device : R.id.chip_gcu);
            ignoreChipChange = false;
        }

        slider.setLabelFormatter(value -> Math.round(value) + " km");
        slider.addOnChangeListener((s, value, fromUser) -> {
            if (fromUser) viewModel.setNearbyRadius(Math.round(value));
        });

        sourceGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ignoreChipChange || ids.isEmpty()) return;
            if (ids.get(0) == R.id.chip_device) requestLocation();
            else useGcu();
        });

        showOnMap.setOnClickListener(v -> {
            List<Situation> list = viewModel.getNearbyResults().getValue();
            NearbyQuery nq = viewModel.getNearbyQuery().getValue();
            if (list != null && !list.isEmpty() && nq != null) {
                viewModel.showOnMap(list, "Within " + nq.radiusKm + " km of " + nq.placeLabel, true);
            }
        });

        viewModel.getNearbyQuery().observe(getViewLifecycleOwner(), nq ->
                radiusLabel.setText(getString(R.string.nearby_radius, nq.radiusKm, nq.placeLabel)));
        viewModel.getNearbyResults().observe(getViewLifecycleOwner(), this::showResults);
        viewModel.getSelected().observe(getViewLifecycleOwner(), adapter::setSelected);
    }

    private void showResults(List<Situation> list) {
        // distances may have changed even if the items didn't, so rebind once the diff is applied
        adapter.submitList(list, () -> adapter.notifyItemRangeChanged(0, adapter.getItemCount()));
        int n = list == null ? 0 : list.size();
        NearbyQuery q = viewModel.getNearbyQuery().getValue();
        resultCount.setText(getString(R.string.nearby_count, n));
        showOnMap.setEnabled(n > 0);
        empty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        empty.setText(getString(R.string.nearby_none, q != null ? q.radiusKm : 0));
    }

    private void requestLocation() {
        if (LocationHelper.hasPermission(requireContext())) {
            locate();
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
    }

    private void locate() {
        radiusLabel.setText(R.string.nearby_locating);
        LocationHelper.getCoarseLocation(requireContext().getApplicationContext(), location -> {
            if (!isAdded()) return;
            if (location == null) {
                useGcu();
                snack(R.string.nearby_no_fix);
            } else {
                viewModel.setNearbyCentre(location.getLatitude(), location.getLongitude(),
                        getString(R.string.nearby_my_location), true);
            }
        });
    }

    private void useGcu() {
        viewModel.setNearbyCentre(NearbyQuery.GCU_LAT, NearbyQuery.GCU_LON, "GCU campus", false);
        ignoreChipChange = true;
        sourceGroup.check(R.id.chip_gcu);
        ignoreChipChange = false;
    }

    private void snack(int message) {
        View v = getView();
        if (v != null) Snackbar.make(v, message, Snackbar.LENGTH_LONG).show();
    }
}
