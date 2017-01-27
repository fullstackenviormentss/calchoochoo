package com.eleith.calchoochoo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eleith.calchoochoo.R;
import com.eleith.calchoochoo.ScheduleExplorerActivity;
import com.eleith.calchoochoo.data.Queries;
import com.eleith.calchoochoo.data.Stop;
import com.eleith.calchoochoo.data.StopTimes;
import com.eleith.calchoochoo.utils.BundleKeys;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.parceler.Parcels;

import java.util.ArrayList;

import butterknife.ButterKnife;

public class StopSummaryFragment extends Fragment implements OnMapReadyCallback {
  private GoogleMap googleMap;
  private MapView googleMapView;
  private Stop stopDestination;
  private Stop stopSource;
  private String tripId;
  // private ArrayList<Pair<Stop, StopTimes>> tripStops;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((ScheduleExplorerActivity) getActivity()).getComponent().inject(this);
    unWrapBundle(getArguments());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_stop_summary, container, false);
    ButterKnife.bind(this, view);

    unWrapBundle(savedInstanceState);

    // initialize the map!
    googleMapView = ((MapView) view.findViewById(R.id.trip_google_maps));
    googleMapView.onCreate(savedInstanceState);
    googleMapView.getMapAsync(this);

    return view;
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    this.googleMap = googleMap;

    LatLng sourceLatLng = new LatLng(stopSource.stop_lat, stopSource.stop_lon);
    LatLng destinationLatLng = new LatLng(stopDestination.stop_lat, stopDestination.stop_lon);
    LatLngBounds latLngBounds = new LatLngBounds(sourceLatLng, destinationLatLng);
    googleMap.addMarker(new MarkerOptions().position(sourceLatLng).title(stopSource.stop_name));
    googleMap.addMarker(new MarkerOptions().position(destinationLatLng).title(stopDestination.stop_name));

    CameraPosition cameraPosition = new CameraPosition.Builder().zoom(10).bearing(90).target(latLngBounds.getCenter()).build();
    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }

  private void unWrapBundle(Bundle savedInstanceState) {
     if (savedInstanceState != null) {
      Log.d("poop", "unwrapping bundle");
      stopDestination = Parcels.unwrap(savedInstanceState.getParcelable(BundleKeys.STOP_DESTINATION));
      stopSource = Parcels.unwrap(savedInstanceState.getParcelable(BundleKeys.STOP_SOURCE));
      // tripId = savedInstanceState.getString(BundleKeys.TRIP_ID);
      // tripStops = Queries.findTripDetails(tripId);
      Log.d("poop", stopSource.stop_name);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    googleMapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    googleMapView.onPause();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    googleMapView.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    googleMapView.onSaveInstanceState(outState);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    googleMapView.onLowMemory();
  }
}
