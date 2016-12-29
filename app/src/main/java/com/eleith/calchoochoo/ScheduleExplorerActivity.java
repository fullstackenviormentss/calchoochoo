package com.eleith.calchoochoo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;

import com.eleith.calchoochoo.dagger.ScheduleExplorerActivityComponent;
import com.eleith.calchoochoo.dagger.ScheduleExplorerActivityModule;
import com.eleith.calchoochoo.data.DatabaseHelper;
import com.eleith.calchoochoo.data.Stop;
import com.eleith.calchoochoo.fragments.DestinationSourceFragment;
import com.eleith.calchoochoo.fragments.HomeFragment;
import com.eleith.calchoochoo.fragments.SearchInputFragment;
import com.eleith.calchoochoo.fragments.SearchResultsFragment;
import com.eleith.calchoochoo.utils.BundleKeys;
import com.eleith.calchoochoo.utils.Permissions;
import com.eleith.calchoochoo.utils.RxBus;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessage;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageArrivalOrDepartDateTime;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageKeys;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessagePairStopReason;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageString;

import org.joda.time.LocalDateTime;
import org.parceler.Parcels;

import java.util.ArrayList;

import javax.inject.Inject;

import rx.functions.Action1;

public class ScheduleExplorerActivity extends AppCompatActivity {
  private SearchResultsFragment searchResultsFragment;
  private SearchInputFragment searchInputFragment;
  private DatabaseHelper databaseHelper;
  private Location location;
  private Stop stopDestination;
  private Stop stopSource;
  private Integer stopMethod = RxMessageArrivalOrDepartDateTime.ARRIVING;
  private LocalDateTime stopDateTime = new LocalDateTime();
  private LocationListener locationListener;
  private static final String SEARCH_REASON_DESTINATION = "destination";
  private static final String SEARCH_REASON_SOURCE = "source";
  private ScheduleExplorerActivityComponent scheduleExplorerActivityComponent;

  @Inject RxBus rxbus;
  @Inject LocationManager locationManager;
  @Inject FragmentManager fragmentManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    scheduleExplorerActivityComponent = ((ChooChooApplication) getApplication()).getAppComponent()
        .activityComponent(new ScheduleExplorerActivityModule(this));

    scheduleExplorerActivityComponent.inject(this);
    initializeLocationListener();

    setContentView(R.layout.schedule_explorer_activity);
    databaseHelper = new DatabaseHelper(this);
    updateDestinationSourceFragment();
    rxbus.observeEvents(RxMessage.class).subscribe(handleScheduleExplorerRxMessages());
  }

  private Action1<RxMessage> handleScheduleExplorerRxMessages() {
    return new Action1<RxMessage>() {
      @Override
      public void call(RxMessage rxMessage) {
        if (rxMessage.isMessageValidFor(RxMessageKeys.SEARCH_INPUT_STRING)) {
          filterSearchResults(((RxMessageString) rxMessage).getMessage());
        } else if (rxMessage.isMessageValidFor(RxMessageKeys.SEARCH_RESULT_PAIR)) {
          Pair<Stop, Integer> pair = ((RxMessagePairStopReason) rxMessage).getMessage();
          if (pair.second.equals(RxMessagePairStopReason.SEARCH_REASON_DESTINATION)) {
            stopDestination = pair.first;
          } else {
            stopSource = pair.first;
          }
          // if we have dest/source/method/time, we can do a search!
        } else if (rxMessage.isMessageValidFor(RxMessageKeys.DESTINATION_SELECTED)) {
          selectDestination();
        } else if (rxMessage.isMessageValidFor(RxMessageKeys.SOURCE_SELECTED)) {
          selectSource();
        } else if(rxMessage.isMessageValidFor(RxMessageKeys.DATE_TIME_SELECTED)) {
          Pair<Integer, LocalDateTime> pair = ((RxMessageArrivalOrDepartDateTime) rxMessage).getMessage();
          stopMethod = pair.first;
          stopDateTime = pair.second;
          // if we have dest/source/method/time, we can do a search!
        }
      }
    };
  }

  private void filterSearchResults(String filterString) {
    if (searchResultsFragment != null && searchResultsFragment.isVisible()) {
      searchResultsFragment.filterResultsBy(filterString, location);
    }
  }

  private void selectDestination() {
    searchInputFragment = new SearchInputFragment();
    searchResultsFragment = new SearchResultsFragment();

    Bundle searchResultsArgs = new Bundle();
    ArrayList<Stop> stops = databaseHelper.getAllStations();
    searchResultsArgs.putParcelable(BundleKeys.STOPS, Parcels.wrap(stops));
    searchResultsArgs.putParcelable(BundleKeys.LOCATION, location);
    searchResultsArgs.putInt(BundleKeys.SEARCH_REASON, RxMessagePairStopReason.SEARCH_REASON_DESTINATION);
    searchResultsFragment.setArguments(searchResultsArgs);

    updateFragments(searchInputFragment, searchResultsFragment);
  }

  private void selectSource() {
    searchInputFragment = new SearchInputFragment();
    searchResultsFragment = new SearchResultsFragment();

    Bundle searchResultsArgs = new Bundle();
    ArrayList<Stop> stops = databaseHelper.getAllStations();
    searchResultsArgs.putParcelable(BundleKeys.STOPS, Parcels.wrap(stops));
    searchResultsArgs.putParcelable(BundleKeys.LOCATION, location);
    searchResultsArgs.putInt(BundleKeys.SEARCH_REASON, RxMessagePairStopReason.SEARCH_REASON_SOURCE);
    searchResultsFragment.setArguments(searchResultsArgs);

    updateFragments(searchInputFragment, searchResultsFragment);
  }

  private void updateDestinationSourceFragment() {
    Bundle destinationSourceArgs = new Bundle();
    DestinationSourceFragment destinationSourceFragment = new DestinationSourceFragment();
    destinationSourceArgs.putParcelable(BundleKeys.STOP_DESTINATION, Parcels.wrap(stopDestination));
    destinationSourceArgs.putParcelable(BundleKeys.STOP_SOURCE, Parcels.wrap(stopSource));
    destinationSourceArgs.putInt(BundleKeys.STOP_METHOD, stopMethod);
    destinationSourceArgs.putLong(BundleKeys.STOP_DATETIME, stopDateTime.toDate().getTime());
    destinationSourceFragment.setArguments(destinationSourceArgs);

    updateFragments(destinationSourceFragment, new HomeFragment());
  }

  private void initializeLocationListener() {
    locationListener = new LocationListener() {
      public void onLocationChanged(Location newLocation) {
        stopListeningForLocation();
        location = newLocation;
      }

      public void onStatusChanged(String provider, int status, Bundle extras) {
      }

      public void onProviderEnabled(String provider) {
      }

      public void onProviderDisabled(String provider) {
      }
    };

    startListeningForLocation(true);
  }

  private void startListeningForLocation(Boolean shouldRequest) {
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 10000, locationListener);
      location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    } else if (shouldRequest) {
      requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Permissions.READ_GPS);
    }
  }

  private void stopListeningForLocation() {
    if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      locationManager.removeUpdates(locationListener);
    }
  }

  private void updateFragments(Fragment f1, Fragment f2) {
    FragmentTransaction ft = fragmentManager.beginTransaction();
    ft.replace(R.id.homeTopFragmentContainer, f1);
    ft.replace(R.id.homeFragmentContainer, f2);
    ft.addToBackStack(null);
    ft.commit();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    switch (requestCode) {
      case Permissions.READ_GPS: {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
          startListeningForLocation(false);
        }
      }
    }
  }

  public ScheduleExplorerActivityComponent getComponent() {
    return this.scheduleExplorerActivityComponent;
  }
}
