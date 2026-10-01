import { Directive, ElementRef, inject } from '@angular/core';
import { DealStatus, EnergyClass } from '../../core/models/deal.model';

export const DEAL_STATUSES: { value: DealStatus; label: string }[] = [
  { value: 'TO_REVIEW', label: 'À étudier' },
  { value: 'TO_VISIT', label: 'À visiter' },
  { value: 'OFFER_MADE', label: 'Offre faite' },
  { value: 'ACQUIRED', label: 'Acquis' },
  { value: 'REJECTED', label: 'Écarté' },
];

export function statusLabel(status: DealStatus): string {
  return DEAL_STATUSES.find((item) => item.value === status)?.label ?? status;
}

/** F and G are "passoires énergétiques": rental is or will be banned. */
export function isEnergySieve(energyClass: EnergyClass | null): boolean {
  return energyClass === 'F' || energyClass === 'G';
}

const FALLBACK_IMAGE =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 160 120"><rect width="160" height="120" fill="#10221e"/>' +
      '<path d="M50 88V58l30-22 30 22v30M68 88V70h24v18" fill="none" stroke="#4f6a60" stroke-width="4" stroke-linejoin="round"/></svg>',
  );

/** Replaces a broken listing photo (expired portal link, blocked host) with a neutral placeholder. */
@Directive({ selector: 'img[appImageFallback]', host: { '(error)': 'useFallback()' } })
export class DealImageFallbackDirective {
  private readonly image = inject<ElementRef<HTMLImageElement>>(ElementRef).nativeElement;

  protected useFallback(): void {
    if (this.image.src !== FALLBACK_IMAGE) this.image.src = FALLBACK_IMAGE;
  }
}
