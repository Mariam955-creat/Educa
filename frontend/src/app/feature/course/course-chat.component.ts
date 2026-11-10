import { Component, ElementRef, OnInit, computed, inject, input, signal, viewChild } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

import { AiApiService, ChatTurn } from '../../core/ai/ai-api.service';

/** Assistant du cours : la conversation est enregistrée côté serveur et retrouvée à chaque visite. */
@Component({
  selector: 'app-course-chat',
  imports: [ReactiveFormsModule, TranslatePipe],
  templateUrl: './course-chat.component.html',
  styleUrl: './course-chat.component.scss',
})
export class CourseChatComponent implements OnInit {
  readonly courseId = input.required<number>();

  private readonly api = inject(AiApiService);
  private readonly translate = inject(TranslateService);
  private readonly messagesBox = viewChild<ElementRef<HTMLElement>>('messages');

  readonly turns = signal<ChatTurn[]>([]);
  readonly pending = signal(false);
  readonly loadingHistory = signal(true);
  readonly confirmingClear = signal(false);
  readonly input = new FormControl('', { nonNullable: true });

  readonly canSend = computed(() => !this.pending());

  ngOnInit(): void {
    // L'historique est un confort : s'il ne se charge pas, le chat reste utilisable
    this.api.history(this.courseId()).subscribe({
      next: (messages) => {
        this.turns.set(messages.map(({ role, content, degraded }) => ({ role, content, degraded })));
        this.loadingHistory.set(false);
        this.scrollToBottom();
      },
      error: () => this.loadingHistory.set(false),
    });
  }

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
    this.scrollToBottom();

    this.api.chat(this.courseId(), message).subscribe({
      next: (res) => {
        this.turns.update((t) => [...t, { role: 'assistant', content: res.reply, degraded: res.degraded }]);
        this.pending.set(false);
        this.scrollToBottom();
      },
      error: () => {
        this.turns.update((t) => [
          ...t,
          { role: 'assistant', content: this.translate.instant('chat.unavailable'), degraded: true },
        ]);
        this.pending.set(false);
        this.scrollToBottom();
      },
    });
  }

  clear(): void {
    this.api.clearHistory(this.courseId()).subscribe({
      next: () => {
        this.turns.set([]);
        this.confirmingClear.set(false);
      },
      error: () => this.confirmingClear.set(false),
    });
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      const box = this.messagesBox()?.nativeElement;
      if (box) box.scrollTop = box.scrollHeight;
    });
  }
}
