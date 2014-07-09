package com.mapzen.route;

import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import com.mapzen.R;
import com.mapzen.activity.BaseActivity;
import com.mapzen.entity.SimpleFeature;
import com.mapzen.fragment.BaseFragment;
import com.mapzen.osrm.Route;
import com.mapzen.osrm.Router;
import com.mapzen.widget.DistanceView;

import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.layers.PathLayer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;

import static com.mapzen.MapController.geoPointToPair;
import static com.mapzen.MapController.getMapController;
import static com.mapzen.MapController.locationToGeoPoint;
import static com.mapzen.MapController.locationToPair;
import static com.mapzen.entity.SimpleFeature.NAME;
import static com.mapzen.osrm.Router.Type;
import static com.mapzen.osrm.Router.Type.BIKING;
import static com.mapzen.osrm.Router.Type.DRIVING;
import static com.mapzen.osrm.Router.Type.WALKING;

public class RoutePreviewFragment extends BaseFragment
        implements Router.Callback {
    public static final String TAG = RoutePreviewFragment.class.getSimpleName();
    public static final int ROUTE_ZOOM_LEVEL = 19;
    private SimpleFeature destination;
    private boolean reverse = false;
    private Type transportationMode = DRIVING;
    private Route route;

    @Inject PathLayer path;
    @Inject ItemizedLayer<MarkerItem> markers;

    @Inject Router router;
    @InjectView(R.id.starting_point) TextView startingPointTextView;
    @InjectView(R.id.destination) TextView destinationTextView;
    @InjectView(R.id.destination_preview) TextView destinationPreview;
    @InjectView(R.id.destination_preview_distance) DistanceView destinationPreviewDistance;
    @InjectView(R.id.route_reverse) ImageButton routeReverse;
    @InjectView(R.id.by_car) RadioButton byCar;
    @InjectView(R.id.by_foot) RadioButton byFoot;
    @InjectView(R.id.by_bike) RadioButton byBike;
    @InjectView(R.id.start) TextView startBtn;
    @InjectView(R.id.top_row) LinearLayout topRow;
    @InjectView(R.id.routing_options) LinearLayout routingMode;
    @InjectView(R.id.border) View border;
    @InjectView(R.id.destination_container) RelativeLayout destinationContainer;
    @InjectView(R.id.divider) View divider;
    @InjectView(R.id.starting_location_icon) ImageView startLocationIcon;
    @InjectView(R.id.destination_location_icon) ImageView destinationLocationIcon;
    @InjectView(R.id.start_location_layout) LinearLayout startLocationLayout;
    @InjectView(R.id.destination_layout) LinearLayout destinationLayout;
    @InjectView(R.id.to_text) TextView toTextView;
    @InjectView(R.id.from_text) TextView fromTextView;
    @InjectView(R.id.location_divider) LinearLayout locationDivider;
    public static RoutePreviewFragment newInstance(BaseActivity act,
                                                   SimpleFeature destination) {
        final RoutePreviewFragment fragment = new RoutePreviewFragment();
        fragment.setAct(act);
        fragment.setMapFragment(act.getMapFragment());
        fragment.inject();
        fragment.setDestination(destination);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        act.hideActionBar();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getMapController().getMap().layers().remove(markers);
        getMapController().getMap().layers().remove(path);
        mapFragment.updateMap();
        act.enableActionbar();
        act.showActionBar();
        unregisterViewUpdater();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.route_preview, container, false);
        ButterKnife.inject(this, view);
        setOriginAndDestination();
        registerViewUpdater();
        return view;
    }

    private void setOriginAndDestination() {
        toTextView.setVisibility(View.VISIBLE);
        fromTextView.setVisibility(View.VISIBLE);

        if (!reverse) {
            startingPointTextView.setText(getString(R.string.current_location));
            destinationTextView.setText(destination.getProperty(NAME));
            destinationPreview.setText(destination.getProperty(NAME));
            startLocationIcon.setVisibility(View.VISIBLE);
            destinationLocationIcon.setVisibility(View.GONE);
            startBtn.setText(getString(R.string.start));

        } else {
            startingPointTextView.setText(destination.getProperty(NAME));
            destinationTextView.setText(getString(R.string.current_location));
            destinationPreview.setText(getString(R.string.current_location));
            startLocationIcon.setVisibility(View.GONE);
            destinationLocationIcon.setVisibility(View.VISIBLE);
            startBtn.setText(getString(R.string.view));
        }
        if (route != null) {
            destinationPreviewDistance.setDistance(route.getTotalDistance());
        }
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.route_reverse) public void reverse() {
        reverse = !reverse;
        toTextView.setVisibility(View.GONE);
        fromTextView.setVisibility(View.GONE);
        animateDestinationReverse();
        setOriginAndDestination();
        createRouteToDestination();
    }

    private void animateDestinationReverse() {
        Animation rotateAnimation = AnimationUtils.loadAnimation(act, R.anim.rotate180);
        routeReverse.startAnimation(rotateAnimation);
        Animation moveDown = AnimationUtils.loadAnimation(act, R.anim.move_down);
        Animation moveUp = AnimationUtils.loadAnimation(act, R.anim.move_up);

        startLocationLayout.startAnimation(moveDown);
        destinationLayout.startAnimation(moveUp);
    }

    @SuppressWarnings("unused")
    @OnCheckedChanged(R.id.by_car) public void byCar(boolean active) {
        if (active) {
            transportationMode = DRIVING;
            createRouteToDestination();
        }
    }
    @SuppressWarnings("unused")
    @OnCheckedChanged(R.id.by_bike) public void byBike(boolean active) {
        if (active) {
            transportationMode = BIKING;
            createRouteToDestination();
        }
    }
    @SuppressWarnings("unused")
    @OnCheckedChanged(R.id.by_foot) public void byFoot(boolean active) {
        if (active) {
            transportationMode = WALKING;
            createRouteToDestination();
        }
    }

    @SuppressWarnings("unused")
    @OnClick(R.id.start) public void start() {
        if (!reverse) {
            startRouting();
        } else {
            showDirectionListFragment();
        }
    }

    public void createRouteToDestination() {
        mapFragment.clearMarkers();
        mapFragment.updateMap();
        act.showProgressDialog();
        router.clearLocations()
                .setLocation(getOriginPoint())
                .setLocation(getDestinationPoint())
                .setZoomLevel(ROUTE_ZOOM_LEVEL)
                .setCallback(this);
        if (transportationMode.equals(DRIVING)) {
            router.setDriving();
        } else if (transportationMode.equals(WALKING)) {
            router.setWalking();
        } else if (transportationMode.equals(BIKING)) {
            router.setBiking();
        }
        router.fetch();
    }

    private double[] getDestinationPoint() {
        return reverse ? locationToPair(getMapController().getLocation()) :
                geoPointToPair(destination.getGeoPoint());
    }

    private double[] getOriginPoint() {
        return !reverse ? locationToPair(getMapController().getLocation()) :
                geoPointToPair(destination.getGeoPoint());
    }

    public void setDestination(SimpleFeature destination) {
        this.destination = destination;
    }

    @Override
    public void success(Route route) {
        this.route = route;
        if (!isAdded()) {
            act.getSupportFragmentManager().beginTransaction()
                    .addToBackStack(null)
                    .add(R.id.routes_preview_container, this, TAG)
                    .commit();
        }
        act.dismissProgressDialog();
        ArrayList<Location> points = route.getGeometry();
        path.clearPath();
        double minlat = points.get(0).getLatitude();
        double minlon = points.get(0).getLongitude();
        double maxlat = points.get(0).getLatitude();
        double maxlon = points.get(0).getLongitude();
        for (Location loc : points) {
            if (maxlat < loc.getLatitude()) {
                maxlat = loc.getLatitude();
            }
            if (maxlon < loc.getLongitude()) {
                maxlon = loc.getLongitude();
            }
            if (minlat > loc.getLatitude()) {
                minlat = loc.getLatitude();
            }
            if (minlon > loc.getLongitude()) {
                minlon = loc.getLongitude();
            }
            path.addPoint(locationToGeoPoint(loc));
        }

        BoundingBox bbox = new BoundingBox(minlat, minlon, maxlat, maxlon);
        int w = getMapController().getMap().getWidth();
        int h = getMapController().getMap().getHeight();
        MapPosition position = new MapPosition();
        position.setByBoundingBox(bbox, w, h);

        position.setScale(position.getZoomScale() * 0.85);

        getMapController().getMap().setMapPosition(position);

        if (!getMapController().getMap().layers().contains(path)) {
            getMapController().getMap().layers().add(path);
        }

        if (!getMapController().getMap().layers().contains(markers)) {
            getMapController().getMap().layers().add(markers);
        }
        markers.removeAllItems();
        markers.addItem(getMarkerItem(R.drawable.ic_a, points.get(0)));
        markers.addItem(getMarkerItem(R.drawable.ic_b, points.get(points.size() - 1)));
    }

    @Override
    public void failure(int statusCode) {
        path.clearPath();
        onServerError(statusCode);
    }

    @Override
    public void onViewUpdate() {
        createRouteToDestination();
    }

    private MarkerItem getMarkerItem(int icon, Location loc) {
        MarkerItem markerItem = new MarkerItem("Generic Marker",
                "Generic Description", locationToGeoPoint(loc));
        markerItem.setMarker(new MarkerSymbol(
                AndroidGraphics.drawableToBitmap(app.getResources().getDrawable(icon)),
                MarkerItem.HotspotPlace.BOTTOM_CENTER));
        return markerItem;
    }

    private void showDirectionListFragment() {
        final Fragment fragment = DirectionListFragment.
                newInstance(route.getRouteInstructions(),
                        new DirectionListFragment.DirectionListener() {
                            @Override
                            public void onInstructionSelected(int index) {
                            }
                        });
        act.getSupportFragmentManager().beginTransaction()
                .add(R.id.full_list, fragment, DirectionListFragment.TAG)
                .addToBackStack(null)
                .commit();
    }

    private void startRouting() {
        hideFragmentContents();
        RouteFragment routeFragment = RouteFragment.newInstance(act, destination);
        routeFragment.setRoute(route);
        act.getSupportFragmentManager().beginTransaction()
                .addToBackStack(null)
                .add(R.id.routes_container, routeFragment, RouteFragment.TAG)
                .commit();
        getMapController().getMap().layers().remove(markers);
    }

    private void hideFragmentContents() {
        topRow.setVisibility(View.INVISIBLE);
        routingMode.setVisibility(View.INVISIBLE);
        border.setVisibility(View.INVISIBLE);
        destinationContainer.setVisibility(View.INVISIBLE);
        divider.setVisibility(View.INVISIBLE);
        locationDivider.setVisibility(View.INVISIBLE);
    }

    public void showFragmentContents() {
        topRow.setVisibility(View.VISIBLE);
        routingMode.setVisibility(View.VISIBLE);
        border.setVisibility(View.VISIBLE);
        destinationContainer.setVisibility(View.VISIBLE);
        divider.setVisibility(View.VISIBLE);
        locationDivider.setVisibility(View.VISIBLE);
    }
}
