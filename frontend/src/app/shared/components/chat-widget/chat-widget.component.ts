import { Component, ElementRef, HostListener, OnInit, ViewChild, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { ChatService } from '../../../features/chat/services/chat.service';
import { ChatProposedAction, ChatThreadMessage } from '../../../features/chat/models/chat.model';
import { TokenService } from '../../../core/services/token.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-chat-widget',
  templateUrl: './chat-widget.component.html',
  styleUrl: './chat-widget.component.scss'
})
export class ChatWidgetComponent implements OnInit {
  private readonly chatService = inject(ChatService);
  private readonly tokenService = inject(TokenService);
  private readonly notificationService = inject(NotificationService);
  private readonly router = inject(Router);

  private static readonly FALLBACK_GREETING =
    'Bonjour, comment puis-je vous aider ?';

  @ViewChild('thread') private thread?: ElementRef<HTMLDivElement>;

  readonly open = signal(false);
  readonly minimized = signal(false);
  readonly width = signal(400);
  readonly height = signal(540);
  readonly loading = signal(false);
  readonly draft = signal('');
  readonly messages = signal<ChatThreadMessage[]>([]);
  readonly draftOpen = signal(false);
  private greetingLoaded = false;
  private resizeState: {
    pointerId: number;
    startX: number;
    startY: number;
    startW: number;
    startH: number;
  } | null = null;

  ngOnInit(): void {
    this.loadGreetingOnce();
  }

  suggestions(): string[] {
    return this.tokenService.currentUser()?.role === 'ADMIN_RH'
      ? [
          'Combien de congés en attente ?',
          'Accepter tous',
          'Quels contrats expirent bientôt ?',
          'Qui est en congé cette semaine ?'
        ]
      : [
          'Je veux un congé',
          'Annuler ma demande en attente',
          'Combien de jours de congé me reste-t-il ?',
          'Mon calendrier des absences'
        ];
  }

  toggle(): void {
    if (this.open() && !this.minimized()) {
      this.minimize();
      return;
    }
    this.restore();
  }

  restore(): void {
    this.open.set(true);
    this.minimized.set(false);
    this.loadGreetingOnce();
    this.scrollToEnd();
  }

  minimize(): void {
    this.stopResize();
    this.open.set(true);
    this.minimized.set(true);
  }

  close(): void {
    this.stopResize();
    this.open.set(false);
    this.minimized.set(false);
  }

  startResize(event: PointerEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.resizeState = {
      pointerId: event.pointerId,
      startX: event.clientX,
      startY: event.clientY,
      startW: this.width(),
      startH: this.height()
    };
    document.body.style.userSelect = 'none';
  }

  @HostListener('window:pointermove', ['$event'])
  onPointerMove(event: PointerEvent): void {
    const state = this.resizeState;
    if (!state || event.pointerId !== state.pointerId) {
      return;
    }
    const maxWidth = Math.max(320, window.innerWidth - 24);
    const maxHeight = Math.max(360, window.innerHeight - 24);
    this.width.set(Math.min(maxWidth, Math.max(320, state.startW + (state.startX - event.clientX))));
    this.height.set(Math.min(maxHeight, Math.max(380, state.startH + (state.startY - event.clientY))));
  }

  @HostListener('window:pointerup')
  @HostListener('window:pointercancel')
  stopResize(): void {
    this.resizeState = null;
    document.body.style.userSelect = '';
  }

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.open() && !this.minimized()) {
      this.minimize();
    }
  }

  onDraft(event: Event): void {
    this.draft.set((event.target as HTMLInputElement).value);
  }

  useSuggestion(text: string): void {
    this.draft.set(text);
    this.send();
  }

  send(): void {
    const message = this.draft().trim();
    if (!message || this.loading()) {
      return;
    }

    this.messages.update((current) => [...current, { role: 'user', text: message }]);
    this.draft.set('');
    this.loading.set(true);
    this.scrollToEnd();

    this.chatService.sendMessage(message).subscribe({
      next: (response) => {
        const data = response.data;
        this.messages.update((current) => [
          ...current.map((item) =>
            data.proposedAction && item.proposedAction && !item.actionResolved
              ? { ...item, actionStale: true }
              : item
          ),
          {
            role: 'assistant',
            text: data.reply,
            link: data.link,
            linkLabel: data.linkLabel,
            intent: data.intent,
            sourceKind: data.sourceKind,
            sourceLabel: data.sourceLabel,
            proposedAction: data.proposedAction,
            actionResolved: false,
            actionStale: false
          }
        ]);
        this.draftOpen.set(!!data.draftOpen);
        this.loading.set(false);
        this.scrollToEnd();
      },
      error: (error) => {
        this.loading.set(false);
        this.messages.update((current) => [
          ...current,
          {
            role: 'assistant',
            text: 'Je n’ai pas pu répondre pour le moment. Réessayez dans un instant.'
          }
        ]);
        this.notificationService.errorFromHttp(error, 'Impossible de contacter l’assistant RH.');
        this.scrollToEnd();
      }
    });
  }

  confirmAction(index: number, action: ChatProposedAction): void {
    const message = this.messages()[index];
    if (this.loading() || message?.actionStale || !action.proposalId) {
      return;
    }
    this.loading.set(true);

    const request$ =
      action.type === 'REVIEW_LEAVE_BATCH'
        ? this.chatService.confirmLeaveReview({
            approveLeaveRequestIds: action.approveLeaveRequestIds || [],
            rejectLeaveRequestIds: action.rejectLeaveRequestIds || [],
            reason: action.reason,
            proposalId: action.proposalId
          })
        : action.type === 'CANCEL_LEAVE_REQUEST' && action.leaveRequestId != null
          ? this.chatService.confirmCancelLeaveRequest({
              leaveRequestId: action.leaveRequestId,
              proposalId: action.proposalId
            })
          : this.chatService.confirmLeaveRequest({
              type: action.leaveType || 'PAID_LEAVE',
              startDate: action.startDate || '',
              endDate: action.endDate || '',
              reason: action.reason || 'Demande via assistant RH',
              proposalId: action.proposalId
            });

    request$.subscribe({
      next: (response) => {
        const data = response.data;
        this.draftOpen.set(false);
        this.messages.update((current) => {
          const next = [...current];
          const target = next[index];
          if (target) {
            next[index] = { ...target, actionResolved: true, proposedAction: null };
          }
          next.push({
            role: 'assistant',
            text: data.reply,
            link: data.link,
            linkLabel: data.linkLabel,
            intent: data.intent,
            sourceKind: data.sourceKind,
            sourceLabel: data.sourceLabel
          });
          return next;
        });
        this.loading.set(false);
        this.notificationService.success(
          action.type === 'REVIEW_LEAVE_BATCH'
            ? 'Revue des congés appliquée'
            : action.type === 'CANCEL_LEAVE_REQUEST'
              ? 'Demande annulée'
              : 'Demande de congé créée'
        );
        this.scrollToEnd();
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.errorFromHttp(error, 'Impossible de confirmer l’action');
        const reason = this.httpErrorMessage(
          error,
          'La confirmation a échoué. Vérifiez les dates / le solde, ou utilisez le menu Congés.'
        );
        this.messages.update((current) => [
          ...current,
          {
            role: 'assistant',
            text: reason
          }
        ]);
        this.scrollToEnd();
      }
    });
  }

  discardOpenDraft(): void {
    if (this.loading()) {
      return;
    }
    this.loading.set(true);
    this.chatService.discardLeaveAction().subscribe({
      next: (response) => {
        this.draftOpen.set(false);
        this.loading.set(false);
        this.messages.update((current) => [
          ...current.map((item) =>
            item.proposedAction && !item.actionResolved
              ? { ...item, actionResolved: true, proposedAction: null }
              : item
          ),
          {
            role: 'assistant' as const,
            text: response.data?.reply || 'Brouillon annulé. Aucune modification n’a été enregistrée.'
          }
        ]);
        this.scrollToEnd();
      },
      error: (error) => {
        this.loading.set(false);
        this.notificationService.errorFromHttp(error, 'Impossible d’annuler le brouillon');
      }
    });
  }

  cancelAction(index: number): void {
    if (this.loading()) {
      return;
    }
    this.loading.set(true);
    this.chatService.discardLeaveAction().subscribe({
      next: (response) => {
        const data = response.data;
        this.draftOpen.set(false);
        this.messages.update((current) => {
          const next = [...current];
          const target = next[index];
          if (target) {
            next[index] = { ...target, actionResolved: true, proposedAction: null };
          }
          next.push({
            role: 'assistant',
            text: data.reply || 'Action annulée. Aucune modification n’a été enregistrée.'
          });
          return next;
        });
        this.loading.set(false);
        this.scrollToEnd();
      },
      error: () => {
        this.messages.update((current) => {
          const next = [...current];
          const target = next[index];
          if (target) {
            next[index] = { ...target, actionResolved: true, proposedAction: null };
          }
          next.push({
            role: 'assistant',
            text: 'Demande annulée. Aucune modification n’a été enregistrée.'
          });
          return next;
        });
        this.loading.set(false);
        this.scrollToEnd();
      }
    });
  }

  openLink(link: string): void {
    this.close();
    void this.router.navigateByUrl(link);
  }

  private scrollToEnd(): void {
    queueMicrotask(() => {
      requestAnimationFrame(() => {
        const element = this.thread?.nativeElement;
        if (!element) {
          return;
        }
        const current = element.querySelector('.action-row:not(.stale)');
        if (current instanceof HTMLElement) {
          current.scrollIntoView({ block: 'nearest' });
        }
        element.scrollTop = element.scrollHeight;
      });
    });
  }

  /**
   * Load proactive greeting once per widget component lifetime.
   * Does not re-inject on minimize/restore, and never clears an existing thread.
   */
  private loadGreetingOnce(): void {
    if (this.greetingLoaded) {
      return;
    }
    this.greetingLoaded = true;
    this.chatService.getGreeting().subscribe({
      next: (response) => {
        const text = response.data?.message?.trim() || ChatWidgetComponent.FALLBACK_GREETING;
        this.draftOpen.set(!!response.data?.draftOpen);
        if (this.messages().length === 0) {
          this.messages.set([{ role: 'assistant', text }]);
          this.scrollToEnd();
        }
      },
      error: () => {
        if (this.messages().length === 0) {
          this.messages.set([{ role: 'assistant', text: ChatWidgetComponent.FALLBACK_GREETING }]);
          this.scrollToEnd();
        }
      }
    });
  }

  private httpErrorMessage(error: unknown, fallback: string): string {
    if (!(error instanceof HttpErrorResponse)) {
      return fallback;
    }
    const body = error.error;
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    if (body?.message && typeof body.message === 'string' && body.message.trim()) {
      return body.message;
    }
    if (body?.error && typeof body.error === 'string' && body.error.trim()) {
      return body.error;
    }
    if (body?.detail && typeof body.detail === 'string' && body.detail.trim()) {
      return body.detail;
    }
    return fallback;
  }
}
