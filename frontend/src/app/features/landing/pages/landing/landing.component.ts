import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

interface LandingFeature {
  icon: string;
  title: string;
  description: string;
}

interface FooterGroup {
  title: string;
  links: Array<{ label: string; href: string }>;
}

@Component({
  selector: 'app-landing',
  imports: [RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss'
})
export class LandingComponent {
  readonly features: LandingFeature[] = [
    {
      icon: '/landing-assets/grid-icons/asset-1.svg',
      title: 'Organisation RH',
      description: 'Centralisez employés, postes, départements, comptes et informations de paiement.'
    },
    {
      icon: '/landing-assets/grid-icons/asset-2.svg',
      title: 'Temps & absences',
      description: 'Gérez les demandes d’absence, soldes de congé, justificatifs, calendriers et pointage.'
    },
    {
      icon: '/landing-assets/grid-icons/asset-3.svg',
      title: 'Paie complète',
      description: 'Préparez variables, calculs, bulletins PDF et exports comptables depuis un seul espace.'
    }
  ];

  readonly workflow = ['Organisation', 'Absences & pointage', 'Variables de paie', 'Calcul & bulletins', 'Exports'];

  readonly partners = [
    'logoipsum-logo-1.svg',
    'logoipsum-logo-2.svg',
    'logoipsum-logo-3.svg',
    'logoipsum-logo-4.svg'
  ];

  readonly footerGroups: FooterGroup[] = [
    {
      title: 'Produit',
      links: [
        { label: 'Fonctionnalités', href: '#features' },
        { label: 'Workflow', href: '#workflow' },
        { label: 'Connexion', href: '/login' }
      ]
    },
    {
      title: 'Modules',
      links: [
        { label: 'Organisation', href: '/login' },
        { label: 'Absences', href: '/login' },
        { label: 'Pointage', href: '/login' },
        { label: 'Paie', href: '/login' }
      ]
    },
    {
      title: 'Prochainement',
      links: [
        { label: 'Paie', href: '#roadmap' },
        { label: 'Présence', href: '#roadmap' },
        { label: 'Bulletins', href: '#roadmap' },
        { label: 'Assistant IA', href: '#roadmap' }
      ]
    }
  ];
}
