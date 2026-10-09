# Journal d'apprentissage

## Étape 0 : déplacer le back dans `backend/` (2026-10-06)

### Notions vues

- **Baseline avant refactoring** : lancer `mvn verify` *avant* de toucher à quoi que ce soit (ici 15 tests, vert).
  Si ça casse après, on sait que c'est le changement, pas un problème préexistant.
- **`${project.basedir}`** : le `pom.xml` référence le contrat via `${project.basedir}/src/main/resources/...`.
  Le chemin est relatif au pom, pas au répertoire courant : le module se déplace sans modification,
  et `mvn -f backend/pom.xml verify` fonctionne depuis la racine (cas typique d'un pipeline CI).
- **`target/` est un artefact, pas une source** : on ne le déplace pas, on fait `mvn clean`.
  Il contient des chemins absolus (`maven-status`, rapports) qui deviendraient faux.
- **Monorepo** : `backend/`, puis `frontend/`, et ce qui est partagé (`docker-compose.yml`, `.gitignore`) à la racine.
  Un `.gitignore` avec `target/` (sans `/` initial) ignore `target/` à toute profondeur, donc `backend/target/` aussi.
- **Surefire et JUnit Platform** : Surefire ne lance pas les tests lui-même. Il est client du *Launcher* JUnit Platform,
  qui découvre les moteurs (`junit-jupiter`, `archunit`…) par `ServiceLoader`
  (`META-INF/services/org.junit.platform.engine.TestEngine`). Jupiter exécute les `@Test` (méthodes),
  ArchUnit exécute les `@ArchTest` (ici des **champs**, donc une source de type `FieldSource`).

### Pièges rencontrés

- **Le garde-fou ArchUnit ne protégeait rien.** Le rapport affichait `Tests run: 0` pour `HexagonalArchitectureTest`.
  Preuve : en ajoutant volontairement `@Component` sur une classe du domaine (dans une copie jetable), le build restait **vert**.
  Diagnostic : le Launcher JUnit appelé directement découvre bien les 2 règles et détecte la violation ;
  c'est **Surefire 3.5.3** (version imposée par Spring Boot 3.5.0) qui perd les tests à source `FieldSource`.
  Surefire 3.5.2 et 3.5.4 comptent 17 tests et font échouer le build. Corrigé dans la PR #1 (voir étape 0 bis).
  - Leçon : **`Tests run: 0` dans un rapport est un signal d'alarme**, pas un détail.
  - Leçon : un test qu'on n'a jamais vu échouer n'est pas un test. Il faut le **faire échouer exprès** au moins une fois.
- **IntelliJ** référençait `$PROJECT_DIR$/pom.xml` (dans `.idea/misc.xml`) : mis à jour vers `backend/pom.xml`.
  À faire côté IDE : onglet Maven > *Reload All Maven Projects*.
- **`docker compose`** se lance maintenant depuis la racine du dépôt, pas depuis `backend/`.

### Questions d'entretien

1. **Monorepo ou un dépôt par application pour un front Angular et un back Spring Boot ?**
   Le monorepo permet de faire évoluer le contrat OpenAPI, le back et le front dans **un seul commit atomique et revu ensemble**.
   On a une seule CI et un seul `docker compose` pour tout lancer. Les dépôts séparés se justifient quand les équipes,
   les cycles de release ou les droits d'accès sont différents, ou quand le back a plusieurs consommateurs indépendants.
   Dans ce cas, on publie le contrat comme un artefact versionné. Le monorepo exige une CI qui ne rebuild
   que ce qui a changé (filtres par chemin `backend/**`, `frontend/**`).

2. **Comment t'assures-tu qu'un test, par exemple un test d'architecture, protège vraiment ?**
   Je le fais échouer volontairement : j'introduis la violation qu'il est censé détecter et je vérifie que le build casse.
   C'est du *mutation testing* manuel ; PIT l'automatise pour les tests unitaires. Je surveille aussi le nombre de tests exécutés.
   Exemple vécu : Surefire 3.5.3 ignorait les `@ArchTest` (`Tests run: 0`), et le build restait vert avec un domaine
   qui dépendait de Spring. Sans cette vérification, la règle « le domaine ne dépend de rien » n'était plus garantie.

3. **Différence entre `mvn test` et `mvn verify` ? Entre Surefire et Failsafe ?**
   `test` s'arrête après les tests unitaires. `verify` va plus loin dans le cycle de vie : `package`
   (le jar), `integration-test`, puis `verify`. Surefire exécute les tests unitaires en phase `test` et fait échouer
   le build immédiatement. Failsafe exécute les tests d'intégration (`*IT`) en phase `integration-test`, mais ne fait
   échouer le build qu'en phase `verify`. Entre les deux, `post-integration-test` peut arrêter proprement
   l'environnement (conteneurs, serveur). C'est pourquoi on lance `mvn verify`, et non `mvn integration-test`.

## Étape 0 bis : Git, GitHub et le correctif Surefire (2026-10-06)

### Notions vues

- **Surcharger une version gérée par Spring Boot** : `spring-boot-dependencies` (le grand-parent) définit les versions
  sous forme de propriétés (`maven-surefire-plugin.version`, `kafka.version`…). Redéfinir la propriété dans notre pom
  l'emporte sur celle du parent, comme une méthode redéfinie dans une classe fille.
  Vérification : la ligne `--- surefire:3.5.4:test` dans la sortie de Maven, pas seulement le pom.
- **`git init -b main`** : sans `init.defaultBranch`, Git crée encore `master`.
- **`.gitattributes` (`* text=auto eol=lf`)** : la règle des fins de ligne est versionnée et s'impose à tous,
  contrairement à `core.autocrlf`, qui est un réglage local à chaque machine. Évite les scripts shell en CRLF
  qui cassent dans un conteneur Linux (`/bin/sh^M: not found`).
- **Conventional Commits** : `type: titre` en minuscules, sans espace avant les deux-points (`fix:`, pas `Fix :`),
  sinon les outils (commitlint, générateurs de changelog) ne reconnaissent pas le type.
  Le titre dit *quoi*, le corps dit *pourquoi* (le diff montre déjà le quoi).
- **`--amend` crée un nouveau commit** (nouveau hash) : sans risque en local, mais si le commit était déjà poussé,
  les branches locale et distante divergent (`ahead 1, behind 1`).
- **`git push --force-with-lease`** : remplace la branche distante, mais **refuse si elle a bougé** depuis notre dernier
  `fetch`. On n'écrase que ce qu'on a vu. On n'utilise jamais `--force` tout seul.
- **Pull request et stratégies de fusion** :
  - *merge commit* : garde les commits d'origine, plus un commit « Merge pull request » (choix fait pour la PR #1) ;
  - *squash and merge* : un seul nouveau commit par PR, historique linéaire, message modifiable au moment de la fusion ;
  - *rebase and merge* : commits recopiés un par un (nouveaux hashes), historique linéaire.
- **Après la fusion** : `git switch main`, `git pull --ff-only`, `git branch -d <branche>`.
  `-d` refuse de supprimer une branche non fusionnée (filet de sécurité). Après un squash, il faut `-D`,
  car le commit de la branche n'est pas dans `main` (le squash en a créé un autre).

### Pièges rencontrés

- **Amend après push** : le message a été corrigé *après* avoir poussé la branche, d'où la divergence avec GitHub.
  Le push simple est refusé (*non-fast-forward*), et Git suggère alors `git pull` : **c'est le piège**.
  Un pull aurait fusionné l'ancien et le nouveau commit (les deux messages, plus un commit de fusion).
  La bonne réponse est `git push --force-with-lease`, acceptable ici car la branche est personnelle et pas encore fusionnée.
- **Réécrire `main` est interdit** une fois publiée : d'autres l'ont peut-être récupérée.
  Les défauts restants (pas de corps dans le commit, commentaire du pom sans condition de retrait) restent dans l'historique.
- **Un commentaire « pourquoi » doit aussi dire « jusqu'à quand »** : une version figée sans condition de retrait devient
  de la dette, car plus personne ne sait si on peut la supprimer.

### Questions d'entretien

1. **Quand est-il acceptable de réécrire l'historique Git, et comment le faire sans risque ?**
   Uniquement sur une branche dont on est seul utilisateur et qui n'est pas encore fusionnée : on peut faire un
   `--amend` ou un `rebase -i` pour nettoyer avant la revue. Jamais sur `main` ni sur une branche partagée.
   Pour pousser, on utilise `git push --force-with-lease` plutôt que `--force` : il échoue si quelqu'un a poussé
   entre-temps, au lieu d'écraser silencieusement son travail. En entreprise, `main` est en plus protégée
   (*branch protection*) : force-push interdit, PR et CI obligatoires.

2. **Merge commit, squash ou rebase : quelle stratégie de fusion pour une PR ?**
   *Squash* : historique linéaire, un commit par PR (donc par entrée de changelog), idéal avec Conventional Commits
   et des PR petites. On perd le détail des commits intermédiaires.
   *Merge commit* : conserve les commits d'origine et la topologie des branches, utile pour des PR longues dont
   les commits ont du sens individuellement, mais l'historique est moins lisible.
   *Rebase* : linéaire et garde chaque commit, mais exige des commits propres un par un.
   L'important est que l'équipe choisisse une seule stratégie, configurée sur le dépôt GitHub.

3. **Comment changer la version d'une dépendance ou d'un plugin géré par Spring Boot ?**
   Si le projet hérite de `spring-boot-starter-parent`, on redéfinit la propriété (`<kafka.version>`,
   `<maven-surefire-plugin.version>`…) : toutes les déclarations qui l'utilisent suivent, ce qui garde les artefacts
   d'une même famille alignés. Piège : si Spring Boot est importé comme BOM (`<scope>import</scope>`) et pas comme parent,
   surcharger la propriété **ne fonctionne pas**. Les versions sont résolues à l'import ; il faut déclarer la version
   explicitement ou importer un autre BOM *avant* celui de Spring Boot. Dans tous les cas, on documente pourquoi
   et quand retirer la surcharge (ici : quand Spring Boot gérera Surefire en version 3.5.4 ou plus).

## Étape 1 : le socle Angular et les signals (2026-10-06 → 2026-10-09)

Exercice : une page Panier. On choisit des produits dans un catalogue, avec une quantité ; le total se calcule seul ;
le plafond de 10 000 € (même règle que `Order.create` côté back) bloque les ajouts ; le panier survit à un F5.

### Notions vues

- **Projet Angular 21 zoneless** (`ng new ... --zoneless --skip-git`) : sans zone.js, Angular rafraîchit l'écran quand
  un **signal lu par le template** change, ou après un événement du template. L'état d'écran doit donc être en signals.
  `--skip-git` évite un dépôt Git imbriqué dans le monorepo.
- **Composants standalone** : chaque composant déclare ses dépendances dans `imports` (`RouterLink`, `CurrencyPipe`…).
  Un oubli ne donne souvent **aucune erreur** : `routerLink="/panier"` devient un simple attribut HTML inerte.
- **Routing et lazy loading** : `loadComponent: () => import('./features/cart/cart').then((m) => m.Cart)` place le
  composant dans un chunk séparé, téléchargé à la première visite. `App` est la coquille ; `<router-outlet />` est
  l'emplacement que le routeur remplit. `pathMatch: 'full'` sur la route vide, et la route `'**'` en dernier.
- **Logique métier en fonctions pures** (`cart-line.ts`) : sans Angular, testées sans `TestBed`. C'est le `domain` de
  l'hexagonal côté front. Montants calculés en **centimes entiers** (`Math.round(prix * 100) * quantité`),
  car `0.1 + 0.2 = 0.30000000000000004` en JavaScript.
- **`signal`** : `lines()` pour lire, `set` pour remplacer, `update` pour calculer à partir de l'ancienne valeur.
  `set` quand la nouvelle valeur ne dépend pas de l'ancienne (`clear`), `update` sinon.
- **Immuabilité** : on ne modifie jamais un tableau sur place. `map` + `{ ...line, quantity: ... }` pour transformer,
  `filter` pour retirer, `[...lines, nouvelleLigne]` pour ajouter.
- **`computed`** : une valeur dérivée, en lecture seule, mise en cache, dont les dépendances sont détectées seules
  (`total`, `remaining`, `selectedProduct`, `canAdd`). Un `computed` peut lire un autre `computed`.
- **Control flow** : `@for (...; track line.productCode)` avec une clé métier unique, `@empty`, `@if`.
  Liaisons : `{{ }}`, `[propriété]`, `[attr.aria-label]`, `(événement)`, pipe `| currency`.
- **`effect`** : agir sur l'extérieur (localStorage) à chaque changement d'un signal. Créé dans le constructeur
  (contexte d'injection), arrêté automatiquement avec le composant. Jamais pour calculer une valeur ou modifier un signal.
- **Service `@Injectable({ providedIn: 'root' })` + `inject()`** : le catalogue. Un service se justifie quand il y a
  des dépendances, un état partagé, ou un besoin de le remplacer dans les tests (`{ provide: ..., useValue: ... }`).
  Un calcul pur reste une simple fonction exportée.
- **`toSignal`** : Observable → signal, avec `initialValue` (le signal doit avoir une valeur avant la première émission)
  et désabonnement automatique.
- **`linkedSignal({ source, computation })`** : un signal **modifiable** qui se réinitialise quand sa source change
  (la quantité revient à 1 quand on change de produit). Remplace l'anti-pattern « `effect` qui fait un `set` ».
- **Les deux sens d'un champ de formulaire** : `[value]="signal()"` pour afficher, `(input)` / `(change)` pour écrire.
  Sans `[value]`, l'écran et l'état peuvent diverger.
- **`toObservable` + `debounceTime(500)`** : signal → Observable, pour accéder aux opérateurs temporels de RxJS.
  La sauvegarde n'a lieu qu'après 500 ms sans modification. `toObservable` termine son flux à la destruction du composant.
- **Locale** : `{ provide: LOCALE_ID, useValue: 'fr-FR' }`, `registerLocaleData(localeFr)` et
  `DEFAULT_CURRENCY_CODE` changent le format de **tous** les pipes depuis la configuration (`9 975,00 €`).
- **Tests Vitest** : tests par le DOM avec `aria-label` et `data-testid`, `await fixture.whenStable()` après chaque
  action (zoneless), `localStorage.clear()` dans le `beforeEach` pour isoler les tests, faux service injecté,
  et horloge simulée (`vi.useFakeTimers`) pour la sauvegarde différée.

### Pièges rencontrés

- **Un test tautologique** : `expect(exceedsMax(MAX_TOTAL_CENTS)).toBe(false)` passait avec un plafond à 0, car
  l'attendu était calculé avec le code testé. L'attendu doit être une valeur en dur, issue du besoin métier (`1_000_000`).
- **L'unité dans le nom** : `MAX_TOTAL_CENTS = 10_000` faisait 100 € et non 10 000 €. Le suffixe `_CENTS` a permis de voir l'erreur.
- **Un import statique casse le lazy loading sans prévenir** : `import { Cart }` dans `app.ts` laissait un chunk `cart`
  de 60 octets (une simple redirection). Il faut regarder la **taille** du chunk dans `ng build`, pas seulement sa présence.
- **Un composant écrit en dur dans `App`** (`<app-cart/>`) contourne le routeur : l'URL change, mais l'écran n'en dépend plus.
- **`computed(() => { totalCents(...) })` sans `return`** donne un `Signal<void>`. Le typer `computed<number>` fait
  apparaître l'erreur tout de suite.
- **`map` qui renvoie la condition** (`lines.map((l) => l.productCode === code)`) produit un tableau de booléens ;
  `filter` retire des lignes au lieu de les modifier. `map` transforme, `filter` sélectionne.
- **Un `try` qui n'entoure pas la ligne risquée** : `JSON.parse` déplacé avant le `try` ne protégeait plus rien.
- **`return null` dans une fonction `void`** est refusé par TypeScript ; un `catch` vide se commente.
- **`localStorage.clear()` hors du `beforeEach`** ne s'exécute qu'une fois : les tests se transmettaient le panier.
- **`vi.useFakeTimers()` sans option bloque les tests zoneless** : Angular planifie le rafraîchissement avec `setTimeout`.
  Et `debounceTime` vérifie `Date.now()` : il faut simuler `setInterval`, `clearInterval` **et** `Date`, rien de plus.
- **Deux méthodes du même nom dans une classe** : la dernière écrase la première ; l'ancienne version doit être commentée.
- **La traduction automatique de Chrome** réécrivait la page (`BOOK` → « LIVRE ») tant que `index.html` déclarait `lang="en"`.
- **Un fichier non enregistré** (le rond ● de l'onglet VS Code) : `ng test` et le relecteur voient l'ancienne version.
  Auto Save, format à l'enregistrement (Prettier) et imports automatiques sont configurés dans `.vscode/settings.json`.

### Questions d'entretien

1. **Quelle différence entre `computed`, `linkedSignal` et un `effect` qui fait un `set` ?**
   `computed` dérive une valeur en lecture seule : elle se recalcule quand ses dépendances changent, on ne peut pas
   l'écrire. `linkedSignal` dérive aussi une valeur d'une source, mais reste **modifiable** : l'utilisateur peut
   la changer, et elle se réinitialise quand la source change (la quantité revient à 1 au changement de produit).
   Un `effect` qui fait un `set` obtient le même résultat, mais l'état est modifié depuis deux endroits, le lien est
   caché, et on risque des boucles. `effect` est réservé aux effets de bord vers l'extérieur (stockage, logs,
   bibliothèque non-Angular).

2. **Signal ou Observable : lequel choisir, et comment passer de l'un à l'autre ?**
   Un signal représente un **état** qui a toujours une valeur, lue de façon synchrone : c'est le bon outil pour l'état
   d'écran, surtout en zoneless. Un Observable représente un **flux d'événements dans le temps** (HTTP, frappes,
   minuteurs) et offre les opérateurs RxJS (`debounceTime`, `switchMap`…). `toSignal` transforme un Observable en
   signal pour l'afficher (avec `initialValue` et désabonnement automatique) ; `toObservable` fait l'inverse pour
   appliquer des opérateurs temporels à un signal, comme la sauvegarde différée du panier.

3. **Pourquoi ne modifie-t-on jamais un tableau sur place dans un signal ?**
   Un signal compare l'ancienne et la nouvelle valeur par référence (`Object.is`). Après un `push` ou un
   `line.quantity++` suivi de `set(memeTableau)`, il voit la même référence et ne prévient personne : l'écran ne
   bouge pas, et les `computed` ne se recalculent pas. Il faut créer un nouveau tableau (`map`, `filter`,
   `[...lines, x]`) et de nouveaux objets (`{ ...line, quantity: ... }`). Typer le signal `readonly CartLine[]`
   fait refuser `push` par le compilateur. Bonus : une donnée partagée modifiée sur place peut aussi contaminer
   d'autres composants ou d'autres tests.
