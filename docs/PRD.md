# ImmoRadar — état du produit et feuille de route

Mise à jour : 1 octobre 2026. Objectif : aider à **présélectionner** des investissements locatifs vérifiables, sans présenter des données fictives ou des hypothèses comme des faits.

## Livré et vérifié dans le code

- [x] Front Angular : catalogue, pagination, filtres, favoris, formulaire de création, états de chargement/erreur, simulation interactive, visualisation des projections et téléchargement PDF.
- [x] Identité visuelle ImmoRadar : symbole maison/radar sur fond transparent, intégré à l'en-tête et à l'icône d'onglet.
- [x] API Spring : persistance des biens, recherche, simulation, PDF et validation des saisies ; tests unitaires et de contexte.
- [x] Source d'annonce conservée avec le bien ; import opportuniste de métadonnées sur domaines autorisés. Aucun repli vers un bien fictif en cas d'échec ; saisie manuelle disponible.
- [x] Agrégat de marché issu de transactions DVF via FoncierData, par **commune, type et tranche de surface**. Année, effectif et liens de provenance visibles ; absence de résultat si données insuffisantes.
- [x] Transactions DVF récentes via l'API FoncierData : jusqu'à cinq ventes proches en type et surface, montrées comme illustrations non représentatives ; tests opt-in réseau et navigateur sur la chaîne Angular → Spring → API externes.
- [x] Loyer d'équilibre calculé en réévaluant vacance, frais de gestion et impôt simplifié à chaque loyer candidat.
- [x] Aucun taux d'endettement calculé sans revenus de l'emprunteur ; aucun TRI inventé lorsque l'apport initial est nul.
- [x] README et interface explicitent les limites des cinq exemples fictifs et des simulations fiscales.

## Priorité P0 — réellement utile pour chercher des biens

- [ ] **Données de référence locative** : intégrer une source de loyers comparables autorisée, avec granularité, millésime et volume ; ne pas déduire un loyer du seul prix de vente. Ajouter loyer bas/central/haut et scénario prudent.
- [ ] **Qualité des annonces** : importer via une source contractuelle/API ou proposer une capture assistée et vérifiable ; gérer doublons, liens morts, date de publication et historique des changements de prix. Les portails peuvent bloquer l'extraction HTML.
- [ ] **Confiance du dossier** : distinguer chaque champ observé, saisi ou estimé ; afficher les pièces manquantes (DPE, copropriété, taxe foncière, charges récupérables, travaux, vacance locale). Interdire le badge « pépite vérifiée » tant que les données critiques ne sont pas justifiées.
- [ ] **Comparables de vente plus fins** : utiliser les transactions individuelles officielles DVF/DVF+ ou une source sous contrat, nettoyer les outliers, rapprocher type/surface/date/secteur et afficher un intervalle de confiance. L'agrégat communal actuel n'est qu'un repère.
- [ ] **Tests full-stack reproductibles en CI** : créer un bien via le navigateur avec le backend et une base isolée, le retrouver après redémarrage et comparer les résultats API/PDF. Le smoke test live actuel dépend du réseau et d'un exemple préchargé ; il n'est pas ce test de CI.

## Priorité P1 — fiabilité financière

- [ ] Assurance de prêt, frais de garantie et de dossier, mensualités réelles, distinction charges récupérables/non récupérables et dépenses exceptionnelles.
- [ ] Fiscalité paramétrable et actualisée : conditions d'éligibilité des régimes, plafonds et cas particuliers, amortissement correctement ventilé, revente et plus-value, fiscalité des distributions SCI. Faire relire les hypothèses par un professionnel.
- [ ] TRI/VAN après coûts et impôts de sortie, scénarios de prix/loyer/taux, analyse de sensibilité et risques de perte ; traiter explicitement les flux de trésorerie et apports supplémentaires.
- [ ] Capacité bancaire uniquement après saisie des revenus, crédits existants, assurance et prise en compte des loyers selon la politique prêteur ; ne pas prétendre donner une décision HCSF universelle.
- [ ] Scénarios sauvegardés et comparables par bien, avec versionnement des hypothèses et date de calcul.

## Priorité P2 — démo publique et exploitation

- [ ] Authentification, autorisations et séparation des données par utilisateur ; ne pas déployer l'API actuelle en accès public.
- [ ] Migrations de schéma (Flyway ou Liquibase), sauvegardes, observabilité, limites d'appels externes et cache borné.
- [ ] Jeu de démonstration clairement fictif et jeu d'évaluation sourcé, reproductible et conforme aux droits d'usage.
- [ ] Déploiement de préproduction, contrôles de sécurité, accessibilité et tests de charge.

## Critère de « pépite » acceptable

Une opportunité ne sera qualifiée qu'avec une annonce encore disponible, un prix et une surface vérifiés, un loyer appuyé par des références locales, un budget de travaux et charges documenté, suffisamment de ventes comparables et plusieurs scénarios financiers incluant une marge de sécurité. Le produit actuel fournit un **outil de tri et de simulation**, pas encore ce niveau de preuve.

Les tests Playwright actuels utilisent des réponses API simulées. Ils valident l'interface, pas la chaîne navigateur–Spring–base. Voir aussi [le parcours Angular](ANGULAR_ARCHITECTURE.md).
