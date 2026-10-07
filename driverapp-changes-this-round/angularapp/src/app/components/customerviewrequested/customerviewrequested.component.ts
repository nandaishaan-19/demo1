import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DriverRequestService } from '../../services/driver-request.service';
import { AuthService } from '../../services/auth.service';
import { DriverRequest } from '../../models/driver-request.model';
import {
  combineDateTime,
  formatDuration,
  parseDurationToMinutes,
  toDateString,
  toDisplayDate,
  toTimeString
} from '../../utils/format';
import { getDriverImage, useDefaultAvatar } from '../../utils/driver-image';
import { canCancelTrip, canEndTripNow, tripHint } from '../../utils/trip';
import { serverMessage } from '../../utils/http-errors';

@Component({
  selector: 'app-customerviewrequested',
  templateUrl: './customerviewrequested.component.html',
  styleUrls: ['./customerviewrequested.component.css']
})
export class CustomerviewrequestedComponent implements OnInit, OnDestroy {
  requests: DriverRequest[] = [];
  /** True until the requests have arrived (shows the skeleton cards). */
  loading: boolean = true;
  searchText: string = '';

  selectedRequest: DriverRequest | null = null;
  showDetailsModal: boolean = false;
  showDeleteModal: boolean = false;
  showCancelModal: boolean = false;
  cancelError: string = '';
  cancelling: boolean = false;
  endTripError: string = '';
  showPayModal: boolean = false;
  payLoading: boolean = false;
  private payTimer: any;

  /** "Now", refreshed every 30 seconds so the Cancel / Trip End buttons switch on their own when the time comes. */
  now: Date = new Date();
  private clockTimer: any;

  toDateString = toDateString;
  toTimeString = toTimeString;
  toDisplayDate = toDisplayDate;
  getDriverImage = getDriverImage;
  useDefaultAvatar = useDefaultAvatar;

  constructor(
    private driverRequestService: DriverRequestService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadRequests();
    this.clockTimer = setInterval(() => (this.now = new Date()), 30000);
  }

  ngOnDestroy(): void {
    clearTimeout(this.payTimer);
    clearInterval(this.clockTimer);
  }

  loadRequests(): void {
    const userId = this.authService.getUserId();
    if (userId === null) {
      this.loading = false;
      return;
    }
    this.driverRequestService.getDriverRequestsByUserId(userId).subscribe({
      // The API answers 404 / 204 when the customer has no requests yet.
      next: (requests) => {
        this.requests = requests || [];
        this.loading = false;
      },
      error: () => {
        this.requests = [];
        this.loading = false;
      }
    });
  }

  get filteredRequests(): DriverRequest[] {
    const query = this.searchText.trim().toLowerCase();
    if (!query) {
      return this.requests;
    }
    return this.requests.filter((request) => (request.driver?.driverName || '').toLowerCase().includes(query));
  }

  isPending(request: DriverRequest): boolean {
    return request.status === 'Pending';
  }

  /** Trip End works only for an approved trip whose start time has been reached. */
  canEndTrip(request: DriverRequest): boolean {
    return canEndTripNow(request, this.now);
  }

  /** Cancel works only for an approved trip, up to 24 hours before it starts. */
  canCancel(request: DriverRequest): boolean {
    return canCancelTrip(request, this.now);
  }

  /** The explanation shown under the buttons of an approved trip that has not started yet. */
  hintFor(request: DriverRequest): string {
    return tripHint(request, this.now);
  }

  cancelTooltip(request: DriverRequest): string {
    if (request.status !== 'Approved') {
      return 'Only an approved trip can be cancelled';
    }
    return this.canCancel(request) ? 'Cancel this trip' : 'Cancelling is closed: less than 24 hours before the trip';
  }

  tripEndTooltip(request: DriverRequest): string {
    if (request.status !== 'Approved') {
      return 'Only an approved trip can be ended';
    }
    return this.canEndTrip(request) ? 'End this trip' : 'The trip has not started yet';
  }

  canFetchPayAmount(request: DriverRequest): boolean {
    return request.status === 'Trip End' || request.status === 'Closed';
  }

  // ----- details -----
  showMore(request: DriverRequest): void {
    this.selectedRequest = request;
    this.showDetailsModal = true;
  }

  closeDetails(): void {
    this.showDetailsModal = false;
    this.selectedRequest = null;
  }

  // ----- edit / delete -----
  editRequest(request: DriverRequest): void {
    if (this.isPending(request)) {
      this.router.navigate(['/customer-request/edit', request.driverRequestId]);
    }
  }

  askDelete(request: DriverRequest): void {
    if (this.isPending(request)) {
      this.selectedRequest = request;
      this.showDeleteModal = true;
    }
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.selectedRequest = null;
  }

  confirmDelete(): void {
    const id = this.selectedRequest?.driverRequestId;
    if (id === undefined) {
      return;
    }
    this.driverRequestService.deleteDriverRequest(id).subscribe({
      next: () => {
        this.requests = this.requests.filter((request) => request.driverRequestId !== id);
        this.cancelDelete();
      },
      error: () => this.cancelDelete()
    });
  }

  // ----- cancel (up to 24 hours before the trip) -----
  askCancel(request: DriverRequest): void {
    if (this.canCancel(request)) {
      this.selectedRequest = request;
      this.cancelError = '';
      this.showCancelModal = true;
    }
  }

  closeCancelModal(): void {
    this.showCancelModal = false;
    this.cancelError = '';
    this.cancelling = false;
    this.selectedRequest = null;
  }

  confirmCancel(): void {
    const request = this.selectedRequest;
    if (!request || request.driverRequestId === undefined || this.cancelling) {
      return;
    }
    this.cancelling = true;
    this.cancelError = '';
    const update = { status: 'Cancelled' } as unknown as DriverRequest;
    this.driverRequestService.updateDriverRequest(request.driverRequestId, update).subscribe({
      next: () => {
        request.status = 'Cancelled';
        this.closeCancelModal();
      },
      error: (err) => {
        // The server checks the 24 hour rule again (the page clock may differ from the server clock).
        this.cancelling = false;
        this.cancelError = serverMessage(err, 'The trip could not be cancelled. Please try again.');
      }
    });
  }

  // ----- trip end (only after the trip has started) -----
  endTrip(request: DriverRequest): void {
    if (!this.canEndTrip(request) || request.driverRequestId === undefined) {
      return;
    }
    this.endTripError = '';
    const now = new Date();
    const start = combineDateTime(request.tripDate, request.timeSlot);
    let minutes = start ? Math.round((now.getTime() - start.getTime()) / 60000) : 0;
    if (minutes <= 0) {
      // The trip was ended in the very minute it started: use the estimated duration for the fare.
      minutes = parseDurationToMinutes(request.estimatedDuration);
    }
    const hourlyRate = Number(request.driver?.hourlyRate || 0);
    const paymentAmount = Math.round(hourlyRate * (minutes / 60) * 100) / 100;

    const update = {
      status: 'Trip End',
      actualDropDate: toDateString(now),
      actualDropTime: toTimeString(now, true),
      actualDuration: formatDuration(minutes),
      paymentAmount
    };
    this.driverRequestService
      .updateDriverRequest(request.driverRequestId, update as unknown as DriverRequest)
      .subscribe({
        next: () => {
          request.status = update.status;
          request.actualDropDate = update.actualDropDate as unknown as Date;
          request.actualDropTime = update.actualDropTime as unknown as Date;
          request.actualDuration = update.actualDuration;
          request.paymentAmount = update.paymentAmount;
        },
        error: (err) => (this.endTripError = serverMessage(err, 'The trip could not be ended. Please try again.'))
      });
  }

  // ----- payment -----
  fetchPayAmount(request: DriverRequest): void {
    this.selectedRequest = request;
    this.showPayModal = true;
    this.payLoading = true;
    clearTimeout(this.payTimer);
    this.payTimer = setTimeout(() => (this.payLoading = false), 1500);
  }

  closePayModal(): void {
    clearTimeout(this.payTimer);
    this.showPayModal = false;
    this.payLoading = false;
    this.selectedRequest = null;
  }

  /** The "Write a Review" button on the request card (enabled for the same requests as "Fetch Pay Amount"). */
  writeReview(request: DriverRequest): void {
    if (!this.canFetchPayAmount(request)) {
      return;
    }
    const driverId = request.driver?.driverId;
    this.router.navigate(['/customerpostfeedback'], { queryParams: driverId !== undefined ? { driverId } : {} });
  }
}
