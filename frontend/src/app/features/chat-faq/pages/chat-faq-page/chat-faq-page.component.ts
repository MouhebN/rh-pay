import { DatePipe } from '@angular/common';
import { Component, ElementRef, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NotificationService } from '../../../../core/services/notification.service';
import { ConfirmDialogComponent } from '../../../../shared/components/confirm-dialog/confirm-dialog.component';
import { AppIconComponent } from '../../../../shared/components/app-icon/app-icon.component';
import { IconActionButtonComponent } from '../../../../shared/components/icon-action-button/icon-action-button.component';
import { LoadingSpinnerComponent } from '../../../../shared/components/loading-spinner/loading-spinner.component';
import { ChatFaqDocument, FaqCategory } from '../../models/chat-faq-document.model';
import { ChatFaqDocumentService } from '../../services/chat-faq-document.service';

@Component({
  selector: 'app-chat-faq-page',
  imports: [
    DatePipe,
    FormsModule,
    LoadingSpinnerComponent,
    ConfirmDialogComponent,
    AppIconComponent,
    IconActionButtonComponent
  ],
  templateUrl: './chat-faq-page.component.html',
  styleUrl: './chat-faq-page.component.scss'
})
export class ChatFaqPageComponent {
  private readonly faqService = inject(ChatFaqDocumentService);
  private readonly notificationService = inject(NotificationService);

  readonly documents = signal<ChatFaqDocument[]>([]);
  readonly loading = signal(true);
  readonly uploading = signal(false);
  readonly documentToDelete = signal<ChatFaqDocument | null>(null);

  @ViewChild('fileInput') fileInput?: ElementRef<HTMLInputElement>;

  title = '';
  category: FaqCategory = 'GENERAL';
  selectedFile: File | null = null;

  constructor() {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.faqService.findAll().subscribe({
      next: (response) => {
        this.documents.set(response.data ?? []);
        this.loading.set(false);
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de charger le guide assistant');
        this.loading.set(false);
      }
    });
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  upload(): void {
    if (!this.selectedFile) {
      this.notificationService.warning('Choisissez un fichier md, txt ou pdf.');
      return;
    }
    this.uploading.set(true);
    this.faqService.upload(this.title.trim(), this.category, this.selectedFile).subscribe({
      next: () => {
        this.notificationService.success('Document ajouté au guide de l’assistant');
        this.title = '';
        this.category = 'GENERAL';
        this.selectedFile = null;
        if (this.fileInput) {
          this.fileInput.nativeElement.value = '';
        }
        this.uploading.set(false);
        this.load();
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible d’importer le document');
        this.uploading.set(false);
      }
    });
  }

  confirmDelete(document: ChatFaqDocument): void {
    this.documentToDelete.set(document);
  }

  deleteDocument(): void {
    const document = this.documentToDelete();
    if (!document) {
      return;
    }
    this.faqService.delete(document.id).subscribe({
      next: () => {
        this.notificationService.success('Document retiré du guide');
        this.documentToDelete.set(null);
        this.load();
      },
      error: (error) => {
        this.notificationService.errorFromHttp(error, 'Impossible de supprimer le document');
      }
    });
  }

  categoryLabel(category: FaqCategory): string {
    switch (category) {
      case 'LEAVE':
        return 'Congés';
      case 'PAYROLL':
        return 'Paie';
      case 'ATTENDANCE':
        return 'Pointage';
      default:
        return 'Général';
    }
  }
}
