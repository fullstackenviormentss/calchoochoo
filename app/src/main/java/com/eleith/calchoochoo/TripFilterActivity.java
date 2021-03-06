package com.eleith.calchoochoo;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.eleith.calchoochoo.dagger.ChooChooComponent;
import com.eleith.calchoochoo.dagger.ChooChooModule;
import com.eleith.calchoochoo.data.ChooChooLoader;
import com.eleith.calchoochoo.data.PossibleTrip;
import com.eleith.calchoochoo.utils.BundleKeys;
import com.eleith.calchoochoo.utils.IntentKeys;
import com.eleith.calchoochoo.utils.RxBus;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessage;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageKeys;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessagePossibleTrips;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageStopMethodAndDateTime;
import com.eleith.calchoochoo.utils.RxBusMessage.RxMessageStopsAndDetails;
import com.google.android.gms.common.api.GoogleApiClient;

import org.joda.time.LocalDateTime;
import org.parceler.Parcel;
import org.parceler.Parcels;

import java.util.ArrayList;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Subscription;
import rx.functions.Action1;

public class TripFilterActivity extends AppCompatActivity {
  private ChooChooComponent chooChooComponent;
  private Subscription subscriptionTrips;
  private Subscription subscription;
  private ArrayList<PossibleTrip> possibleTrips;
  private String stopSourceId;
  private String stopDestinationId;
  private Integer stopMethod;
  private Long stopDateTime;
  private ChooChooFab chooChooFab;

  @Inject
  RxBus rxBus;
  @Inject
  ChooChooRouterManager chooChooRouterManager;
  @Inject
  ChooChooLoader chooChooLoader;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    chooChooComponent = ChooChooApplication.from(this).getAppComponent().activityComponent(new ChooChooModule(this));
    chooChooComponent.inject(this);

    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_appbar_drawer_fab);
    ButterKnife.bind(this);

    ChooChooDrawer chooChooDrawer = new ChooChooDrawer(this, getWindow().getDecorView().getRootView());
    chooChooFab = new ChooChooFab(this, rxBus, getWindow().getDecorView().getRootView());
    chooChooFab.setImageDrawable(getDrawable(R.drawable.ic_swap_vert_black_24dp));

    subscription = rxBus.observeEvents(RxMessage.class).subscribe(handleRxMessage());
    Intent intent = getIntent();

    if (savedInstanceState != null) {
      unWrapBundle(savedInstanceState);
    } else if (intent != null){
      Bundle bundle = intent.getExtras();
      if (bundle != null) {
        stopSourceId = bundle.getString(BundleKeys.STOP_SOURCE);
        stopDestinationId = bundle.getString(BundleKeys.STOP_DESTINATION);
        stopMethod = bundle.getInt(BundleKeys.STOP_METHOD);
        stopDateTime = bundle.getLong(BundleKeys.STOP_DATETIME, new LocalDateTime().toDateTime().getMillis());
      }

      if (stopSourceId != null && stopSourceId.equals(stopDestinationId)) {
        stopDestinationId = null;
      }

      if (stopMethod == null) {
        stopMethod = RxMessageStopsAndDetails.DETAIL_DEPARTING;
      }

      if (stopDateTime == null) {
        stopDateTime = new LocalDateTime().toDateTime().getMillis();
      }

      if (possibleTrips != null && possibleTrips.size() > 0) {
        chooChooRouterManager.loadTripFilterFragment(possibleTrips, stopMethod, new LocalDateTime(stopDateTime), stopSourceId, stopDestinationId);
      } else if (stopDestinationId != null && stopSourceId != null) {
        subscriptionTrips = rxBus.observeEvents(RxMessagePossibleTrips.class).take(1).subscribe(handleRxMessagePossibleTrips());
        chooChooLoader.loadPossibleTrips(stopSourceId, stopDestinationId, new LocalDateTime(stopDateTime));
      } else {
        chooChooRouterManager.loadTripFilterFragment(null, stopMethod, new LocalDateTime(stopDateTime), stopSourceId, stopDestinationId);
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    subscription = rxBus.observeEvents(RxMessage.class).subscribe(handleRxMessage());
  }

  @Override
  protected void onStop() {
    super.onStop();
    fabShow();
    if (subscriptionTrips != null) {
      subscriptionTrips.unsubscribe();
    }
    subscription.unsubscribe();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == IntentKeys.STOP_SEARCH_RESULT) {
      if (data != null) {
        Bundle bundle = data.getExtras();
        if (bundle != null) {
          String stopId = bundle.getString(BundleKeys.STOP);
          Integer reason = bundle.getInt(BundleKeys.SEARCH_REASON);
          if (resultCode == RESULT_OK) {
            if (reason == 1) {
              stopDestinationId = stopId;
            } else {
              stopSourceId = stopId;
            }
            subscriptionTrips = rxBus.observeEvents(RxMessagePossibleTrips.class).take(1).subscribe(handleRxMessagePossibleTrips());
            chooChooLoader.loadPossibleTrips(stopSourceId, stopDestinationId, new LocalDateTime(stopDateTime));
          }
        }
      }
    }
  }

  private void unWrapBundle(Bundle bundle) {
    stopSourceId = bundle.getString(BundleKeys.STOP_SOURCE);
    stopDestinationId = bundle.getString(BundleKeys.STOP_DESTINATION);
    stopMethod = bundle.getInt(BundleKeys.STOP_METHOD);
    stopDateTime = bundle.getLong(BundleKeys.STOP_DATETIME, new LocalDateTime().toDateTime().getMillis());
    possibleTrips = Parcels.unwrap(bundle.getParcelable(BundleKeys.POSSIBLE_TRIPS));
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putString(BundleKeys.STOP_SOURCE, stopSourceId);
    outState.putString(BundleKeys.STOP_DESTINATION, stopDestinationId);
    outState.putInt(BundleKeys.STOP_METHOD, stopMethod);
    outState.putLong(BundleKeys.STOP_DATETIME, stopDateTime);
    outState.putParcelable(BundleKeys.POSSIBLE_TRIPS, Parcels.wrap(possibleTrips));
    super.onSaveInstanceState(outState);
  }

  public ChooChooComponent getComponent() {
    return chooChooComponent;
  }

  private Action1<RxMessage> handleRxMessage() {
    return new Action1<RxMessage>() {
      @Override
      public void call(RxMessage rxMessage) {
        if (rxMessage.isMessageValidFor(RxMessageKeys.DATE_TIME_SELECTED)) {
          Pair<Integer, LocalDateTime> pair = ((RxMessageStopMethodAndDateTime) rxMessage).getMessage();
          stopMethod = pair.first;
          stopDateTime = pair.second.toDateTime().getMillis();
        }
      }
    };
  }

  private Action1<RxMessagePossibleTrips> handleRxMessagePossibleTrips() {
    return new Action1<RxMessagePossibleTrips>() {
      @Override
      public void call(RxMessagePossibleTrips rxMessage) {
        possibleTrips = rxMessage.getMessage();
        chooChooRouterManager.loadTripFilterFragment(possibleTrips, stopMethod, new LocalDateTime(stopDateTime), stopSourceId, stopDestinationId);
      }
    };
  }

  public void fabHide() {
    chooChooFab.hide();
  }

  public void fabShow() {
    chooChooFab.show();
  }
}
