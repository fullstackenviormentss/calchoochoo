package com.eleith.calchoochoo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eleith.calchoochoo.R;
import com.eleith.calchoochoo.adapters.RouteViewAdapter;
import com.eleith.calchoochoo.ScheduleExplorerActivity;
import com.eleith.calchoochoo.data.PossibleTrip;
import com.eleith.calchoochoo.utils.BundleKeys;
import com.eleith.calchoochoo.utils.RxBus;

import org.parceler.Parcels;

import java.util.ArrayList;

import javax.inject.Inject;

public class RouteStopsFragment extends Fragment {
  private ArrayList<PossibleTrip> possibleTrips;

  @Inject RxBus rxBus;
  @Inject RouteViewAdapter routeViewAdapter;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    ((ScheduleExplorerActivity) getActivity()).getComponent().inject(this);
    unPackBundle(savedInstanceState != null ? savedInstanceState : getArguments());
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    unPackBundle(savedInstanceState);
    View view = inflater.inflate(R.layout.fragment_search_results, container, false);
    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.searchResults);

    if (recyclerView != null) {
      routeViewAdapter.setPossibleTrips(possibleTrips);

      recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
      recyclerView.setAdapter(routeViewAdapter);
    }

    return view;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
  }

  private void unPackBundle(Bundle bundle) {
    if (bundle != null) {
      possibleTrips = Parcels.unwrap(bundle.getParcelable(BundleKeys.ROUTE_STOPS));
    }
  }
}