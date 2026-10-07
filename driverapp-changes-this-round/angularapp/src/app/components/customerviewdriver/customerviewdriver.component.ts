import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DriverService } from '../../services/driver.service';
import { DriverRequestService } from '../../services/driver-request.service';
import { AiService } from '../../services/ai.service';
import { AuthService } from '../../services/auth.service';
import { Driver } from '../../models/driver.model';
import { getDriverImage, useDefaultAvatar } from '../../utils/driver-image';

/** Request statuses that mean "this driver is currently requested by me". */
const OPEN_STATUSES = ['Pending', 'Approved', 'Trip End'];

@Component({
  selector: 'app-customerviewdriver',
  templateUrl: './customerviewdriver.component.html',
  styleUrls: ['./customerviewdriver.component.css']
})
export class CustomerviewdriverComponent implements OnInit {
  drivers: Driver[] = [];
  /** True until the driver list has arrived (shows the skeleton cards). */
  loadingDrivers: boolean = true;
  searchText: string = '';

  // AI (semantic) search
  aiQuery: string = '';
  aiActive: boolean = false;
  aiLoading: boolean = false;
  aiResults: Driver[] = [];
  aiMessage: string = '';

  /** driverId -> status ("Pending", "Approved" or "Trip End") of the customer's open request for that driver. */
  requestedDrivers: Map<number, string> = new Map<number, string>();

  getDriverImage = getDriverImage;
  useDefaultAvatar = useDefaultAvatar;

  constructor(
    private driverService: DriverService,
    private driverRequestService: DriverRequestService,
    private aiService: AiService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.driverService.getAllDrivers().subscribe({
      // The API answers 204 (empty body) when there are no drivers.
      next: (drivers) => {
        this.drivers = drivers || [];
        this.loadingDrivers = false;
      },
      error: () => {
        this.drivers = [];
        this.loadingDrivers = false;
      }
    });
    this.loadMyRequests();
  }

  private loadMyRequests(): void {
    const userId = this.authService.getUserId();
    if (userId === null) {
      return;
    }
    this.driverRequestService.getDriverRequestsByUserId(userId).subscribe({
      next: (requests) => {
        const open = new Map<number, string>();
        (requests || [])
          .filter((request) => OPEN_STATUSES.includes(request.status))
          .forEach((request) => {
            const id = request.driver?.driverId;
            // an approved request wins over a second, pending one for the same driver
            if (id !== undefined && id !== null && open.get(id) !== 'Approved') {
              open.set(id, request.status);
            }
          });
        this.requestedDrivers = open;
      },
      error: () => (this.requestedDrivers = new Map<number, string>()) // 404 = no requests yet
    });
  }

  /** Drivers shown on the page: the AI matches (when active) narrowed down by the text search. */
  get displayedDrivers(): Driver[] {
    const base = this.aiActive ? this.aiResults : this.drivers;
    const query = this.searchText.trim().toLowerCase();
    if (!query) {
      return base;
    }
    return base.filter(
      (driver) =>
        (driver.driverName || '').toLowerCase().includes(query) ||
        (driver.vehicleType || '').toLowerCase().includes(query) ||
        (driver.licenseNumber || '').toLowerCase().includes(query)
    );
  }

  /** True while the customer has a pending or approved request for this driver. */
  isRequested(driver: Driver): boolean {
    return driver.driverId !== undefined && this.requestedDrivers.has(driver.driverId);
  }

  /** True once the admin has approved the customer's request for this driver (the trip may also be over, not yet closed). */
  isApproved(driver: Driver): boolean {
    const status = driver.driverId !== undefined ? this.requestedDrivers.get(driver.driverId) : undefined;
    return status === 'Approved' || status === 'Trip End';
  }

  canRequest(driver: Driver): boolean {
    return driver.availabilityStatus === 'Active' && !this.isRequested(driver);
  }

  requestDriver(driver: Driver): void {
    if (this.canRequest(driver)) {
      this.router.navigate(['/customer-request', driver.driverId]);
    }
  }

  runAiSearch(): void {
    const query = this.aiQuery.trim();
    if (!query) {
      return;
    }
    this.aiLoading = true;
    this.aiMessage = '';
    this.aiService.searchDrivers(query).subscribe({
      next: (results) => {
        this.aiResults = results || [];
        this.aiActive = true;
        this.aiLoading = false;
      },
      error: () => {
        // AI search is optional: fall back to the normal list.
        this.aiActive = false;
        this.aiLoading = false;
        this.aiMessage = 'AI search is currently unavailable. Showing all drivers.';
      }
    });
  }

  clearAiSearch(): void {
    this.aiQuery = '';
    this.aiActive = false;
    this.aiResults = [];
    this.aiMessage = '';
  }
}
