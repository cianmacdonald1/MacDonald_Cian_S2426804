/*  Mobile Platform Development - CW1 2026/27
    Traffic Scotland incidents and roadworks app.

    Architecture: single Activity + Fragments, organised with MVVM.
      Model      - model/ (Situation and its parts) + data/ (download, PullParser, cache, repository)
      ViewModel  - viewmodel/TrafficViewModel (all UI state, survives rotation)
      View       - this Activity and the fragments in ui/
*/

//
// Name                 Cian MacDonald
// Student ID           S2426804
// Programme of Study   BSc (Hons) Software Engineering
//

package org.me.gcu.macdonald_cian_s2426804;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import org.me.gcu.macdonald_cian_s2426804.data.DataSource;
import org.me.gcu.macdonald_cian_s2426804.data.RefreshStatus;
import org.me.gcu.macdonald_cian_s2426804.data.TrafficSnapshot;
import org.me.gcu.macdonald_cian_s2426804.model.FeedType;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.me.gcu.macdonald_cian_s2426804.ui.NearbyFragment;
import org.me.gcu.macdonald_cian_s2426804.ui.SearchFragment;
import org.me.gcu.macdonald_cian_s2426804.ui.SituationDetailFragment;
import org.me.gcu.macdonald_cian_s2426804.ui.SituationListFragment;
import org.me.gcu.macdonald_cian_s2426804.ui.SituationStyle;
import org.me.gcu.macdonald_cian_s2426804.ui.TrafficMapFragment;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.Tab;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.TrafficViewModel;

public class MainActivity extends AppCompatActivity {

    private static final String DETAIL_TAG = "detail";

    private TrafficViewModel viewModel;
    private NavigationBarView navigation;
    private MaterialToolbar toolbar;
    private LinearProgressIndicator progress;
    private TextView banner;
    private View detailContainer;
    private boolean twoPane;
    private OnBackPressedCallback closeDetailsOnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progress = findViewById(R.id.progress);
        banner = findViewById(R.id.banner);
        navigation = findViewById(R.id.navigation);      // BottomNavigationView or NavigationRailView
        detailContainer = findViewById(R.id.detail_container);
        twoPane = getResources().getBoolean(R.bool.two_pane);

        viewModel = new ViewModelProvider(this).get(TrafficViewModel.class);

        // the details fragment always lives in detail_container (both layouts have one)
        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(DETAIL_TAG) == null) {
            fm.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.detail_container, new SituationDetailFragment(), DETAIL_TAG)
                    .commitNow();
        }

        navigation.setOnItemSelectedListener(item -> {
            viewModel.setTab(tabFor(item.getItemId()));
            return true;
        });

        closeDetailsOnBack = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                viewModel.clearSelection();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, closeDetailsOnBack);
        toolbar.setNavigationOnClickListener(v -> viewModel.clearSelection());

        viewModel.getCurrentTab().observe(this, this::showTab);
        viewModel.getSelected().observe(this, this::onSelectionChanged);
        viewModel.getStatus().observe(this, s -> updateStatus());
        viewModel.getSnapshot().observe(this, s -> updateStatus());
        viewModel.getOnline().observe(this, online -> updateStatus());
    }

    // ------------------------------------------------------------------ navigation

    private static Tab tabFor(int itemId) {
        if (itemId == R.id.nav_roadworks) return Tab.ROADWORKS;
        if (itemId == R.id.nav_search) return Tab.SEARCH;
        if (itemId == R.id.nav_nearby) return Tab.NEARBY;
        if (itemId == R.id.nav_map) return Tab.MAP;
        return Tab.INCIDENTS;
    }

    private static int itemFor(Tab tab) {
        switch (tab) {
            case ROADWORKS: return R.id.nav_roadworks;
            case SEARCH: return R.id.nav_search;
            case NEARBY: return R.id.nav_nearby;
            case MAP: return R.id.nav_map;
            default: return R.id.nav_incidents;
        }
    }

    /** Puts the right fragment in the main container (unless it's already there, e.g. after rotation). */
    private void showTab(Tab tab) {
        MenuItem item = navigation.getMenu().findItem(itemFor(tab));
        if (item != null && !item.isChecked()) item.setChecked(true);

        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentById(R.id.list_container);
        if (current != null && tab.name().equals(current.getTag())) return;

        Fragment fragment;
        switch (tab) {
            case ROADWORKS: fragment = SituationListFragment.newInstance(FeedType.ROADWORKS); break;
            case SEARCH: fragment = new SearchFragment(); break;
            case NEARBY: fragment = new NearbyFragment(); break;
            case MAP: fragment = new TrafficMapFragment(); break;
            default: fragment = SituationListFragment.newInstance(FeedType.INCIDENT); break;
        }
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.list_container, fragment, tab.name())
                .commit();
    }

    /** Portrait: details cover the list while something is selected. Landscape: always side by side. */
    private void onSelectionChanged(Situation selected) {
        boolean showing = selected != null;
        if (!twoPane) {
            detailContainer.setVisibility(showing ? View.VISIBLE : View.GONE);
            closeDetailsOnBack.setEnabled(showing);
            if (showing) {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
                toolbar.setNavigationIconTint(ContextCompat.getColor(this, android.R.color.white));
                toolbar.setNavigationContentDescription(R.string.back);
            } else {
                toolbar.setNavigationIcon(null);
            }
        } else {
            closeDetailsOnBack.setEnabled(false);
            toolbar.setNavigationIcon(null);
        }
    }

    // ------------------------------------------------------------------ status

    private void updateStatus() {
        RefreshStatus status = viewModel.getStatus().getValue();
        TrafficSnapshot snapshot = viewModel.getSnapshot().getValue();
        Boolean online = viewModel.getOnline().getValue();

        boolean loading = status != null && status.isLoading();
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);

        // toolbar subtitle
        String subtitle;
        if (loading) {
            subtitle = getString(R.string.status_updating);
        } else if (snapshot == null) {
            subtitle = getString(R.string.loading);
        } else if (snapshot.getOverallSource() == DataSource.LIVE && status != null) {
            subtitle = getString(R.string.status_live,
                    SituationStyle.formatMillis(status.getLastSuccessMillis(), false),
                    SituationStyle.formatMillis(status.getNextRefreshMillis(), true));
        } else if (snapshot.getOverallSource() == DataSource.CACHE) {
            subtitle = getString(R.string.status_cache, SituationStyle.formatMillis(snapshot.getDataTimeMillis(), false));
        } else {
            subtitle = getString(R.string.status_sample);
        }
        toolbar.setSubtitle(subtitle);

        // banner explaining anything unusual
        String bannerText = null;
        int bannerColour = R.color.banner_offline;
        if (Boolean.FALSE.equals(online)) {
            bannerText = getString(R.string.banner_offline);
        } else if (!viewModel.hasApiKeys()) {
            bannerText = getString(R.string.banner_no_keys);
            bannerColour = R.color.banner_sample;
        } else if (status != null && status.getState() == RefreshStatus.State.FAILED) {
            bannerText = getString(R.string.banner_failed, status.getMessage());
        }
        banner.setVisibility(bannerText != null ? View.VISIBLE : View.GONE);
        if (bannerText != null) {
            banner.setText(bannerText);
            banner.setBackgroundColor(ContextCompat.getColor(this, bannerColour));
        }
    }

    // ------------------------------------------------------------------ menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem refresh = menu.findItem(R.id.action_refresh);
        if (refresh != null && refresh.getIcon() != null) {
            refresh.getIcon().mutate().setTint(ContextCompat.getColor(this, android.R.color.white));
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Integer minutes = viewModel.getRefreshMinutes().getValue();
        int id = minutes == null || minutes >= 60 ? R.id.interval_60 : minutes >= 15 ? R.id.interval_15 : R.id.interval_1;
        MenuItem item = menu.findItem(id);
        if (item != null) item.setChecked(true);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            viewModel.refresh();
            return true;
        } else if (id == R.id.interval_1 || id == R.id.interval_15 || id == R.id.interval_60) {
            int minutes = id == R.id.interval_1 ? 1 : id == R.id.interval_15 ? 15 : 60;
            viewModel.setRefreshMinutes(minutes);
            item.setChecked(true);
            com.google.android.material.snackbar.Snackbar.make(findViewById(R.id.list_container),
                    getString(R.string.auto_refresh_set, item.getTitle()),
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_about) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_text)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
