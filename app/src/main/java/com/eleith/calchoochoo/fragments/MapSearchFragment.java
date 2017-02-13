package com.eleith.calchoochoo.fragments;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eleith.calchoochoo.R;
import com.eleith.calchoochoo.ScheduleExplorerActivity;
import com.eleith.calchoochoo.data.Queries;
import com.eleith.calchoochoo.data.Stop;
import com.eleith.calchoochoo.utils.BundleKeys;
import com.eleith.calchoochoo.utils.RxBus;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessage;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageKeys;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageStop;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.parceler.Parcels;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MapSearchFragment extends Fragment implements OnMapReadyCallback {
  private GoogleMap googleMap;
  private MapView googleMapView;
  private ArrayList<Stop> stops;
  private Location location;

  @Inject
  RxBus rxBus;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((ScheduleExplorerActivity) getActivity()).getComponent().inject(this);
    unWrapBundle(getArguments());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_map_search, container, false);
    ButterKnife.bind(this, view);

    unWrapBundle(savedInstanceState);

    // initialize the map!
    googleMapView = ((MapView) view.findViewById(R.id.search_google_maps));
    googleMapView.onCreate(savedInstanceState);
    googleMapView.getMapAsync(this);

    return view;
  }

  @OnClick(R.id.map_search_input)
  void onClickSearchInput() {
    rxBus.send(new RxMessage(RxMessageKeys.DESTINATION_SELECTED));
  }

  @OnClick(R.id.map_action_button)
  void onClickActionButton() {
    LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());
    CameraPosition cameraPosition = new CameraPosition.Builder().zoom(13).target(myLatLng).build();
    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
  }

  @Override
  public void onMapReady(GoogleMap googleMap) {
    this.googleMap = googleMap;

    if (location != null) {
      LatLng myLatLng = new LatLng(location.getLatitude(), location.getLongitude());

      for (Stop stop : stops) {
        LatLng stopLatLng = new LatLng(stop.stop_lat, stop.stop_lon);
        MarkerOptions markerOptions = new MarkerOptions().position(stopLatLng).title(stop.stop_name);
        Marker marker = googleMap.addMarker(markerOptions);
        marker.setTag(stop.stop_id);
      }

      CameraPosition cameraPosition = new CameraPosition.Builder().zoom(13).target(myLatLng).build();
      googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

      googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
        @Override
        public boolean onMarkerClick(Marker marker) {
          String stopId = (String) marker.getTag();
          Stop touchedStop = Queries.getStopById(stopId);
          rxBus.send(new RxMessageStop(RxMessageKeys.STOP_SELECTED, touchedStop));
          return true;
        }
      });
    }
  }

  private void unWrapBundle(Bundle savedInstanceState) {
     if (savedInstanceState != null) {
      stops = Parcels.unwrap(savedInstanceState.getParcelable(BundleKeys.STOPS));
      location = savedInstanceState.getParcelable(BundleKeys.LOCATION);
       // if googleMap is set, then it never got the location!
       if (googleMap != null) {
         onMapReady(googleMap);
       }
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
