# order-service : projet d'apprentissage Angular 21 + Spring Boot

## Qui je suis et ce que je veux

Je suis développeur Java fullstack (Spring Boot, microservices, Kafka, banque).
Ce projet est mon terrain d'entraînement : je veux maîtriser **Angular 21** et **Spring Boot** sur le bout des doigts,
et savoir expliquer chaque choix en entretien technique.
**La compréhension compte plus que la vitesse.**

## Mode de travail (important)

- Avant de coder une notion nouvelle, explique-la en quelques phrases, avec le « pourquoi ».
- Pour chaque fonctionnalité : propose d'abord un plan court, puis attends ma validation.
- Quand je dis « je l'écris », ne génère pas le code : guide-moi, puis relis mon code comme en revue de merge request.
- Quand tu fais un choix (opérateur RxJS, signal ou Observable, annotation Spring…), dis pourquoi celui-là et pas l'autre.
- Signale les pièges classiques et les questions qu'un tech lead pourrait me poser sur ce que l'on vient de faire.
- À la fin de chaque étape, ajoute dans `LEARNING.md` : les notions vues, les pièges rencontrés, 3 questions d'entretien avec leur réponse.
- Réponds en français.

## Structure du dépôt (cible)

```
order-service/
├── backend/            Spring Boot (le projet existant)
├── frontend/           Angular 21
├── docker-compose.yml  Kafka, Keycloak (plus tard), etc.
├── CLAUDE.md
└── LEARNING.md         mon journal d'apprentissage
```

Première tâche : déplacer le projet Spring Boot existant dans `backend/` sans rien casser (`mvn verify` doit passer).

## Back-end : règles à respecter

- **Stack** : Java 21, Spring Boot 3.5, Maven, H2 en local, Kafka, springdoc.
- **Architecture hexagonale** :
  - `domain` : Java pur, aucune annotation (pas de Spring, JPA, Jackson). Les règles métier vivent ici.
  - `application` : ports (`port/in`, `port/out`) et services de cas d'usage, sans Spring.
  - `infrastructure` : adaptateurs (REST, Kafka, JPA, Outbox) et `config`. Seul endroit où Spring apparaît.
  - Les services d'application sont déclarés en `@Bean` dans `infrastructure/config`, pas de `@Service`.
  - Un mapper à chaque frontière : DTO ⇄ commande, domaine ⇄ entité JPA, événement ⇄ message.
  - Le test `HexagonalArchitectureTest` (ArchUnit) doit toujours passer.
- **API-first** : tout nouvel endpoint commence par le contrat `order-api.yaml`. On ne modifie jamais le code généré.
- **Erreurs** : format ProblemDetail via `RestExceptionHandler`.
- **Événements** : publication via l'Outbox (`app.events.publication: outbox`).
- **Tests** : JUnit 5, Mockito, AssertJ. Domaine testé sans mock.

## Front-end : conventions Angular 21

- Composants **standalone** uniquement, pas de `NgModule`.
- Application **zoneless** : l'état d'écran passe par les **signals**.
- `inject()` plutôt que l'injection par constructeur.
- Nouveau control flow : `@if`, `@for` (avec `track`), `@switch`.
- `input()` / `output()` plutôt que `@Input()` / `@Output()`.
- Intercepteurs et guards **fonctionnels** (`HttpInterceptorFn`, `CanActivateFn`).
- Formulaires **typés** (Reactive Forms), puis comparaison avec les Signal Forms (expérimentales).
- Pas de `any`. Les modèles TypeScript reflètent le contrat OpenAPI.
- Organisation par fonctionnalité : `core/` (services transverses, intercepteurs), `shared/`, `features/orders/`.
- Tests avec **Vitest**.

## Mon parcours (cocher au fur et à mesure)

- [ ] 1. Socle : composants standalone, routing, `@if`/`@for`, **`signal`, `computed`, `effect`, `linkedSignal`, `toSignal`, `toObservable`**
      (exercice : un panier avec total calculé, plafond de 10 000 et brouillon sauvegardé dans le localStorage)
- [ ] 2. Lister et consulter : `HttpClient`, **Observables** (`pipe`, `map`, `switchMap`, `catchError`), `async` pipe
      / back : cas d'usage `ListOrders` et `GetOrder`, pagination, 404
- [ ] 3. Recherche instantanée : `debounceTime`, `distinctUntilChanged`, `switchMap` / back : filtres et tri
- [ ] 4. Créer une commande : **Reactive Forms**, `FormArray` pour les lignes, validateurs, erreurs 400 du back
- [ ] 5. Le même formulaire en **Signal Forms**, et comparaison
- [ ] 6. **Authentification** : Keycloak, OIDC + PKCE, **intercepteur** qui ajoute le token, **guards** par rôle
      / back : resource server JWT, `customerId` tiré du token
- [ ] 7. Intercepteurs avancés : erreurs (401, 500), chargement, identifiant de corrélation
- [ ] 8. Temps réel : SSE alimenté par Kafka, consommé comme un Observable
- [ ] 9. État partagé : store à base de signals, puis NgRx SignalStore
- [ ] 10. Tests : Vitest, `HttpTestingController`, Playwright / back : Testcontainers
- [ ] 11. Production : image Docker Nginx pour le front, Docker Compose complet, pipeline CI

## Commandes

- Back : `cd backend && mvn verify` (tests) / `mvn spring-boot:run` (port 8080)
- Front : `cd frontend && ng serve` (port 4200) / `ng test`
- Infra : `docker compose up -d` / `docker compose stop`
- Swagger : http://localhost:8080/swagger-ui.html
- Console H2 : http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:orders`, utilisateur `sa`)

## Git

- Une fonctionnalité = une branche (`feature/etape-1-signals`…).
- Petits commits au format Conventional Commits (`feat:`, `fix:`, `test:`, `refactor:`, `docs:`).
- Lancer les tests back et front avant chaque commit.

## À ne pas faire

- Générer une étape entière d'un coup sans m'expliquer.
- Mettre une annotation Spring, JPA ou Jackson dans `domain` ou `application`.
- Modifier le code généré par OpenAPI ou contourner le contrat.
- Désactiver un test qui échoue au lieu de le corriger.
- Utiliser des API Angular dépréciées (`NgModule`, `@Input()`, `*ngIf`, `*ngFor`) sauf pour me montrer la différence.
