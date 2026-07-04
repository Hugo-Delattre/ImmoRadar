import { Service } from '@angular/core';
import { Deal, DealFilters, SimulationInput, SimulationResult } from '../models/deal.model';

@Service()
export class DealService {
  private readonly mockDeals: Deal[] = [
    {
      id: '1',
      title: 'Immeuble de Rapport - 4 Lots',
      price: 245000,
      monthlyRent: 2150,
      monthlyCharges: 180,
      propertyTax: 1600,
      renovationCost: 35000,
      location: 'Saint-Étienne (42)',
      surface: 140,
      propertyType: 'Building',
      description: 'Immeuble de rapport composé de 2 studios et 2 T2 en parfait état. Tous les lots sont actuellement loués. Compteurs électriques individuels. Faible taxe foncière.',
      opportunityScore: 9.2,
      imageUrl: 'https://images.unsplash.com/photo-1570129477492-45c003edd2be?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: '2',
      title: 'Appartement T4 Spécial Colocation',
      price: 135000,
      monthlyRent: 1200,
      monthlyCharges: 110,
      propertyTax: 950,
      renovationCost: 15000,
      location: 'Limoges (87)',
      surface: 78,
      propertyType: 'Apartment',
      description: 'Appartement T4 proche des facultés. Aménagé en 3 chambres pour colocation étudiante. Vendu entièrement meublé et équipé. Rendement optimal immédiat.',
      opportunityScore: 8.8,
      imageUrl: 'https://images.unsplash.com/photo-1502672260266-1c1ef2d93688?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: '3',
      title: 'Studio meublé hyper-centre',
      price: 89000,
      monthlyRent: 620,
      monthlyCharges: 65,
      propertyTax: 520,
      renovationCost: 5000,
      location: 'Mulhouse (68)',
      surface: 24,
      propertyType: 'Studio',
      description: 'Studio entièrement rénové par un architecte d\'intérieur. Emplacement numéro 1, à 2 minutes à pied de la gare et des commerces. Idéal LMNP.',
      opportunityScore: 8.4,
      imageUrl: 'https://images.unsplash.com/photo-1522708323590-d24dbb6b0267?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: '4',
      title: 'Maison divisée en 2 appartements',
      price: 195000,
      monthlyRent: 1480,
      monthlyCharges: 120,
      propertyTax: 1250,
      renovationCost: 20000,
      location: 'Le Mans (72)',
      surface: 115,
      propertyType: 'House',
      description: 'Maison de ville divisée en un T3 avec jardin privatif et un T2 à l\'étage. Entrées séparées. Fort potentiel de revente après découpe cadastrale officielle.',
      opportunityScore: 7.9,
      imageUrl: 'https://images.unsplash.com/photo-1512917774080-9991f1c4c750?auto=format&fit=crop&w=800&q=80'
    },
    {
      id: '5',
      title: 'Petit immeuble de centre-ville',
      price: 310000,
      monthlyRent: 2600,
      monthlyCharges: 220,
      propertyTax: 2100,
      renovationCost: 45000,
      location: 'Belfort (90)',
      surface: 180,
      propertyType: 'Building',
      description: 'Immeuble de rapport comprenant 5 appartements. Toiture refaite en 2024. Travaux de rafraîchissement à prévoir sur 2 appartements pour optimiser les loyers.',
      opportunityScore: 8.1,
      imageUrl: 'https://images.unsplash.com/photo-1564013799919-ab600027ffc6?auto=format&fit=crop&w=800&q=80'
    }
  ];

  async getDeals(filters: DealFilters): Promise<Deal[]> {
    const queryParams = new URLSearchParams();
    if (filters.priceMax !== undefined && filters.priceMax !== null) {
      queryParams.append('priceMax', filters.priceMax.toString());
    }
    if (filters.yieldMin !== undefined && filters.yieldMin !== null) {
      queryParams.append('yieldMin', filters.yieldMin.toString());
    }
    if (filters.cashflowMin !== undefined && filters.cashflowMin !== null) {
      queryParams.append('cashflowMin', filters.cashflowMin.toString());
    }
    if (filters.location) {
      queryParams.append('location', filters.location);
    }

    const response = await fetch(`/api/deals?${queryParams.toString()}`);
    if (!response.ok) {
      throw new Error('Failed to fetch deals from server');
    }
    return response.json();
  }

  calculateSimulation(deal: Deal, input: SimulationInput): SimulationResult {
    const notaryFees = Math.round(deal.price * 0.08); // 8% frais de notaire
    const totalProjectCost = deal.price + deal.renovationCost + notaryFees;
    const loanAmount = Math.max(0, totalProjectCost - input.downpayment);
    const loanTerm = typeof input.loanTermYears === 'string' ? parseInt(input.loanTermYears, 10) : input.loanTermYears;

    // Calcul de la mensualité du crédit
    let monthlyMortgage = 0;
    if (loanAmount > 0 && input.interestRate > 0 && loanTerm > 0) {
      const monthlyRate = (input.interestRate / 12) / 100;
      const totalMonths = loanTerm * 12;
      monthlyMortgage = Math.round(
        (loanAmount * monthlyRate * Math.pow(1 + monthlyRate, totalMonths)) /
        (Math.pow(1 + monthlyRate, totalMonths) - 1)
      );
    }

    // Calcul des rendements
    const grossYield = parseFloat(((deal.monthlyRent * 12) / deal.price * 100).toFixed(2));
    const netYield = parseFloat((((deal.monthlyRent * 12) - (deal.monthlyCharges * 12) - deal.propertyTax) / totalProjectCost * 100).toFixed(2));

    // Calcul des impôts annuels basiques
    let taxAnnual = 0;
    const annualRent = deal.monthlyRent * 12;
    
    if (input.taxRegime === 'MICRO_BIC') {
      // Abattement de 50%, imposé au TMI moyen estimé à 30% + prélèvements sociaux 17.2% = 47.2%
      taxAnnual = Math.round((annualRent * 0.5) * 0.472);
    } else if (input.taxRegime === 'NU') {
      // Régime foncier classique micro-foncier (abattement 30%)
      taxAnnual = Math.round((annualRent * 0.7) * 0.472);
    } else {
      // LMNP Réel - Amortissements (typiquement 3% du bien + 10% des travaux par an) + intérêts + charges déduites.
      // Souvent 0 impôt sur les premières années.
      const interestAnnual = monthlyMortgage * 12 * 0.4; // estimation grossière des intérêts
      const depreciation = (deal.price * 0.03) + (deal.renovationCost * 0.1);
      const deductibleExpenses = (deal.monthlyCharges * 12) + deal.propertyTax + interestAnnual + depreciation;
      const taxableAmount = Math.max(0, annualRent - deductibleExpenses);
      taxAnnual = Math.round(taxableAmount * 0.472);
    }

    // Cashflow mensuel net
    const monthlyCashFlow = Math.round(
      deal.monthlyRent - 
      deal.monthlyCharges - 
      (deal.propertyTax / 12) - 
      monthlyMortgage - 
      (taxAnnual / 12)
    );

    return {
      price: deal.price,
      grossYield,
      netYield,
      monthlyMortgage,
      monthlyCashFlow,
      taxAnnual
    };
  }
}
