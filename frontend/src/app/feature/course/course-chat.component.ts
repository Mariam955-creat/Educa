import { Component, computed, inject, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { AiApiService, ChatTurn } from '../../core/ai/ai-api.service';

@Component({
  selector: 'app-course-chat',
  imports: [ReactiveFormsModule],
  templateUrl: './course-chat.component.html',
  styleUrl: './course-chat.component.scss',
})
export class CourseChatComponent {
  readonly courseId = input.required<number>();

  private readonly api = inject(AiApiService);

  readonly turns = signal<ChatTurn[]>([]);
  readonly pending = signal(false);
  readonly input = new FormControl('', { nonNullable: true });

  readonly canSend = computed(() => !this.pending());

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
          { role: 'assistant', content: "L'assistant est indisponible pour le moment." },
        ]);
        this.pending.set(false);
      },
    });
  }
}
