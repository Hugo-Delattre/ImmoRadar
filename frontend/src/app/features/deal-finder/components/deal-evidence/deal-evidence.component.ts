import {
  ChangeDetectionStrategy,
  Component,
  inject,
  input,
  linkedSignal,
  output,
  signal,
} from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { DatePipe } from '@angular/common';
import { rxResource } from '@angular/core/rxjs-interop';
import {
  disabled,
  form,
  FormField,
  maxLength,
  pattern,
  required,
  submit,
  validate,
} from '@angular/forms/signals';
import { DealEvidenceService } from '../../../../core/services/deal-evidence';
import {
  EvidenceCheck,
  EvidenceStatus,
  UpdateEvidenceRequest,
} from '../../../../core/models/evidence.model';

@Component({
  selector: 'app-deal-evidence',
  imports: [DatePipe, FormField],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './deal-evidence.component.html',
  styleUrl: './deal-evidence.component.scss',
})
export class DealEvidenceComponent {
  readonly dealId = input.required<string>();
  readonly changed = output<void>();
  private readonly service = inject(DealEvidenceService);
  protected readonly evidence = rxResource({
    params: () => this.dealId(),
    stream: ({ params }) => this.service.getSummary(params),
  });
  protected readonly editing = linkedSignal({
    source: this.dealId,
    computation: () => null as EvidenceCheck | null,
  });
  protected readonly saving = signal(false);
  protected readonly saveError = linkedSignal({ source: this.dealId, computation: () => '' });
  protected readonly savedMessage = linkedSignal({ source: this.dealId, computation: () => '' });
  protected readonly editModel = linkedSignal<UpdateEvidenceRequest>(() => {
    const check = this.editing();
    return {
      status: check?.status ?? 'UNVERIFIED',
      sourceUrl: check?.sourceUrl ?? '',
      note: check?.note ?? '',
      checkedOn: check?.checkedOn ?? this.today(),
    };
  });
  protected readonly editForm = form(this.editModel, (path) => {
    disabled(path, { when: () => this.saving() });
    maxLength(path.sourceUrl, 2048);
    pattern(path.sourceUrl, /^(|https:\/\/[^\s]+)$/, { message: 'Utilise un lien HTTPS.' });
    required(path.note, {
      when: ({ valueOf }) => valueOf(path.status) !== 'UNVERIFIED',
      message: 'Décris la référence ou ton hypothèse.',
    });
    maxLength(path.note, 1000);
    required(path.checkedOn, { message: 'Indique la date du contrôle.' });
    validate(path.checkedOn, ({ value }) =>
      value() > this.today()
        ? { kind: 'future', message: 'La date du contrôle ne peut pas être dans le futur.' }
        : undefined,
    );
  });
  protected readonly statusLabels: Record<EvidenceStatus, string> = {
    UNVERIFIED: 'À vérifier',
    OBSERVED: 'Observé',
    ESTIMATED: 'Estimé',
    DOCUMENTED: 'Justificatif renseigné',
    NOT_APPLICABLE: 'Hors copropriété',
  };

  protected edit(check: EvidenceCheck): void {
    this.editing.set(check);
    this.editForm().reset();
    this.saveError.set('');
    this.savedMessage.set('');
  }

  protected save(): void {
    void submit(this.editForm, async () => {
      const check = this.editing();
      if (!check || this.saving()) return;
      const dealId = this.dealId();
      this.saving.set(true);
      this.saveError.set('');
      try {
        const result = await this.service.update(dealId, check.field, this.editModel());
        // Selection may change while the request is in flight; never apply it to another property.
        if (this.dealId() === dealId) {
          this.evidence.value.set(result);
          this.changed.emit();
          this.editing.set(null);
          this.savedMessage.set(`${check.label} : contrôle enregistré.`);
        }
      } catch (error) {
        if (this.dealId() === dealId)
          this.saveError.set(
            error instanceof HttpErrorResponse
              ? (error.error?.detail ?? 'Enregistrement impossible. Réessaie.')
              : 'Enregistrement impossible. Réessaie.',
          );
      } finally {
        this.saving.set(false);
      }
    });
  }

  protected formatValue(check: EvidenceCheck): string {
    if (!check.currentValue) return '';
    return check.field === 'SURFACE'
      ? `${check.currentValue} m²`
      : new Intl.NumberFormat('fr-FR', {
          style: 'currency',
          currency: 'EUR',
          maximumFractionDigits: 0,
        }).format(Number(check.currentValue));
  }

  private today(): string {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
