import { UpperCasePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { debounceTime, distinctUntilChanged } from 'rxjs';

import { CourseApiService } from '../../core/courses/course-api.service';
import { CourseSummary } from '../../core/courses/course.models';

@Component({
  selector: 'app-catalog',
  imports: [ReactiveFormsModule, RouterLink, UpperCasePipe],
  templateUrl: './catalog.component.html',
  styleUrl: './catalog.component.scss',
})
export class CatalogComponent implements OnInit {
  private readonly api = inject(CourseApiService);

  readonly search = new FormControl('', { nonNullable: true });
  readonly courses = signal<CourseSummary[]>([]);
  readonly loading = signal(true);

  ngOnInit(): void {
    this.load('');
    this.search.valueChanges
      .pipe(debounceTime(250), distinctUntilChanged())
      .subscribe((value) => this.load(value));
  }

  private load(q: string): void {
    this.loading.set(true);
    this.api.catalog(q || undefined).subscribe({
      next: (page) => {
        this.courses.set(page.content);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
