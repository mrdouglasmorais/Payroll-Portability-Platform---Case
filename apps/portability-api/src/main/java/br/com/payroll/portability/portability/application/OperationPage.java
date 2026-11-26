package br.com.payroll.portability.portability.application;

import java.util.List;

/** Zero-based page of operation summaries. */
public record OperationPage(List<OperationSummary> items, int page, int size, long totalElements) {

  public int totalPages() {
    if (size <= 0) {
      return 0;
    }
    return (int) Math.ceil((double) totalElements / (double) size);
  }

  public boolean hasNext() {
    return page + 1 < totalPages();
  }

  public boolean hasPrevious() {
    return page > 0 && totalElements > 0;
  }
}
