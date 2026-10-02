# Apprendre Angular avec ImmoRadar

Ce guide décrit le code présent dans le dépôt, basé sur Angular 22. Il sert de parcours de lecture, pas de promesse que toute l'application est déjà industrialisée.

## 1. Suivre la donnée

Commencer par `frontend/src/app/core/models/deal.model.ts` : les contrats TypeScript décrivent les requêtes et réponses. Ils ne valident pas les données à l'exécution ; cette responsabilité reste au backend pour les entrées métier.

Puis lire `core/services/deal.service.ts` : le service construit les appels HTTP. Il ne choisit ni le bien sélectionné ni l'année affichée. Les lectures de recherche et de simulation exposent des Observables ; les commandes ponctuelles (ajout, favori, PDF) sont attendues avec `firstValueFrom`.

Dans `features/deal-finder/deal-finder.component.ts`, le composant de page orchestre la recherche et la simulation avec `rxResource`. Quand les paramètres changent, Angular se désabonne du flux précédent et `HttpClient` peut annuler la requête. Une Promise issue de `firstValueFrom` ne propageait pas cette annulation au réseau.

`hasValue()` protège la lecture d'une ressource en échec : `value()` peut lever une erreur. Chargement, résultat vide et panne réseau doivent rester des états distincts.

## 2. Choisir le bon signal

| Besoin concret | API | Exemple |
| --- | --- | --- |
| État modifié directement | `signal` | Indicateur du graphique, ouverture du formulaire |
| Valeur uniquement dérivée | `computed` | Statistiques de la page, tracé SVG |
| État modifiable dépendant d'un autre | `linkedSignal` | Page remise à zéro après changement des filtres |
| Requête réactive avec Observable | `rxResource` | Recherche et simulation HTTP |
| Données fournies par le parent | `input.required` | Projection du composant graphique |

Éviter un `effect` pour recopier l'état d'un signal dans un autre. Cela multiplie les synchronisations et les états intermédiaires. Ici la pagination exprime directement sa dépendance aux filtres avec `linkedSignal`.

## 3. Un composant de présentation autonome

Lire `features/deal-finder/components/projection-chart/` dans cet ordre :

1. `projection-chart.component.ts` : un seul input obligatoire, état d'exploration local et dérivations avec `computed`. Aucune requête HTTP.
2. `projection-geometry.ts` : transformation pure de valeurs métier en coordonnées. Le domaine inclut zéro pour représenter correctement les pertes. L'échantillonnage initial a été supprimé pour conserver l'année finale.
3. `projection-chart.component.html` : boutons natifs avec `aria-pressed`, curseur clavier, résumé annoncé et tableau alternatif. Le SVG est décoratif pour les lecteurs d'écran puisque les mêmes données sont accessibles en texte.
4. `projection-chart.component.scss` : styles encapsulés ; couleurs héritées du thème via les variables CSS.
5. `projection-chart.component.spec.ts` : interactions et invariants, notamment les valeurs négatives, l'année finale et le changement de scénario.

`OnPush` exprime la stratégie de mise à jour. Les signaux lus par le template préviennent Angular des changements. Le composant demeure réutilisable sur une future page de comparaison sans embarquer la recherche de biens.

Le SVG natif suffit pour trois courbes alternatives simples et évite une dépendance graphique lourde. Une bibliothèque deviendra pertinente si plusieurs séries, zooms ou exports complexes sont nécessaires.

## 4. Tester à la bonne frontière

- Vitest : géométrie pure et interactions du composant avec `TestBed`. Modifier l'input ou le DOM, attendre `whenStable()`, puis vérifier le résultat visible.
- Playwright : recherche, pagination, modification du financement, exploration clavier et déclenchement du téléchargement. `analysis.spec.ts` contrôle les réponses HTTP pour rendre ces tests déterministes.
- Java : calcul financier et création du PDF. Le faux PDF des tests navigateur vérifie le téléchargement, pas la validité d'un document OpenPDF.

Un test navigateur avec API simulée n'est pas un test full-stack. `playwright.fullstack.config.ts` démarre maintenant Spring sur SQLite en mémoire et Angular : `fullstack/stress-test.spec.ts` crée un bien par le formulaire, contrôle les résultats réels et la relecture après rechargement navigateur. Seul le marché externe est remplacé. Le redémarrage du backend et la comparaison PDF/API restent à couvrir.

## 5. Dette restante, explicitement

Le composant de page contient encore trop de responsabilités : formulaire de création, filtres et simulation sont de bonnes prochaines extractions. Les Signal Forms existent, mais leurs schémas de validation doivent être complétés. La modale doit gérer le focus, sa restitution et le clavier. La localisation pourrait être temporisée pour limiter les requêtes pendant la saisie. Les statistiques affichées sont celles de la page courante, pas des agrégats globaux.

Exercice conseillé : extraire le formulaire de création avec un input d'état et un output de requête validée, puis tester qu'un prix négatif empêche la soumission. Garder la persistance dans le parent ou un service de fonctionnalité, pas dans un composant purement visuel.

## 6. Un composant de fonctionnalité : les justificatifs

Lire `features/deal-finder/components/deal-evidence/`, puis `core/services/deal-evidence.ts` et `core/models/evidence.model.ts`. Ce composant n'est pas purement visuel : il porte un cas d'usage autonome, charger et enregistrer les contrôles du bien fourni par `input.required<string>()`. La page ne connaît pas le formulaire interne.

`rxResource` recharge les contrôles quand l'identifiant change et annule la lecture précédente. Les `linkedSignal` remettent l'éditeur et les messages à zéro sur changement de bien. Le modèle du formulaire dépend du contrôle sélectionné ; les Signal Forms valident la note, la date et le lien HTTPS. Le backend valide aussi ces contraintes et décide quels contrôles comptent : on ne fait pas confiance au compteur calculé dans le navigateur.

Une écriture n'est pas annulée à la navigation. La méthode `save` capture donc l'identifiant avant l'appel et vérifie qu'il est toujours sélectionné avant d'appliquer la réponse. En cas d'échec, le texte saisi reste disponible. Un test Playwright vérifie la sauvegarde/relecture avec API simulée, un autre les estimations et le changement de sélection, un troisième l'erreur sans perte de saisie. Les tests Java HTTP vérifient séparément la persistance réelle sur une base SQLite en mémoire isolée.

Le compteur est volontairement nommé « documentés », pas « certifiés ». Les changements de chiffres invalident les références associées ; la disponibilité expire après 30 jours. Les documents eux-mêmes ne sont pas téléversés : cette étape et leur revue restent dans la roadmap.

## 7. Comparer des scénarios sans multiplier les synchronisations

`components/stress-test/` reçoit une `SimulationRequest` obligatoire et calcule les variantes via un seul endpoint. Le calcul métier reste dans `StressTestService`, qui construit des copies détachées du bien et réutilise le simulateur financier ; aucun changement n'est persisté par ce POST de calcul.

Un `computed` extrait l'identifiant du bien avec égalité scalaire. Le `linkedSignal` des chocs dépend de cet identifiant, pas directement de l'objet de financement : changer l'apport conserve les chocs personnalisés ; changer de bien rétablit les valeurs initiales. Un test a détecté la réinitialisation indésirable lors d'un refinancement et couvre maintenant cette régression.

Les quatre champs utilisent les Signal Forms avec bornes et validation de nombres finis. `rxResource` n'envoie une requête que si ce formulaire est valide ; la lecture précédente est désabonnée lors d'une modification. Le composant distingue chargement, résultat et erreur avec relance. Les tests Vitest vérifient conservation/réinitialisation des réglages et saisies invalides ; Playwright vérifie les montants affichés avec API simulée ; les tests Java HTTP utilisent une base isolée et le vrai moteur de calcul.

## 8. Une décision explicite, jamais un badge obsolète

Lire `components/deal-qualification/`, `core/services/deal-qualification.ts`, puis `core/models/qualification.model.ts`. Les lectures de références utilisent `rxResource`. Les commandes (ajout, suppression, évaluation) utilisent une Promise ; la qualification n'est pas recalculée à chaque frappe et aucun critère métier n'est décidé dans Angular. Spring calcule tous les motifs avec une politique versionnée.

Le formulaire de référence utilise les Signal Forms (HTTPS, bornes numériques, date, contexte). `linkedSignal` réinitialise l'éditeur quand le bien change. Un `computed` scalaire de bien/régime permet de conserver un mode locatif choisi manuellement quand seul l'apport change. `DealEvidenceComponent` émet un `output<void>()` après une écriture réussie ; le parent ne connaît pas ses champs internes, mais incrémente une révision pour invalider une ancienne décision.

Une empreinte réactive regroupe le bien, le financement, le mode et les révisions. Le résultat est un `linkedSignal` remis à zéro à chaque changement de cette empreinte. L'évaluation capture l'empreinte avant le POST et ne publie la réponse que si elle correspond toujours à l'état courant. Ce n'est pas une annulation réseau : c'est une protection contre l'application d'un résultat obsolète. Le test Vitest fait arriver une réponse après un changement de sélection et vérifie qu'aucun badge n'est affiché.

Le JSON exporté provient de la réponse serveur, avec les hypothèses effectivement évaluées. Il est destiné à la relecture, pas à prouver l'authenticité de données déclarées. Le parcours `fullstack/qualification.spec.ts` n'intercepte pas les endpoints de qualification, de justificatifs ou de références : il démontre qu'un dossier complet ne suffit pas en l'absence de marché, puis teste export, rechargement et suppression sur une vraie base isolée. Les tests Java isolent le parcours positif avec un marché explicitement simulé ; ils ne prétendent pas avoir trouvé une annonce rentable réelle.
