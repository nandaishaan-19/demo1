import { Component, OnDestroy, OnInit } from '@angular/core';
import { DriverRequestService } from '../../services/driver-request.service';
import { DriverRequest } from '../../models/driver-request.model';
import { REQUEST_STAGES, REQUEST_STATUSES } from '../../utils/constants';
import { toDateString } from '../../utils/format';
import { getDriverImage, useDefaultAvatar } from '../../utils/driver-image';
import { TRIP_FILTERS, tripPhase } from '../../utils/trip';

@Component({
  selector: 'app-adminviewrequests',
  templateUrl: './adminviewrequests.component.html',
  styleUrls: ['./adminviewrequests.component.css']
})
export class AdminviewrequestsComponent implements OnInit, OnDestroy {
  requests: DriverRequest[] = [];
  /** True until the requests have arrived (shows skeleton table rows). */
  loading: boolean = true;
  filteredRequests: DriverRequest[] = [];
  searchText: string = '';
  statusFilter: string = 'All';
  statuses: string[] = REQUEST_STATUSES;
  /** The entries of the "Filter by" drop-down: all requests, upcoming / ongoing / completed trips, ... */
  filters = TRIP_FILTERS;
  stages: string[] = REQUEST_STAGES;

  selectedRequest: DriverRequest | null = null;
  showDriverModal: boolean = false;
  showStageModal: boolean = false;

  /** "Now" for the upcoming / ongoing split; refreshed every minute together with the filter. */
  now: Date = new Date();
  private clockTimer: any;

  toDateString = toDateString;
  getDriverImage = getDriverImage;
  useDefaultAvatar = useDefaultAvatar;

  constructor(private driverRequestService: DriverRequestService) {}

  ngOnInit(): void {
    this.loadRequests();
    this.clockTimer = setInterval(() => {
      this.now = new Date();
      this.applyFilters();
    }, 60000);
  }

  ngOnDestroy(): void {
    clearInterval(this.clockTimer);
  }

  loadRequests(): void {
    this.driverRequestService.getAllDriverRequests().subscribe({
      // The API answers 204 (empty body) when there are no requests.
      next: (requests) => {
        this.requests = requests || [];
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.requests = [];
        this.applyFilters();
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    const query = this.searchText.trim().toLowerCase();
    this.filteredRequests = this.requests.filter((request) => {
      const matchesStatus = this.statusFilter === 'All' || this.phaseOf(request) === this.statusFilter;
      const matchesText =
        !query ||
        (request.driver?.driverName || '').toLowerCase().includes(query) ||
        (request.pickupLocation || '').toLowerCase().includes(query);
      return matchesStatus && matchesText;
    });
  }

  /** The request status refined with the clock: an approved trip is "Upcoming" before its start and "Ongoing" after it. */
  phaseOf(request: DriverRequest): string {
    return tripPhase(request, this.now);
  }

  /** Number of requests behind a drop-down entry, shown next to its label. */
  countFor(filter: string): number {
    return filter === 'All' ? this.requests.length : this.requests.filter((request) => this.phaseOf(request) === filter).length;
  }

  canApprove(request: DriverRequest): boolean {
    return request.status === 'Pending' || request.status === 'Rejected';
  }

  canReject(request: DriverRequest): boolean {
    return request.status === 'Pending' || request.status === 'Approved';
  }

  approve(request: DriverRequest): void {
    this.changeStatus(request, 'Approved');
  }

  reject(request: DriverRequest): void {
    this.changeStatus(request, 'Rejected');
  }

  private changeStatus(request: DriverRequest, status: string, onDone?: () => void): void {
    if (request.driverRequestId === undefined) {
      return;
    }
    const update = { status } as unknown as DriverRequest;
    this.driverRequestService.updateDriverRequest(request.driverRequestId, update).subscribe({
      next: () => {
        request.status = status;
        this.applyFilters();
        if (onDone) {
          onDone();
        }
      }
    });
  }

  openDriverDetails(request: DriverRequest): void {
    this.selectedRequest = request;
    this.showDriverModal = true;
  }

  closeDriverModal(): void {
    this.showDriverModal = false;
    this.selectedRequest = null;
  }

  openStages(request: DriverRequest): void {
    this.selectedRequest = request;
    this.showStageModal = true;
  }

  closeStageModal(): void {
    this.showStageModal = false;
    this.selectedRequest = null;
  }

  /** Stages shown in the progression modal: a rejected request stops at "Rejected". */
  get visibleStages(): string[] {
    const status = this.selectedRequest?.status;
    if (status === 'Rejected') {
      return ['Pending', 'Rejected'];
    }
    if (status === 'Cancelled') {
      return ['Pending', 'Approved', 'Cancelled'];
    }
    return this.stages;
  }

  stageClass(stage: string): string {
    const status = this.selectedRequest?.status || '';
    if (status === 'Rejected') {
      return stage === 'Rejected' ? 'rejected' : 'idle';
    }
    if (status === 'Cancelled') {
      return stage === 'Cancelled' ? 'rejected' : 'done';
    }
    return this.stages.indexOf(stage) <= this.stages.indexOf(status) ? 'done' : 'idle';
  }

  get canCloseRequest(): boolean {
    return this.selectedRequest?.status === 'Trip End';
  }

  closeRequest(): void {
    if (this.selectedRequest && this.canCloseRequest) {
      this.changeStatus(this.selectedRequest, 'Closed', () => this.closeStageModal());
    }
  }
}
