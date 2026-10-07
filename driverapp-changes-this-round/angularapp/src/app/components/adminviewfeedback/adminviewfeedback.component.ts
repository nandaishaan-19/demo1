import { Component, OnInit } from '@angular/core';
import { FeedbackService } from '../../services/feedback.service';
import { AiService } from '../../services/ai.service';
import { Feedback } from '../../models/feedback.model';
import { Driver } from '../../models/driver.model';
import { User } from '../../models/user.model';
import { DriverFeedbackSummary } from '../../models/ai.model';
import { FEEDBACK_CATEGORIES, SENTIMENTS } from '../../utils/constants';
import { toDisplayDate } from '../../utils/format';

@Component({
  selector: 'app-adminviewfeedback',
  templateUrl: './adminviewfeedback.component.html',
  styleUrls: ['./adminviewfeedback.component.css']
})
export class AdminviewfeedbackComponent implements OnInit {
  feedbacks: Feedback[] = [];
  /** True until the feedback has arrived (shows skeleton table rows). */
  loading: boolean = true;
  filteredFeedbacks: Feedback[] = [];
  categories: string[] = FEEDBACK_CATEGORIES;
  sentiments: string[] = SENTIMENTS;
  categoryFilter: string = 'All';
  sentimentFilter: string = 'All';

  showUserModal: boolean = false;
  selectedUser: User | null = null;

  showDriverModal: boolean = false;
  selectedDriver: Driver | null = null;
  summary: DriverFeedbackSummary | null = null;
  summaryLoading: boolean = false;

  toDisplayDate = toDisplayDate;

  constructor(private feedbackService: FeedbackService, private aiService: AiService) {}

  ngOnInit(): void {
    this.loadFeedbacks();
  }

  loadFeedbacks(): void {
    this.feedbackService.getFeedbacks().subscribe({
      // The API answers 204 (empty body) when there is no feedback.
      next: (feedbacks) => {
        this.feedbacks = feedbacks || [];
        this.applyFilters();
        this.loading = false;
      },
      error: () => {
        this.feedbacks = [];
        this.applyFilters();
        this.loading = false;
      }
    });
  }

  applyFilters(): void {
    this.filteredFeedbacks = this.feedbacks.filter(
      (feedback) =>
        (this.categoryFilter === 'All' || feedback.category === this.categoryFilter) &&
        (this.sentimentFilter === 'All' || feedback.sentiment === this.sentimentFilter)
    );
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

  showProfile(feedback: Feedback): void {
    this.selectedUser = feedback.user || null;
    this.showUserModal = true;
  }

  closeUserModal(): void {
    this.showUserModal = false;
    this.selectedUser = null;
  }

  viewDriverInfo(feedback: Feedback): void {
    this.selectedDriver = feedback.driver || null;
    this.summary = null;
    this.showDriverModal = true;
    const driverId = this.selectedDriver?.driverId;
    if (driverId !== undefined) {
      this.summaryLoading = true;
      this.aiService.getDriverFeedbackSummary(driverId).subscribe({
        next: (summary) => {
          this.summary = this.normalise(summary);
          this.summaryLoading = false;
        },
        error: () => {
          // The AI summary is optional: the driver details stay available without it.
          this.summary = null;
          this.summaryLoading = false;
        }
      });
    }
  }

  closeDriverModal(): void {
    this.showDriverModal = false;
    this.selectedDriver = null;
    this.summary = null;
    this.summaryLoading = false;
  }

  private normalise(summary: any): DriverFeedbackSummary {
    const toList = (value: any): string[] =>
      Array.isArray(value)
        ? value
        : String(value || '')
            .split('\n')
            .map((item) => item.trim())
            .filter((item) => !!item);
    return { ...summary, strengths: toList(summary?.strengths), improvements: toList(summary?.improvements) };
  }
}
