import { combineDateTime, toDisplayDate, toTimeString } from './format';

/** A trip can be cancelled only until this many hours before it starts. */
export const CANCEL_NOTICE_HOURS = 24;

/** What a request is "doing" right now: the request status refined with the clock. */
export type TripPhase = 'Pending' | 'Upcoming' | 'Ongoing' | 'Trip End' | 'Completed' | 'Rejected' | 'Cancelled';

/** The entries of the admin's "Filter by" drop-down (value = the TripPhase, or 'All'). */
export const TRIP_FILTERS: { value: 'All' | TripPhase; label: string }[] = [
  { value: 'All', label: 'All Requests' },
  { value: 'Pending', label: 'Awaiting Approval' },
  { value: 'Upcoming', label: 'Upcoming Trips (approved, not started)' },
  { value: 'Ongoing', label: 'Ongoing Trips (started, not ended)' },
  { value: 'Trip End', label: 'Trip Ended (waiting to be closed)' },
  { value: 'Completed', label: 'Completed Trips (closed)' },
  { value: 'Rejected', label: 'Rejected' },
  { value: 'Cancelled', label: 'Cancelled by Customer' }
];

interface TripTimes {
  status: string;
  tripDate: any;
  timeSlot: any;
}

/** The moment the trip starts (trip date + time slot, local time), or null when it is not known. */
export function tripStart(request: TripTimes): Date | null {
  return combineDateTime(request.tripDate, request.timeSlot);
}

/** True once the trip's start time has been reached. A trip without a known start time is not blocked. */
export function hasTripStarted(request: TripTimes, now: Date = new Date()): boolean {
  const start = tripStart(request);
  return !start || now.getTime() >= start.getTime();
}

/** The last moment a trip can be cancelled (24 hours before the start), or null when the start is unknown. */
export function cancelDeadline(request: TripTimes): Date | null {
  const start = tripStart(request);
  return start ? new Date(start.getTime() - CANCEL_NOTICE_HOURS * 60 * 60 * 1000) : null;
}

/** An approved trip can be cancelled until 24 hours before it starts. */
export function canCancelTrip(request: TripTimes, now: Date = new Date()): boolean {
  if (request.status !== 'Approved') {
    return false;
  }
  const deadline = cancelDeadline(request);
  return !deadline || now.getTime() <= deadline.getTime();
}

/** A trip can be ended only when it is approved and its start time has been reached. */
export function canEndTripNow(request: TripTimes, now: Date = new Date()): boolean {
  return request.status === 'Approved' && hasTripStarted(request, now);
}

export function tripPhase(request: TripTimes, now: Date = new Date()): TripPhase {
  switch (request.status) {
    case 'Approved':
      return hasTripStarted(request, now) ? 'Ongoing' : 'Upcoming';
    case 'Closed':
      return 'Completed';
    case 'Pending':
    case 'Rejected':
    case 'Cancelled':
    case 'Trip End':
      return request.status;
    default:
      return 'Pending';
  }
}

/** "09/10/2026 at 10:30" */
export function formatTripStart(request: TripTimes): string {
  const start = tripStart(request);
  return start ? `${toDisplayDate(start)} at ${toTimeString(start)}` : '';
}

/** One line telling the customer what an approved trip allows right now ('' when nothing needs saying). */
export function tripHint(request: TripTimes, now: Date = new Date()): string {
  if (request.status !== 'Approved') {
    return '';
  }
  const start = formatTripStart(request);
  const deadline = cancelDeadline(request);
  if (!start || !deadline) {
    return '';
  }
  if (hasTripStarted(request, now)) {
    return '';
  }
  if (canCancelTrip(request, now)) {
    return `Trip starts ${start}. You can cancel it until ${toDisplayDate(deadline)} at ${toTimeString(deadline)}. Trip End opens when the trip starts.`;
  }
  return `Trip starts ${start}. Cancelling is closed (less than ${CANCEL_NOTICE_HOURS} hours to go). Trip End opens when the trip starts.`;
}
