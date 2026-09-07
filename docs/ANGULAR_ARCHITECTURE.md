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

Un test navigateur avec API simulée n'est pas un test full-stack. Le prochain niveau est de démarrer Spring et une base isolée dans la CI, créer un bien par le formulaire puis vérifier sa persistance et les résultats réels.

## 5. Dette restante, explicitement

Le composant de page contient encore trop de responsabilités : formulaire de création, filtres et simulation sont de bonnes prochaines extractions. Les Signal Forms existent, mais leurs schémas de validation doivent être complétés. La modale doit gérer le focus, sa restitution et le clavier. La localisation pourrait être temporisée pour limiter les requêtes pendant la saisie. Les statistiques affichées sont celles de la page courante, pas des agrégats globaux.

Exercice conseillé : extraire le formulaire de création avec un input d'état et un output de requête validée, puis tester qu'un prix négatif empêche la soumission. Garder la persistance dans le parent ou un service de fonctionnalité, pas dans un composant purement visuel.
