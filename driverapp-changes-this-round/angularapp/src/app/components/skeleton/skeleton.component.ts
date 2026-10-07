import { Component, Input } from '@angular/core';

/**
 * Skeleton loader: grey shimmering placeholders shown while a page waits for the server, so the layout
 * does not jump when the real content arrives.
 *
 *   <app-skeleton variant="card" [count]="3"></app-skeleton>      driver / request cards
 *   <app-skeleton variant="feedback" [count]="2"></app-skeleton>  feedback cards
 *   <app-skeleton variant="lines" [count]="4"></app-skeleton>     paragraph lines (e.g. an AI summary)
 *
 * Table pages use the same look without this component: <tr class="sk-row"> with <span class="sk sk-line">
 * in every cell (the .sk classes are in styles.css).
 */
@Component({
  selector: 'app-skeleton',
  templateUrl: './skeleton.component.html',
  styleUrls: ['./skeleton.component.css']
})
export class SkeletonComponent {
  @Input() variant: 'card' | 'feedback' | 'lines' = 'card';
  @Input() count: number = 3;
  /** How many cards fit next to each other (the request cards are wider than the driver cards). */
  @Input() columns: 2 | 3 = 3;
  /** Text read out by screen readers while loading. */
  @Input() label: string = 'Loading...';

  get items(): number[] {
    const total = Math.max(1, Math.min(Number(this.count) || 1, 12));
    return Array.from({ length: total }, (_, index) => index);
  }
}
