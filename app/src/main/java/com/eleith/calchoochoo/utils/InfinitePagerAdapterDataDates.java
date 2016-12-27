package com.eleith.calchoochoo.utils;

import android.support.v4.view.ViewPager;
import android.util.Log;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class InfinitePagerAdapterDataDates extends InfinitePagerAdapterData<LocalDate> {
  private LocalDate today = new LocalDate();
  private LocalDate tomorrow = today.plusDays(1);
  private LocalDate yesterday = today.minusDays(1);
  private DateTimeFormatter dateDisplayFormat = DateTimeFormat.forPattern("MMM dd, yyyy");

  public InfinitePagerAdapterDataDates(final ViewPager viewPager, LocalDate[] dataArray) {
    super(viewPager, dataArray);
  }

  @Override
  public LocalDate getNextData() {
    return getData(getDataSize() - 1).plusDays(1);
  }

  @Override
  public LocalDate getPreviousData() {
    return getData(0).minusDays(1);
  }

  @Override
  public String getTextFor(int position) {
    if (getData(position).isEqual(today)) {
      return "today";
    } else if (getData(position).isEqual(tomorrow)) {
      return "tomorrow";
    } else if (getData(position).isEqual(yesterday)) {
      return "yesterday";
    } else {
      return dateDisplayFormat.print(getData(position));
    }
  }
}
