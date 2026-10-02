import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { rxResource } from '@angular/core/rxjs-interop';
import {
  disabled,
  form,
  FormField,
  max,
  maxLength,
  min,
  pattern,
  required,
  submit,
  validate,
} from '@angular/forms/signals';
import { Deal, SimulationRequest } from '../../../../core/models/deal.model';
import {
  QualificationResponse,
  RentalMode,
  RentalReferenceRequest,
} from '../../../../core/models/qualification.model';
import { DealQualificationService } from '../../../../core/services/deal-qualification';

@Component({
  selector: 'app-deal-qualification',
  imports: [DatePipe, FormField],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './deal-qualification.component.html',
  styleUrl: './deal-qualification.component.scss',
})
export class DealQualificationComponent {
  readonly deal = input.required<Deal>();
  readonly request = input.required<SimulationRequest>();
  readonly evidenceRevision = input(0);
  private readonly service = inject(DealQualificationService);
  private readonly propertyId = computed(() => this.deal().id);
  protected readonly references = rxResource({
    params: this.propertyId,
    stream: ({ params }) => this.service.references(params),
  });
  private readonly modeContext = computed(() => `${this.propertyId()}:${this.request().taxRegime}`);
  protected readonly modeModel = linkedSignal({
    source: this.modeContext,
    computation: () => ({
      rentalMode: (this.request().taxRegime === 'NU' ? 'UNFURNISHED' : 'FURNISHED') as RentalMode,
    }),
  });
  protected readonly modeForm = form(this.modeModel);
  private readonly referenceRevision = signal(0);
  // Invalidate both completed and in-flight assessments when any dependency changes.
  private readonly fingerprint = computed(() =>
    JSON.stringify({
      deal: this.deal(),
      base: this.request(),
      mode: this.modeModel(),
      evidenceRevision: this.evidenceRevision(),
      references: this.referenceRevision(),
    }),
  );
  protected readonly result = linkedSignal({
    source: this.fingerprint,
    computation: () => null as QualificationResponse | null,
  });
  protected readonly error = linkedSignal({ source: this.fingerprint, computation: () => '' });
  protected readonly busy = signal(false);
  protected readonly saving = signal(false);
  protected readonly editorOpen = linkedSignal({
    source: this.propertyId,
    computation: () => false,
  });
  protected readonly saveError = linkedSignal({ source: this.propertyId, computation: () => '' });
  protected readonly referenceModel = linkedSignal({
    source: this.propertyId,
    computation: (): RentalReferenceRequest => ({
      sourceUrl: '',
      observedOn: this.today(),
      location: this.deal().location,
      propertyType: this.deal().propertyType,
      surface: this.deal().surface,
      monthlyRent: 0,
      rentalMode: this.request().taxRegime === 'NU' ? 'UNFURNISHED' : 'FURNISHED',
      kind: 'ASKING_RENT',
      note: '',
    }),
  });
  protected readonly referenceForm = form(this.referenceModel, (path) => {
    disabled(path, { when: () => this.saving() });
    required(path.sourceUrl);
    maxLength(path.sourceUrl, 2048);
    pattern(path.sourceUrl, /^https:\/\/[^\s]+$/, { message: 'Renseigne une source HTTPS.' });
    required(path.observedOn);
    validate(path.observedOn, ({ value }) =>
      value() > this.today()
        ? { kind: 'future', message: 'La date ne peut pas être future.' }
        : undefined,
    );
    required(path.location);
    maxLength(path.location, 120);
    required(path.surface);
    min(path.surface, 1);
    max(path.surface, 10000);
    required(path.monthlyRent);
    min(path.monthlyRent, 1);
    max(path.monthlyRent, 100000);
    for (const field of [path.surface, path.monthlyRent]) {
      validate(field, ({ value }) =>
        Number.isFinite(value())
          ? undefined
          : { kind: 'finite', message: 'Saisis un nombre valide.' },
      );
    }
    required(path.note);
    maxLength(path.note, 1000);
  });
  protected readonly outcomeLabels = {
    POTENTIAL: 'Bonne affaire potentielle',
    INCOMPLETE: 'Dossier incomplet',
    NOT_QUALIFIED: 'Critères non satisfaits',
  };
  protected readonly stateLabels = {
    PASS: 'Satisfait',
    MISSING: 'À compléter',
    FAIL: 'Non satisfait',
  };

  protected async assess(): Promise<void> {
    if (this.busy() || this.saving()) return;
    const fingerprint = this.fingerprint();
    this.busy.set(true);
    this.error.set('');
    this.result.set(null);
    try {
      const result = await this.service.assess({
        base: this.request(),
        rentalMode: this.modeModel().rentalMode,
      });
      if (fingerprint === this.fingerprint()) this.result.set(result);
    } catch (error) {
      if (fingerprint === this.fingerprint())
        this.error.set(this.message(error, 'Qualification indisponible. Réessaie.'));
    } finally {
      this.busy.set(false);
    }
  }

  protected save(): void {
    void submit(this.referenceForm, async () => {
      if (this.saving()) return;
      const id = this.propertyId();
      this.saving.set(true);
      this.referenceRevision.update((n) => n + 1);
      this.saveError.set('');
      try {
        const references = await this.service.add(id, this.referenceModel());
        if (id === this.propertyId()) {
          this.references.value.set(references);
          this.referenceRevision.update((n) => n + 1);
          this.editorOpen.set(false);
          this.referenceModel.update((model) => ({
            ...model,
            sourceUrl: '',
            monthlyRent: 0,
            note: '',
            kind: 'ASKING_RENT',
          }));
          this.referenceForm().reset();
        }
      } catch (error) {
        if (id === this.propertyId())
          this.saveError.set(this.message(error, 'Référence non enregistrée.'));
      } finally {
        this.saving.set(false);
      }
    });
  }

  protected async remove(referenceId: string): Promise<void> {
    if (this.saving()) return;
    const id = this.propertyId();
    this.saving.set(true);
    this.referenceRevision.update((n) => n + 1);
    this.saveError.set('');
    try {
      const references = await this.service.remove(id, referenceId);
      if (id === this.propertyId()) {
        this.references.value.set(references);
        this.referenceRevision.update((n) => n + 1);
      }
    } catch (error) {
      if (id === this.propertyId())
        this.saveError.set(this.message(error, 'Suppression impossible.'));
    } finally {
      this.saving.set(false);
    }
  }

  protected export(): void {
    const result = this.result();
    if (!result) return;
    const url = URL.createObjectURL(
      new Blob([JSON.stringify(result, null, 2)], { type: 'application/json' }),
    );
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `immoradar-qualification-${result.dealId}.json`;
    anchor.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  protected currency(value: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(value);
  }
  private message(error: unknown, fallback: string): string {
    return error instanceof HttpErrorResponse ? (error.error?.detail ?? fallback) : fallback;
  }
  private today(): string {
    const date = new Date();
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
