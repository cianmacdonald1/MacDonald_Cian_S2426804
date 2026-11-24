package org.me.gcu.macdonald_cian_s2426804.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import org.me.gcu.macdonald_cian_s2426804.R;
import org.me.gcu.macdonald_cian_s2426804.data.TrafficSnapshot;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Search for every incident and roadworks on a road. Type a road number (exact match,
 * so "M8" doesn't return the M80) or a place / street name. Results can be put on the map.
 */
public class SearchFragment extends Fragment {

    private TrafficViewModel viewModel;
    private SituationAdapter adapter;
    private MaterialAutoCompleteTextView input;
    private TextView resultCount, empty;
    private MaterialButton showOnMap;
    private ChipGroup quickRoads;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        input = view.findViewById(R.id.search_input);
        resultCount = view.findViewById(R.id.result_count);
        empty = view.findViewById(R.id.empty);
        showOnMap = view.findViewById(R.id.show_on_map);
        quickRoads = view.findViewById(R.id.quick_roads);

        RecyclerView results = view.findViewById(R.id.results);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new SituationAdapter(viewModel::select);
        results.setAdapter(adapter);

        // restore the query after rotation without re-triggering the watcher logic twice
        String query = viewModel.getRoadQuery().getValue();
        if (query != null && !query.equals(input.getText().toString())) input.setText(query, false);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { viewModel.setRoadQuery(s.toString()); }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
        input.setOnItemClickListener((parent, v, position, id) -> hideKeyboard());

        setUpScopeChips(view);

        showOnMap.setOnClickListener(v -> {
            List<Situation> list = viewModel.getRoadResults().getValue();
            if (list != null && !list.isEmpty()) {
                viewModel.showOnMap(list, "Results for \"" + viewModel.getRoadQuery().getValue() + "\"", false);
            }
        });

        viewModel.getRoadResults().observe(getViewLifecycleOwner(), this::showResults);
        viewModel.getSnapshot().observe(getViewLifecycleOwner(), this::updateSuggestions);
        viewModel.getSelected().observe(getViewLifecycleOwner(), adapter::setSelected);
    }

    private void setUpScopeChips(View view) {
        ChipGroup scope = view.findViewById(R.id.scope_group);
        TrafficViewModel.SearchScope current = viewModel.getSearchScope().getValue();
        if (current == TrafficViewModel.SearchScope.INCIDENTS) scope.check(R.id.chip_scope_incidents);
        else if (current == TrafficViewModel.SearchScope.ROADWORKS) scope.check(R.id.chip_scope_roadworks);
        else scope.check(R.id.chip_scope_all);

        scope.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_scope_incidents) viewModel.setSearchScope(TrafficViewModel.SearchScope.INCIDENTS);
            else if (id == R.id.chip_scope_roadworks) viewModel.setSearchScope(TrafficViewModel.SearchScope.ROADWORKS);
            else viewModel.setSearchScope(TrafficViewModel.SearchScope.ALL);
        });
    }

    private void showResults(List<Situation> results) {
        adapter.submitList(results);
        String q = viewModel.getRoadQuery().getValue();
        int n = results == null ? 0 : results.size();
        boolean hasQuery = q != null && !q.isEmpty();

        showOnMap.setEnabled(n > 0);
        resultCount.setText(hasQuery ? getString(R.string.search_count, n, q) : "");
        empty.setVisibility(n == 0 ? View.VISIBLE : View.GONE);
        empty.setText(hasQuery ? getString(R.string.search_none, q) : getString(R.string.search_prompt));
    }

    /** Autocomplete with every road in the data, and quick chips for the busiest roads. */
    private void updateSuggestions(TrafficSnapshot snapshot) {
        if (snapshot == null) return;
        List<String> roads = snapshot.getKnownRoads();
        input.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, roads));

        // count reports per road and show the top ones as one-tap chips
        Map<String, Integer> counts = new HashMap<>();
        for (Situation s : snapshot.getAll()) {
            String road = s.getPrimaryRoad();
            if (road != null) counts.merge(road, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort((a, b) -> b.getValue() - a.getValue());

        quickRoads.removeAllViews();
        for (int i = 0; i < Math.min(12, sorted.size()); i++) {
            String road = sorted.get(i).getKey();
            Chip chip = new Chip(requireContext());
            chip.setText(road + " (" + sorted.get(i).getValue() + ")");
            chip.setOnClickListener(v -> {
                input.setText(road, false);
                input.setSelection(road.length());
                input.dismissDropDown();
                hideKeyboard();
            });
            quickRoads.addView(chip);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        input.clearFocus();
    }
}
