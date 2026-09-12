import { Component, computed, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AiApiService, ChatTurn } from '../../core/ai/ai-api.service';

@Component({
  selector: 'app-course-chat',
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './course-chat.component.html',
  styleUrl: './course-chat.component.scss',
})
export class CourseChatComponent {
  readonly courseId = input.required<number>();

  private readonly api = inject(AiApiService);
  private readonly translate = inject(TranslateService);

  readonly turns = signal<ChatTurn[]>([]);
  readonly pending = signal(false);
  readonly input = new FormControl('', { nonNullable: true });

  readonly canSend = computed(() => !this.pending());

  onSubmit(event: Event): void {
    // `(ngSubmit)` needs the `NgForm` directive (from `FormsModule`), qui n'est pas
    // importé ici (on utilise juste `[formControl]`, sans `FormGroup`) : sans ce
    // `preventDefault()`, le clic déclenche une vraie soumission HTML native, qui
    // recharge la page (retour au début du cours).
    event.preventDefault();
    this.send();
  }

  send(): void {
    const message = this.input.value.trim();
    if (!message || this.pending()) return;

    this.turns.update((t) => [...t, { role: 'user', content: message }]);
    this.input.setValue('');
    this.pending.set(true);

    const history = this.turns().slice(-7, -1); // les échanges précédents (hors message courant)
    this.api.chat(this.courseId(), message, history).subscribe({
      next: (res) => {
        this.turns.update((t) => [...t, { role: 'assistant', content: res.reply }]);
        this.pending.set(false);
      },
      error: () => {
        this.turns.update((t) => [
          ...t,
          { role: 'assistant', content: this.translate.instant('chat.unavailable') },
        ]);
        this.pending.set(false);
      },
    });
  }
}
