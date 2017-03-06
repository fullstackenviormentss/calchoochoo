package com.eleith.calchoochoo.fragments;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eleith.calchoochoo.ChooChooActivity;
import com.eleith.calchoochoo.ChooChooFragmentManager;
import com.eleith.calchoochoo.R;
import com.eleith.calchoochoo.data.Queries;
import com.eleith.calchoochoo.data.Stop;
import com.eleith.calchoochoo.utils.BundleKeys;
import com.eleith.calchoochoo.utils.DeviceLocation;
import com.eleith.calchoochoo.utils.DrawableUtils;
import com.eleith.calchoochoo.utils.MapUtils;
import com.eleith.calchoochoo.utils.RxBus;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessage;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageKeys;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageStop;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageStopsAndDetails;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.joda.time.LocalDateTime;
import org.parceler.Parcels;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.functions.Action1;

import static com.eleith.calchoochoo.utils.DrawableUtils.getBitmapCircle;

public class MapSearchFragment extends Fragment implements OnMapReadyCallback {
  private GoogleMap googleMap;
  private MapView googleMapView;
  private ArrayList<Stop> stops;
  private Location lastLocation;
  private Marker locationMarker;
  private LatLng myDefaultLatLng = new LatLng(37.30, -122.06);
  private Subscription subscriptionLocation;
  private Subscription subscriptionRxBus;
  private ChooChooActivity chooChooActivity;

  @Inject
  RxBus rxBus;
  @Inject
  DeviceLocation deviceLocation;
  @Inject
  ChooChooFragmentManager chooChooFragmentManager;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    chooChooActivity = (ChooChooActivity) getActivity();
    chooChooActivity.getComponent().inject(this);
    unWrapBundle(savedInstanceState == null ? getArguments() : savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_map_search, container, false);
    ButterKnife.bind(this, view);

    unWrapBundle(savedInstanceState);
    chooChooActivity.fabEnable(R.drawable.ic_gps_not_fixed_black_24dp);

    // initialize the map!
    googleMapView = ((MapView) view.findViewById(R.id.search_google_maps));
    googleMapView.onCreate(savedInstanceState);
    googleMapView.getMapAsync(this);
    return view;
  }

  @OnClick(R.id.map_search_input)
  void onClickSearchInput() {
    Stop stop = Queries.findStopClosestTo(lastLocation);
    LocalDateTime stopDateTime = new LocalDateTime();
    int stopMethod = RxMessageStopsAndDetails.DETAIL_DEPARTING;
    chooChooFragmentManager.loadSearchForSpotFragment(stop, null, stopMethod, stopDateTime);
  }

  @Override
  public void onMapReady(final GoogleMap googleMap) {
    this.googleMap = googleMap;
    CameraPosition.Builder cameraBuilder = new CameraPosition.Builder().zoom(13);
    LatLng myLatLng;

    setStopMarkers();

    if (locationMarker != null) {
      locationMarker.remove();
      locationMarker = null;
    }

    if (lastLocation != null) {
      myLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
    } else {
      myLatLng = myDefaultLatLng;
    }

    cameraBuilder.target(myLatLng);
    CameraPosition cameraPosition = cameraBuilder.build();
    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
      @Override
      public boolean onMarkerClick(Marker marker) {
        String stopId = (String) marker.getTag();
        Stop touchedStop = Queries.getParentStopById(stopId);
        if (touchedStop != null) {
          chooChooFragmentManager.loadStopsFragments(touchedStop);
        }
        return true;
      }
    });

    deviceLocation.requestLocation(new DeviceLocation.LocationGetListener() {
      @Override
      public void onLocationGet(Location location) {
        MapUtils.moveMapToLocation(location, googleMap, new CameraPosition.Builder().zoom(13));
        setMyLocationMarker(location);
      }
    });

    subscriptionRxBus = rxBus.observeEvents(RxMessage.class).subscribe(handleRxMessages());
    subscriptionLocation = deviceLocation.subscribeToLocationUpdates(new DeviceLocation.LocationGetListener() {
      @Override
      public void onLocationGet(Location location) {
        setMyLocationMarker(location);
      }
    });
  }

  private void setStopMarkers() {
    for (Stop stop : stops) {
      LatLng stopLatLng = new LatLng(stop.stop_lat, stop.stop_lon);
      Bitmap trainIcon = DrawableUtils.getBitmapFromVectorDrawable(getContext(), R.drawable.ic_train_local, 0.25f);
      MarkerOptions markerOptions = new MarkerOptions().position(stopLatLng).title(stop.stop_name);
      markerOptions.icon(BitmapDescriptorFactory.fromBitmap(trainIcon));

      Marker marker = googleMap.addMarker(markerOptions);
      marker.setTag(stop.stop_id);
    }
  }

  private void unWrapBundle(Bundle bundle) {
    if (bundle != null) {
      stops = Parcels.unwrap(bundle.getParcelable(BundleKeys.STOPS));
      // if googleMap is set, then it never got the location!
      if (googleMap != null) {
        onMapReady(googleMap);
      }
    }
  }

  private void setMyLocationMarker(Location location) {
    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

    if (locationMarker != null) {
      MapUtils.animateMarker(locationMarker, latLng, googleMap);
    } else {
      MarkerOptions markerOptions = new MarkerOptions();
      markerOptions.position(latLng);
      markerOptions.icon(BitmapDescriptorFactory.fromBitmap(getBitmapCircle(104, 3, Color.RED, Color.WHITE)));
      locationMarker = googleMap.addMarker(markerOptions);
    }
    lastLocation = location;
    //mapActionButton.setVisibility(View.VISIBLE);
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
  public void onDestroyView() {
    super.onDestroyView();
    if (subscriptionLocation != null) {
      subscriptionLocation.unsubscribe();
    }
    if (subscriptionRxBus != null) {
      subscriptionRxBus.unsubscribe();
    }
    ((ChooChooActivity) getActivity()).fabDisable();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    googleMapView.onDestroy();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    googleMapView.onSaveInstanceState(outState);
    outState.putParcelable(BundleKeys.STOPS, Parcels.wrap(stops));
    super.onSaveInstanceState(outState);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    googleMapView.onLowMemory();
  }

  private Action1<RxMessage> handleRxMessages() {
    return new Action1<RxMessage>() {
      @Override
      public void call(RxMessage rxMessage) {
        if (rxMessage.isMessageValidFor(RxMessageKeys.FAB_CLICKED)) {
          LatLng myLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
          CameraPosition cameraPosition = new CameraPosition.Builder().zoom(13).target(myLatLng).build();
          googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
      }
    };
  }
}
