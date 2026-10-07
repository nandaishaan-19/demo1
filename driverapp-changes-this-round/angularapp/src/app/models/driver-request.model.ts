import { Driver } from './driver.model';
import { User } from './user.model';

export interface DriverRequest {
  driverRequestId?: number;
  userId: number;
  driverId?: number;
  requestDate: Date; // ISO Date format (YYYY-MM-DD)
  status: string; // "Pending", "Approved", "Rejected", "Cancelled", "Trip End", "Closed"
  tripDate: Date; // ISO Date format (YYYY-MM-DD)
  timeSlot: Date; // Time as a Date object (compatible with LocalTime)
  pickupLocation: string;
  dropLocation: string;
  estimatedDuration: string; // e.g., "3 hours"
  paymentAmount?: number;
  comments?: string;
  actualDropTime?: Date;
  actualDuration?: string;
  actualDropDate?: Date;

  // The backend sends / expects the related entities nested ({ user: {...}, driver: {...} }).
  user?: User;
  driver?: Driver;
}
