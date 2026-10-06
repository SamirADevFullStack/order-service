# order-service : exemple d'architecture hexagonale

Petit microservice Spring Boot 3 / Java 21 qui crée des commandes. Il sert à **comprendre et retenir**
l'architecture hexagonale (Ports & Adapters) : un même cas d'usage, appelé par deux adaptateurs entrants
(REST et Kafka), qui utilise deux adaptateurs sortants (JPA et Kafka).

L'API REST est développée en **API-first** : le contrat OpenAPI est écrit d'abord, le code Java en est généré.

## Le plan des packages

```
com.banque.order
├── domain                      Java pur : règles métier
│   ├── model                   Order, OrderLine, Money, OrderId, OrderStatus, OrderCreatedEvent
│   └── exception               InvalidOrderException
├── application                 Java pur : cas d'usage
│   ├── port/in                 CreateOrderUseCase, CreateOrderCommand
│   ├── port/out                OrderRepository, EventPublisher
│   └── service                 CreateOrderService
└── infrastructure              Spring : tout ce qui est technique
    ├── in/rest                 OrderController (implements OrdersApi), RestExceptionHandler, mapper/
    │   └── api, api.model      GÉNÉRÉS depuis order-api.yaml : OrdersApi, CreateOrderRequest, OrderResponse...
    ├── in/kafka                OrderEventConsumer, OrderRequestMessage
    ├── out/persistence         OrderJpaAdapter, OrderJpaEntity, OrderLineJpaEntity, SpringDataOrderRepository, OrderPersistenceMapper
    ├── out/outbox              OutboxEventPublisher, OutboxRelay, OutboxEventJpaEntity, SpringDataOutboxRepository
    ├── out/messaging           KafkaEventPublisher (version directe, pour comparer), OrderCreatedMessage
    └── config                  BeanConfiguration, TransactionalCreateOrderUseCase, KafkaTopicConfig
```

Le contrat se trouve dans `src/main/resources/static/openapi/order-api.yaml`.

**La règle d'or** : les dépendances pointent vers le centre. `domain` ne dépend de rien,
`application` dépend seulement de `domain`, `infrastructure` dépend des deux.
Le test `HexagonalArchitectureTest` (ArchUnit) fait échouer le build si la règle est violée.

## Lancer le projet

Le serveur démarre sur le **port 8080** (paramètre `server.port` dans `application.yml`).

1. **Importer** dans IntelliJ : *File > Open*, choisir le dossier racine du dépôt.
   Le `pom.xml` est dans `backend/` : si IntelliJ ne le détecte pas, clic droit sur `backend/pom.xml` > *Add as Maven Project*.
   Vérifier que le SDK du projet est un **JDK 21**.
2. **Générer le code du contrat** : onglet Maven > *order-service* > *Lifecycle* > double-clic sur **compile**.
   Le code généré apparaît dans `target/generated-sources/openapi`. Tant que cette étape n'est pas faite,
   IntelliJ affiche `OrdersApi`, `CreateOrderRequest` et `OrderResponse` en rouge : c'est normal.
   Si le dossier n'est pas reconnu comme source, clic droit sur `pom.xml` > *Maven* > *Generate Sources and Update Folders*.
3. **Lancer les tests** : clic droit sur `src/test/java` > *Run 'All Tests'*, ou `mvn verify` dans `backend/`.
   Ils ne demandent ni Docker ni Kafka.
4. **Démarrer Kafka** : `docker compose up -d` à la **racine du dépôt** (le `docker-compose.yml` n'est pas dans `backend/`).
5. **Démarrer l'application** : lancer `OrderApplication`.
6. **Appeler l'API**, au choix :
   - **Swagger UI** : http://localhost:8080/swagger-ui.html, puis *POST /api/orders* > *Try it out* > *Execute* ;
   - **IntelliJ** : ouvrir `requests.http` et cliquer sur la flèche verte à côté d'une requête.

   Swagger affiche directement le contrat, servi sur http://localhost:8080/openapi/order-api.yaml.
7. **Voir la base** : http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:orders`, utilisateur `sa`, sans mot de passe).

Sans Kafka, l'application démarre quand même et l'API REST enregistre les commandes en base,
mais les logs signaleront que le broker est injoignable et la publication de l'événement échouera.

## API-first : le cycle de travail

1. On modifie le **contrat** `order-api.yaml` (et on le fait valider par les consommateurs de l'API).
2. On relance le build : `openapi-generator` régénère `OrdersApi` et les DTO dans `target/generated-sources/openapi`.
3. Si le contrat a changé une signature, **le build échoue** tant que `OrderController` et `OrderRestMapper`
   ne suivent pas. Le code ne peut donc pas dériver du contrat.

On ne modifie jamais le code généré à la main : il est écrasé à chaque build.
La validation technique (`required`, `minLength`, `pattern`, `minimum`) est écrite dans le contrat
et traduite en annotations Bean Validation (`@NotNull`, `@Size`, `@Pattern`, `@Min`) sur les DTO générés.

Configuration du générateur dans le `pom.xml` (plugin `openapi-generator-maven-plugin`) :
`interfaceOnly` génère l'interface sans contrôleur, `skipDefaultInterface` oblige à implémenter chaque opération,
`useTags` nomme l'interface d'après le tag (`Orders` -> `OrdersApi`), `useSpringBoot3` utilise `jakarta.*`.

## Tester le second point d'entrée : Kafka

Envoyer une demande de commande sur le topic `order-requests` :

```
docker exec -it kafka /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic order-requests
```

puis coller sur une seule ligne :

```
{"customerId":"C-100","currency":"EUR","lines":[{"productCode":"BOOK","quantity":1,"unitPrice":9.90}]}
```

Lire les événements publiés sur `orders.created` :

```
docker exec -it kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic orders.created --from-beginning
```

## Le pattern Outbox

**Le problème** : écrire la commande en base puis publier dans Kafka, c'est écrire dans deux systèmes.
Si Kafka tombe entre les deux, la commande existe mais l'événement est perdu (double écriture).

**La solution en place** :

1. `TransactionalCreateOrderUseCase` exécute tout le cas d'usage dans **une seule transaction**.
   C'est un décorateur déclaré dans `BeanConfiguration` : `CreateOrderService` reste sans Spring.
2. Le port `EventPublisher` est implémenté par `OutboxEventPublisher`, qui **n'appelle pas Kafka** :
   il écrit l'événement (JSON prêt à envoyer) dans la table `outbox_events`, dans la même transaction que la commande.
   La commande et son événement sont validés ensemble, ou annulés ensemble.
3. `OutboxRelay` (le « facteur ») passe toutes les secondes, publie dans Kafka les lignes où `published_at` est vide,
   dans l'ordre, puis les marque publiées. Si Kafka est indisponible, les lignes attendent le passage suivant.

Le domaine, le port et le service **n'ont pas changé** : seul l'adaptateur derrière `EventPublisher` a été remplacé.

**Voir l'Outbox fonctionner** :

1. Arrêter Kafka : `docker compose stop`.
2. Créer une commande depuis Swagger : réponse 201.
3. Console H2 : `SELECT * FROM OUTBOX_EVENTS;` montre l'événement avec `PUBLISHED_AT` vide.
   Les logs affichent « Kafka indisponible : l'événement ... sera republié au prochain passage ».
4. Redémarrer Kafka : `docker compose start`. Au passage suivant, l'événement part et `PUBLISHED_AT` se remplit.

**Comparer avec la version naïve** : mettre `app.events.publication: direct` dans `application.yml`.
`KafkaEventPublisher` publie alors directement ; refaire le même scénario : l'événement est perdu.

**Limites à connaître** :

- Livraison **au moins une fois** : si l'application plante entre l'envoi et la mise à jour de `published_at`,
  l'événement est renvoyé. Les consommateurs doivent être idempotents.
- **Plusieurs instances** : chacune ferait tourner le relais et publierait les mêmes lignes.
  En production : `SELECT ... FOR UPDATE SKIP LOCKED`, ShedLock, ou **Debezium**, qui lit le journal de la base
  et remplace complètement le relais.
- **Purge** : les lignes publiées s'accumulent ; prévoir une purge régulière (par exemple après 7 jours).
