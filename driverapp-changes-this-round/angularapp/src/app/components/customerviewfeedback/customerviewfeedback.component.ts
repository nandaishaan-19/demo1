import { Component, OnInit } from '@angular/core';
import { FeedbackService } from '../../services/feedback.service';
import { AuthService } from '../../services/auth.service';
import { Feedback } from '../../models/feedback.model';
import { Driver } from '../../models/driver.model';
import { toDisplayDate } from '../../utils/format';
import { getDriverImage, useDefaultAvatar } from '../../utils/driver-image';

@Component({
  selector: 'app-customerviewfeedback',
  templateUrl: './customerviewfeedback.component.html',
  styleUrls: ['./customerviewfeedback.component.css']
})
export class CustomerviewfeedbackComponent implements OnInit {
  feedbacks: Feedback[] = [];
  /** True until the feedback has arrived (shows the skeleton cards). */
  loading: boolean = true;

  showDriverModal: boolean = false;
  selectedDriver: Driver | null = null;

  showDeleteModal: boolean = false;
  feedbackToDelete: Feedback | null = null;

  toDisplayDate = toDisplayDate;
  getDriverImage = getDriverImage;
  useDefaultAvatar = useDefaultAvatar;

  constructor(private feedbackService: FeedbackService, private authService: AuthService) {}

  ngOnInit(): void {
    this.loadFeedbacks();
  }

  loadFeedbacks(): void {
    const userId = this.authService.getUserId();
    if (userId === null) {
      this.loading = false;
      return;
    }
    this.feedbackService.getAllFeedbacksByUserId(userId).subscribe({
      // The API answers 404 / 204 when the customer has not posted any feedback yet.
      next: (feedbacks) => {
        this.feedbacks = feedbacks || [];
        this.loading = false;
      },
      error: () => {
        this.feedbacks = [];
        this.loading = false;
      }
    });
  }

  starsOf(rating: number): string {
    const value = Math.max(0, Math.min(5, Math.round(Number(rating) || 0)));
    return '★'.repeat(value) + '☆'.repeat(5 - value);
  }

  tagsOf(feedback: Feedback): string[] {
    return (feedback.aiTags || '')
      .split(',')
      .map((tag) => tag.trim())
      .filter((tag) => !!tag);
  }

  sentimentClass(sentiment?: string): string {
    return (sentiment || 'none').toLowerCase();
  }

  viewDriverInfo(feedback: Feedback): void {
    this.selectedDriver = feedback.driver || null;
    this.showDriverModal = true;
  }

  closeDriverModal(): void {
    this.showDriverModal = false;
    this.selectedDriver = null;
  }

  askDelete(feedback: Feedback): void {
    this.feedbackToDelete = feedback;
    this.showDeleteModal = true;
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.feedbackToDelete = null;
  }

  confirmDelete(): void {
    const id = this.feedbackToDelete?.feedbackId;
    if (id === undefined) {
      return;
    }
    this.feedbackService.deleteFeedback(id).subscribe({
      next: () => {
        this.feedbacks = this.feedbacks.filter((feedback) => feedback.feedbackId !== id);
        this.cancelDelete();
      },
      error: () => this.cancelDelete()
    });
  }
}
