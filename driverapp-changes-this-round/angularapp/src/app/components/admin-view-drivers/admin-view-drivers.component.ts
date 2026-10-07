import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { DriverService } from '../../services/driver.service';
import { Driver } from '../../models/driver.model';
import { DRIVER_STATUSES } from '../../utils/constants';
import { getDriverImage, useDefaultAvatar } from '../../utils/driver-image';

@Component({
  selector: 'app-admin-view-drivers',
  templateUrl: './admin-view-drivers.component.html',
  styleUrls: ['./admin-view-drivers.component.css']
})
export class AdminViewDriversComponent implements OnInit {
  drivers: Driver[] = [];
  /** True until the drivers have arrived (shows the skeleton cards). */
  loading: boolean = true;
  filteredDrivers: Driver[] = [];
  searchText: string = '';
  statusFilter: string = 'All';
  statuses: string[] = DRIVER_STATUSES;

  openActionDriverId: number | null = null;
  showDeletePopup: boolean = false;
  driverToDelete: Driver | null = null;
  deleteError: string = '';

  getDriverImage = getDriverImage;
  useDefaultAvatar = useDefaultAvatar;

  constructor(private driverService: DriverService, private router: Router) {}

  ngOnInit(): void {
    this.loadDrivers();
  }

  loadDrivers(): void {
    this.driverService.getAllDrivers().subscribe({
      // The API answers 204 (empty body) when there are no drivers.
      next: (drivers) => {
        this.drivers = drivers || [];
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.drivers = [];
        this.applyFilters();
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    const query = this.searchText.trim().toLowerCase();
    this.filteredDrivers = this.drivers.filter((driver) => {
      const matchesStatus = this.statusFilter === 'All' || driver.availabilityStatus === this.statusFilter;
      const matchesText =
        !query ||
        (driver.driverName || '').toLowerCase().includes(query) ||
        (driver.vehicleType || '').toLowerCase().includes(query);
      return matchesStatus && matchesText;
    });
  }

  /** Inactive drivers cannot be edited or deleted (Delete is the last item of the Action menu). */
  isLocked(driver: Driver): boolean {
    return driver.availabilityStatus === 'Inactive';
  }

  editDriver(driver: Driver): void {
    if (this.isLocked(driver)) {
      return;
    }
    this.router.navigate(['/driver-management', driver.driverId]);
  }

  openDeletePopup(driver: Driver): void {
    if (this.isLocked(driver)) {
      return;
    }
    this.openActionDriverId = null; // close the Action menu
    this.driverToDelete = driver;
    this.deleteError = '';
    this.showDeletePopup = true;
  }

  cancelDelete(): void {
    this.showDeletePopup = false;
    this.driverToDelete = null;
    this.deleteError = '';
  }

  confirmDelete(): void {
    if (!this.driverToDelete || this.driverToDelete.driverId === undefined) {
      return;
    }
    this.driverService.deleteDriver(this.driverToDelete.driverId).subscribe({
      next: () => {
        this.cancelDelete();
        this.loadDrivers();
      },
      error: (err) => {
        if (err && err.status === 409) {
          this.deleteError = 'This driver has requests or feedback and cannot be deleted.';
        } else if (err && err.status === 404) {
          this.cancelDelete();
          this.loadDrivers();
        }
      }
    });
  }

  otherStatuses(driver: Driver): string[] {
    return this.statuses.filter((status) => status !== driver.availabilityStatus);
  }

  toggleActionMenu(driver: Driver): void {
    this.openActionDriverId = this.openActionDriverId === driver.driverId ? null : (driver.driverId ?? null);
  }

  changeStatus(driver: Driver, status: string): void {
    if (driver.driverId === undefined) {
      return;
    }
    const updated: Driver = { ...driver, availabilityStatus: status };
    this.driverService.updateDriver(driver.driverId, updated).subscribe({
      next: () => {
        driver.availabilityStatus = status;
        this.openActionDriverId = null;
        this.applyFilters();
      },
      error: () => (this.openActionDriverId = null)
    });
  }
}
