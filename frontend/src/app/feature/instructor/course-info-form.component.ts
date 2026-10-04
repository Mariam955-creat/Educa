import { Component, OnDestroy, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormArray, FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '@ngx-translate/core';

import {
  COURSE_CATEGORIES,
  COURSE_LEVELS,
  CourseCategory,
  CourseDetail,
  CourseFormValue,
  CourseLevel,
} from '../../core/courses/course.models';
import { CourseLanguage } from '../../core/language/language-api.service';

const MAX_LIST_ITEMS = 12;

export interface CourseFormSubmit {
  value: CourseFormValue;
  /** Image de couverture choisie à la création (téléversée une fois le cours créé). */
  cover: File | null;
}

/**
 * Formulaire de présentation d'un cours, en sections : informations générales, ce que les apprenants vont
 * acquérir, prix et évaluation, et (à la création) image de couverture.
 */
@Component({
  selector: 'app-course-info-form',
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './course-info-form.component.html',
  styleUrl: './course-info-form.component.scss',
})
export class CourseInfoFormComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);

  /** Cours édité, `null` à la création. */
  readonly course = input<CourseDetail | null>(null);
  readonly languages = input<CourseLanguage[]>([]);
  readonly saving = input(false);
  readonly submitted = output<CourseFormSubmit>();

  readonly categories = COURSE_CATEGORIES;
  readonly levels = COURSE_LEVELS;
  readonly maxListItems = MAX_LIST_ITEMS;

  readonly form = this.fb.group({
    title: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(200)]),
    subtitle: this.fb.nonNullable.control('', [Validators.maxLength(200)]),
    description: this.fb.nonNullable.control(''),
    category: this.fb.control<CourseCategory | null>(null),
    level: this.fb.control<CourseLevel | null>(null),
    language: this.fb.nonNullable.control('fr', [Validators.required]),
    objectives: this.fb.array<FormControl<string>>([]),
    prerequisites: this.fb.array<FormControl<string>>([]),
    targetAudience: this.fb.nonNullable.control('', [Validators.maxLength(2000)]),
    durationHours: this.fb.control<number | null>(null, [Validators.min(0), Validators.max(999)]),
    // Pas de prix par défaut : le formateur doit le saisir explicitement (0 = gratuit reste possible).
    price: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    passThreshold: this.fb.nonNullable.control(70, [Validators.required, Validators.min(0), Validators.max(100)]),
    controlWeight: this.fb.nonNullable.control(40, [Validators.required, Validators.min(0), Validators.max(100)]),
  });

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  readonly titleLength = computed(() => this.formValue().title?.length ?? 0);
  readonly subtitleLength = computed(() => this.formValue().subtitle?.length ?? 0);
  readonly controlWeight = computed(() => this.formValue().controlWeight ?? 40);

  /** Couverture choisie avant la création du cours (aperçu local). */
  readonly coverFile = signal<File | null>(null);
  readonly coverPreview = signal<string | null>(null);

  constructor() {
    // Le cours chargé (édition) remplit le formulaire ; à la création, une ligne vide invite à saisir
    effect(() => this.fill(this.course()));
  }

  get objectives(): FormArray<FormControl<string>> {
    return this.form.controls.objectives;
  }

  get prerequisites(): FormArray<FormControl<string>> {
    return this.form.controls.prerequisites;
  }

  addItem(list: FormArray<FormControl<string>>): void {
    if (list.length < MAX_LIST_ITEMS) {
      list.push(this.fb.nonNullable.control('', [Validators.maxLength(200)]));
    }
  }

  removeItem(list: FormArray<FormControl<string>>, index: number): void {
    list.removeAt(index);
    if (list.length === 0) this.addItem(list);
  }

  onCoverPicked(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.setCover(file);
  }

  clearCover(): void {
    this.setCover(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const controlWeight = raw.controlWeight;
    this.submitted.emit({
      value: {
        title: raw.title.trim(),
        subtitle: raw.subtitle.trim() || undefined,
        description: raw.description,
        category: raw.category ?? undefined,
        level: raw.level ?? undefined,
        language: raw.language,
        objectives: raw.objectives.map((o) => o.trim()).filter(Boolean),
        prerequisites: raw.prerequisites.map((p) => p.trim()).filter(Boolean),
        targetAudience: raw.targetAudience.trim() || undefined,
        durationHours: raw.durationHours ?? undefined,
        price: raw.price ?? 0, // non null ici : le formulaire est valide
        passThreshold: raw.passThreshold,
        controlWeight,
        examWeight: 100 - controlWeight,
      },
      cover: this.coverFile(),
    });
  }

  ngOnDestroy(): void {
    this.setCover(null);
  }

  private fill(course: CourseDetail | null): void {
    this.form.patchValue({
      title: course?.title ?? '',
      subtitle: course?.subtitle ?? '',
      description: course?.description ?? '',
      category: course?.category ?? null,
      level: course?.level ?? null,
      language: course?.language ?? 'fr',
      targetAudience: course?.targetAudience ?? '',
      durationHours: course?.durationHours ?? null,
      price: course ? course.price : null,
      passThreshold: course?.passThreshold ?? 70,
      controlWeight: course?.controlWeight ?? 40,
    });
    this.fillList(this.objectives, course?.objectives ?? []);
    this.fillList(this.prerequisites, course?.prerequisites ?? []);
    this.form.markAsPristine();
  }

  private fillList(list: FormArray<FormControl<string>>, items: string[]): void {
    list.clear();
    for (const item of items) list.push(this.fb.nonNullable.control(item, [Validators.maxLength(200)]));
    if (list.length === 0) this.addItem(list);
  }

  private setCover(file: File | null): void {
    const previous = this.coverPreview();
    if (previous) URL.revokeObjectURL(previous);
    this.coverFile.set(file);
    this.coverPreview.set(file ? URL.createObjectURL(file) : null);
  }
}
