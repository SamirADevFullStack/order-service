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
  Surefire 3.5.2 et 3.5.4 comptent 17 tests et font échouer le build. Correctif à faire dans un commit `fix:` dédié.
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
