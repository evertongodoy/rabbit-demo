# 🐇 rabbit-demo — Spring Boot + RabbitMQ

Microsserviço de demonstração que ilustra como publicar e consumir mensagens usando **RabbitMQ** com **Spring Boot 3** e **Java 21**.

---

## 📋 Índice

- [O que é RabbitMQ?](#o-que-é-rabbitmq)
- [Conceitos fundamentais](#conceitos-fundamentais)
- [Arquitetura deste projeto](#arquitetura-deste-projeto)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Configurações RabbitMQ](#configurações-rabbitmq)
- [Fluxo de mensagens](#fluxo-de-mensagens)
- [Como executar](#como-executar)
- [Testando a API](#testando-a-api)
- [Monitorando pelo Management UI](#monitorando-pelo-management-ui)

---

## O que é RabbitMQ?

**RabbitMQ** é um **message broker** (intermediário de mensagens) de código aberto.  
Ele permite que sistemas se comuniquem de forma **assíncrona e desacoplada** — o serviço que envia a mensagem não precisa saber quem vai recebê-la, nem aguardar a resposta.

> Imagine um correio: você deposita uma carta (mensagem) numa caixa postal (fila). O destinatário retira quando puder, sem que você precise esperar.

---

## Conceitos fundamentais

| Conceito | O que é |
|---|---|
| **Producer** | Quem **publica** a mensagem no broker |
| **Consumer** | Quem **consome** (lê) a mensagem da fila |
| **Exchange** | "Roteador" que recebe a mensagem do Producer e decide para qual fila encaminhar |
| **Queue** | Fila onde as mensagens ficam armazenadas até serem consumidas |
| **Routing Key** | Chave usada pelo Exchange para rotear a mensagem para a fila correta |
| **Binding** | Vínculo entre um Exchange e uma Queue, associado a uma Routing Key |
| **Direct Exchange** | Tipo de Exchange que entrega a mensagem na fila cuja Routing Key bate exatamente com a informada na publicação |

### Como o roteamento funciona (Direct Exchange)

```
Producer
   │
   │  publica com routingKey = "demo.routing.key-1"
   ▼
[ demo.exchange ]  (Direct Exchange)
   │
   ├── binding: routingKey = "demo.routing.key-1"  ──▶  [ demo.queue-1 ]  ──▶  Consumer 1
   │
   └── binding: routingKey = "demo.routing.key-2"  ──▶  [ demo.queue-2 ]  ──▶  Consumer 2
```

O Exchange analisa a `routingKey` da mensagem e a envia **apenas** para a fila que possui o binding correspondente.

---

## Arquitetura deste projeto

```
HTTP Client
    │
    │  POST /api/messages/queue/{numQueue}
    ▼
MessageController          ← recebe a requisição REST
    │
    ▼
MessageProducer            ← publica no RabbitMQ usando RabbitTemplate
    │
    │  exchange = demo.exchange
    │  routingKey = demo.routing.key-1  (numQueue == 1)
    │              demo.routing.key-2  (numQueue != 1)
    ▼
[ demo.exchange ] ─── Direct Exchange ───┐
                                         ├── demo.queue-1 ──▶ MessageListener#receiveMessage1
                                         └── demo.queue-2 ──▶ MessageListener#receiveMessage2
```

---

## Estrutura do projeto

```
rabbit-demo/
├── compose/
│   ├── docker-compose.yml                   # Sobe o RabbitMQ via Docker
│   └── rabbitmq-init/
│       ├── rabbitmq.conf                    # Aponta para o arquivo de definições
│       └── rabbitmq-definitions.json        # Exchange, Queues e Bindings pré-criados
│
├── src/main/java/com/demo/rabbit/
│   ├── RabbitDemoApplication.java           # Entrypoint Spring Boot
│   ├── config/
│   │   └── RabbitMQConfig.java             # Beans: Queue, Exchange, Binding, RabbitTemplate
│   ├── controller/
│   │   └── MessageController.java          # Endpoint POST /api/messages/queue/{numQueue}
│   └── messaging/
│       ├── MessageProducer.java            # Publica mensagens no RabbitMQ
│       └── MessageListener.java           # Consome mensagens das filas e loga no console
│
├── src/main/resources/
│   └── application.yml                     # Configurações do Spring e do RabbitMQ
│
└── pom.xml                                  # Dependências Maven (Spring AMQP, Web, Lombok)
```

### Descrição de cada arquivo

| Arquivo | Responsabilidade |
|---|---|
| `docker-compose.yml` | Sobe o container `rabbitmq:3.13-management`, expõe as portas AMQP (`5672`) e Management UI (`15672`), e monta os arquivos de configuração |
| `rabbitmq.conf` | Instrui o RabbitMQ a carregar as definições do arquivo JSON na inicialização |
| `rabbitmq-definitions.json` | Define previamente o Exchange, as duas Queues e os Bindings — o broker já está configurado ao subir |
| `RabbitMQConfig.java` | Declara os Beans Spring: as duas filas, o exchange, os dois bindings e o `RabbitTemplate` com conversor JSON |
| `MessageController.java` | Recebe requisições HTTP `POST` e delega ao `MessageProducer` |
| `MessageProducer.java` | Usa o `RabbitTemplate` para publicar no exchange com a routing key correta conforme o `numQueue` informado |
| `MessageListener.java` | Possui dois `@RabbitListener`, um por fila, que consomem as mensagens e as exibem no log |
| `application.yml` | Define host/porta/credenciais do RabbitMQ e os nomes de exchange, queues e routing keys |

---

## Configurações RabbitMQ

### Topologia (definida em `rabbitmq-definitions.json` e espelhada em `RabbitMQConfig.java`)

| Recurso | Valor | Tipo |
|---|---|---|
| **Exchange** | `demo.exchange` | Direct |
| **Queue 1** | `demo.queue-1` | Durable |
| **Queue 2** | `demo.queue-2` | Durable |
| **Routing Key 1** | `demo.routing.key-1` | bind: exchange → queue-1 |
| **Routing Key 2** | `demo.routing.key-2` | bind: exchange → queue-2 |

### Bindings

```
demo.exchange
    ├── demo.routing.key-1  ──▶  demo.queue-1
    └── demo.routing.key-2  ──▶  demo.queue-2
```

### Conexão (definida em `application.yml`)

| Propriedade | Valor |
|---|---|
| Host | `localhost` |
| Porta AMQP | `5672` |
| Usuário | `guest` |
| Senha | `guest` |
| Virtual Host | `/` |

---

## Fluxo de mensagens

### Publicando na Queue 1 (`numQueue = 1`)

```
POST /api/messages/queue/1
Body: "Olá Queue 1!"

  MessageController
       │
       ▼
  MessageProducer
    rabbitTemplate.convertAndSend("demo.exchange", "demo.routing.key-1", "Olá Queue 1!")
       │
       ▼
  demo.exchange  →  routing key: demo.routing.key-1
       │
       ▼
  demo.queue-1
       │
       ▼
  MessageListener#receiveMessage1
    LOG: "Mensagem recebida do RabbitMQ - rabbitmq.queue-1 : Olá Queue 1!"
```

### Publicando na Queue 2 (`numQueue = 2`)

```
POST /api/messages/queue/2
Body: "Olá Queue 2!"

  MessageController
       │
       ▼
  MessageProducer
    rabbitTemplate.convertAndSend("demo.exchange", "demo.routing.key-2", "Olá Queue 2!")
       │
       ▼
  demo.exchange  →  routing key: demo.routing.key-2
       │
       ▼
  demo.queue-2
       │
       ▼
  MessageListener#receiveMessage2
    LOG: "Mensagem recebida do RabbitMQ - rabbitmq.queue-2 : Olá Queue 2!"
```

> ⚠️ **Atenção:** A separação das filas é feita pela **routing key**.  
> Se você publicar com `routingKey-2`, o consumer da `queue-1` **não receberá** a mensagem — somente o consumer da `queue-2` consome.

---

## Como executar

### Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker + Docker Compose

### 1. Subir o RabbitMQ

```bash
cd compose
docker compose up -d
```

O RabbitMQ já subirá com o Exchange, as Queues e os Bindings criados automaticamente via `rabbitmq-definitions.json`.

### 2. Compilar e executar a aplicação

```bash
# Na raiz do projeto
mvn spring-boot:run
```

Ou usando o JAR gerado:

```bash
mvn clean package
java -jar target/rabbit-demo-0.0.1-SNAPSHOT.jar
```

A aplicação estará disponível em: `http://localhost:8080`

---

## Testando a API

### Endpoint

```
POST /api/messages/queue/{numQueue}
Content-Type: application/json
Body: "sua mensagem aqui"
```

| `numQueue` | Routing Key usada | Fila de destino |
|---|---|---|
| `1` | `demo.routing.key-1` | `demo.queue-1` |
| `2` (ou qualquer outro valor) | `demo.routing.key-2` | `demo.queue-2` |

### Exemplos com cURL

```bash
# Publica na demo.queue-1
curl -X POST http://localhost:8080/api/messages/queue/1 \
  -H "Content-Type: application/json" \
  -d '"Mensagem para a fila 1"'

# Publica na demo.queue-2
curl -X POST http://localhost:8080/api/messages/queue/2 \
  -H "Content-Type: application/json" \
  -d '"Mensagem para a fila 2"'
```

### Resposta esperada

```
Mensagem enviada ao RabbitMQ com sucesso: Mensagem para a fila 1
```

### Log esperado no console da aplicação

```
INFO  MessageController  - Requisição recebida na Controller | message="Mensagem para a fila 1"
INFO  MessageProducer    - Publicando mensagem no RabbitMQ | exchange=demo.exchange | message="Mensagem para a fila 1"
INFO  MessageProducer    - Mensagem publicada com sucesso.
INFO  MessageListener    - ============================================
INFO  MessageListener    - Mensagem recebida do RabbitMQ - rabbitmq.queue-1 : Mensagem para a fila 1
INFO  MessageListener    - ============================================
```

---

## Monitorando pelo Management UI

Acesse **http://localhost:15672** com as credenciais `guest / guest`.

No painel você pode:

| Aba | O que monitorar |
|---|---|
| **Overview** | Status geral do broker, taxa de mensagens |
| **Exchanges** | Ver o `demo.exchange` e seus bindings |
| **Queues** | Ver `demo.queue-1` e `demo.queue-2`, mensagens pendentes e consumidas |
| **Connections / Channels** | Ver a conexão ativa da aplicação Spring Boot |

### Visualizando os Bindings do Exchange

1. Clique em **Exchanges** → `demo.exchange`
2. Expanda a seção **Bindings**
3. Você verá:

```
demo.routing.key-1  →  demo.queue-1
demo.routing.key-2  →  demo.queue-2
```

---

## Stack utilizada

| Tecnologia | Versão | Papel |
|---|---|---|
| Java | 21 | Linguagem |
| Spring Boot | 3.3.0 | Framework principal |
| Spring AMQP | (via Boot) | Integração com RabbitMQ |
| RabbitMQ | 3.13 | Message Broker |
| Lombok | (via Boot) | Redução de boilerplate |
| Docker Compose | — | Infraestrutura local |

