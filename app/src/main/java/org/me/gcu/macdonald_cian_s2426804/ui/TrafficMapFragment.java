package org.me.gcu.macdonald_cian_s2426804.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.me.gcu.macdonald_cian_s2426804.R;
import org.me.gcu.macdonald_cian_s2426804.model.PointLocation;
import org.me.gcu.macdonald_cian_s2426804.model.Situation;
import org.me.gcu.macdonald_cian_s2426804.model.SituationCategory;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.MapContent;
import org.me.gcu.macdonald_cian_s2426804.viewmodel.TrafficViewModel;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google map of all incidents / all roadworks / a user-chosen selection (road search
 * results, nearby results or a single situation). Each situation gets a marker with its
 * category icon, and a line from its "from" point to its "to" point, coloured by delay.
 * Tapping a marker's label selects the situation and opens its details.
 */
public class TrafficMapFragment extends Fragment implements OnMapReadyCallback {

    private static final LatLng SCOTLAND = new LatLng(56.8, -4.2);

    private TrafficViewModel viewModel;
    private GoogleMap map;
    private TextView mapTitle;
    private ChipGroup modeGroup;
    private Chip selectionChip;
    private final Map<SituationCategory, BitmapDescriptor> iconCache = new EnumMap<>(SituationCategory.class);
    private final Map<String, Marker> markersById = new HashMap<>();
    private boolean ignoreChipChange;
    private MapContent lastDrawn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);
        mapTitle = view.findViewById(R.id.map_title);
        modeGroup = view.findViewById(R.id.map_mode_group);
        selectionChip = view.findViewById(R.id.chip_map_selection);

        if (!hasMapsKey(requireContext())) view.findViewById(R.id.map_no_key).setVisibility(View.VISIBLE);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map_holder);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map_holder, mapFragment).commitNow();
        }
        mapFragment.getMapAsync(this);

        modeGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ignoreChipChange || ids.isEmpty()) return;
            int id = ids.get(0);
            if (id == R.id.chip_map_incidents) viewModel.setMapMode(MapContent.Mode.INCIDENTS);
            else if (id == R.id.chip_map_roadworks) viewModel.setMapMode(MapContent.Mode.ROADWORKS);
            else if (id == R.id.chip_map_selection) viewModel.setMapMode(MapContent.Mode.SELECTION);
            else viewModel.setMapMode(MapContent.Mode.ALL);
        });

        viewModel.getMapMode().observe(getViewLifecycleOwner(), this::syncChips);
    }

    private void syncChips(MapContent.Mode mode) {
        selectionChip.setVisibility(viewModel.hasCustomMapSelection() ? View.VISIBLE : View.GONE);
        ignoreChipChange = true;
        if (mode == MapContent.Mode.INCIDENTS) modeGroup.check(R.id.chip_map_incidents);
        else if (mode == MapContent.Mode.ROADWORKS) modeGroup.check(R.id.chip_map_roadworks);
        else if (mode == MapContent.Mode.SELECTION) modeGroup.check(R.id.chip_map_selection);
        else modeGroup.check(R.id.chip_map_all);
        ignoreChipChange = false;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));

        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation) viewModel.select((Situation) tag);
        });
        // in landscape the details pane is visible, so select straight away on marker tap
        map.setOnMarkerClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation && getResources().getBoolean(R.bool.two_pane)) {
                viewModel.select((Situation) tag);
            }
            return false;   // still show the info window and centre the marker
        });

        viewModel.getMapContent().observe(getViewLifecycleOwner(), this::draw);
        viewModel.getSelected().observe(getViewLifecycleOwner(), s -> {
            if (s == null) return;
            Marker m = markersById.get(s.getRecordId());
            if (m != null) m.showInfoWindow();
        });
    }

    private void draw(MapContent content) {
        if (map == null || content == null) return;
        boolean sameView = lastDrawn != null && lastDrawn.mode == content.mode
                && lastDrawn.situations.size() == content.situations.size()
                && String.valueOf(lastDrawn.title).equals(content.title);
        lastDrawn = content;

        map.clear();
        markersById.clear();
        mapTitle.setText(getString(R.string.map_count, content.title, content.situations.size())
                + " · " + getString(R.string.map_tap_hint));

        Context c = requireContext();
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int pointCount = 0;

        for (Situation s : content.situations) {
            List<PointLocation> points = s.getLocation().getPoints();
            if (points.isEmpty()) continue;

            // line between the "from" and "to" points, coloured by delay
            if (points.size() >= 2) {
                PolylineOptions line = new PolylineOptions()
                        .width(10f)
                        .color(SituationStyle.delayColor(c, s.getDelayLevel()));
                for (PointLocation p : points) line.add(new LatLng(p.getLatitude(), p.getLongitude()));
                map.addPolyline(line);
            }

            double[] centre = s.getLocation().getCentre();
            LatLng pos = new LatLng(centre[0], centre[1]);
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(s.getTypeLabel() + (s.getPrimaryRoad() != null ? " – " + s.getPrimaryRoad() : ""))
                    .snippet(s.getDescription())
                    .icon(iconFor(s.getCategory()))
                    .anchor(0.5f, 0.5f));
            if (marker != null) {
                marker.setTag(s);
                markersById.put(s.getRecordId(), marker);
            }
            for (PointLocation p : points) {
                bounds.include(new LatLng(p.getLatitude(), p.getLongitude()));
                pointCount++;
            }
        }

        // nearby search: show the centre and the radius
        if (content.centre != null) {
            LatLng centre = new LatLng(content.centre[0], content.centre[1]);
            map.addMarker(new MarkerOptions().position(centre).title(getString(R.string.map_centre))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            map.addCircle(new CircleOptions().center(centre).radius(content.radiusKm * 1000)
                    .strokeColor(0xAA0B5394).strokeWidth(3f).fillColor(0x220B5394));
            bounds.include(centre);
            pointCount++;
        }

        if (sameView) return;   // a background refresh - don't move the camera on the user
        moveCamera(bounds, pointCount, content);
    }

    private void moveCamera(LatLngBounds.Builder bounds, int pointCount, MapContent content) {
        View v = getView();
        if (v == null) return;
        if (pointCount == 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
            return;
        }
        if (content.situations.size() == 1 && content.centre == null) {
            double[] c = content.situations.get(0).getLocation().getCentre();
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(c[0], c[1]), 13f));
            Marker m = markersById.get(content.situations.get(0).getRecordId());
            if (m != null) m.showInfoWindow();
            return;
        }
        LatLngBounds b = bounds.build();
        View mapView = v.findViewById(R.id.map_holder);
        mapView.post(() -> {
            if (map == null || mapView.getWidth() == 0) return;
            int padding = (int) (48 * getResources().getDisplayMetrics().density);
            try {
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(b, mapView.getWidth(), mapView.getHeight(), padding));
            } catch (IllegalStateException e) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(b.getCenter(), 9f));
            }
        });
    }

    /** Category icon drawn white on a coloured circle, cached per category. */
    private BitmapDescriptor iconFor(SituationCategory category) {
        BitmapDescriptor cached = iconCache.get(category);
        if (cached != null) return cached;

        Context c = requireContext();
        float density = getResources().getDisplayMetrics().density;
        int size = (int) (34 * density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);                 // white border
        paint.setColor(SituationStyle.colorFor(c, category));
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 2 * density, paint);   // coloured fill

        Drawable icon = ContextCompat.getDrawable(c, SituationStyle.iconFor(category));
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(0xFFFFFFFF);
            int inset = (int) (7 * density);
            icon.setBounds(inset, inset, size - inset, size - inset);
            icon.draw(canvas);
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        iconCache.put(category, descriptor);
        return descriptor;
    }

    private static boolean hasMapsKey(Context c) {
        try {
            ApplicationInfo info = c.getPackageManager().getApplicationInfo(c.getPackageName(), PackageManager.GET_META_DATA);
            String key = info.metaData != null ? info.metaData.getString("com.google.android.geo.API_KEY") : null;
            return key != null && !key.isEmpty() && !key.equals("KEY_HERE");
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onDestroyView() {
        map = null;
        markersById.clear();
        iconCache.clear();
        lastDrawn = null;
        super.onDestroyView();
    }
}
