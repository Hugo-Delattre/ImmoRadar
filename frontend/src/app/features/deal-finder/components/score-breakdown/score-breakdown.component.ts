import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { ScoreFactor } from '../../../../core/models/deal.model';

/** Explains the Radar score: one bar per criterion, with the reason behind the points. */
@Component({
  selector: 'app-score-breakdown',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="score-card" aria-labelledby="score-title">
      <header>
        <div>
          <span class="eyebrow">Pourquoi ce score</span>
          <h3 id="score-title">Radar score <strong>{{ score() }}</strong><small>/10</small></h3>
        </div>
        <p>Décote, rendement, cash-flow, négociation et DPE.</p>
      </header>
      <ul>
        @for (factor of factors(); track factor.key) {
          <li [class.malus]="factor.points < 0">
            <div class="row">
              <span>{{ factor.label }}</span>
              <b>{{ factor.points > 0 ? '+' : '' }}{{ factor.points }} <small>/ {{ factor.maxPoints }}</small></b>
            </div>
            <div class="track" aria-hidden="true"><i [style.width.%]="width(factor)"></i></div>
            <small>{{ factor.detail }}</small>
          </li>
        }
      </ul>
    </section>
  `,
  styles: `
    .score-card { margin: 0 0 22px; padding: 20px; border: 1px solid rgba(154,181,171,.14); border-radius: 18px; background: rgba(255,255,255,.02); }
    header { display: flex; justify-content: space-between; align-items: end; gap: 12px; flex-wrap: wrap; margin-bottom: 14px; }
    .eyebrow { color: #c7ff66; font-size: 11px; font-weight: 900; text-transform: uppercase; letter-spacing: .12em; }
    h3 { margin: 4px 0 0; font-size: 18px; }
    h3 strong { color: #c7ff66; font-size: 24px; }
    h3 small, header p { color: #8ba099; font-size: 13px; }
    header p { margin: 0; }
    ul { margin: 0; padding: 0; list-style: none; display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 14px 22px; }
    .row { display: flex; justify-content: space-between; font-size: 13px; font-weight: 700; }
    .row b { color: #eef7f2; }
    .row b small { color: #8ba099; font-weight: 600; }
    .track { height: 6px; margin: 7px 0 6px; border-radius: 4px; background: rgba(255,255,255,.07); overflow: hidden; }
    .track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #6ee7b7, #c7ff66); }
    li > small { color: #8ba099; font-size: 12px; line-height: 1.45; }
    li.malus .row b { color: #ff9e8b; }
    li.malus .track i { background: #ff8d76; }
  `,
})
export class ScoreBreakdownComponent {
  readonly factors = input.required<ScoreFactor[]>();
  readonly score = input.required<number>();

  protected width(factor: ScoreFactor): number {
    if (factor.points < 0) return Math.min(100, (Math.abs(factor.points) / 2) * 100);
    return Math.max(0, Math.min(100, (factor.points / factor.maxPoints) * 100));
  }
}
