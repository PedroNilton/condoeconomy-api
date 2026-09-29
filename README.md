# HabitOS API

O coração da plataforma HabitOS (anteriormente CondoEconomy). Esta é uma API RESTful robusta construída em Java com Spring Boot, responsável por gerenciar toda a lógica de negócios, segurança e comunicação em tempo real do ecossistema condominial.

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.3.x**
- **Spring Security** com JWT (JSON Web Tokens) para Autenticação
- **Spring Data JPA** e Hibernate para Persistência de Dados
- **PostgreSQL** via Docker Compose
- **Spring WebSockets (STOMP)** para comunicação e notificações em Tempo Real
- **Maven** para gerenciamento de dependências

## Segurança e Arquitetura

O sistema implementa autenticação via token JWT, garantindo que rotas protegidas só sejam acessadas por usuários autorizados. O Spring Security foi configurado com permissões estritas baseadas em perfis (Roles):
- `ROLE_SINDICO`: Acesso total à gestão financeira, aprovação de reservas e relatórios.
- `ROLE_PORTEIRO`: Permissão para manipular chegadas/saídas de encomendas e visitantes.
- `ROLE_MORADOR`: Acesso restrito aos próprios dados (seus boletos, seus chamados, suas reservas e dependentes).

## Funcionalidades Principais (Endpoints)

- **Auth:** `/api/v1/auth/login` - Validação de credenciais e geração do JWT.
- **Finanças:** Emissão e gestão de status de Boletos, além de métricas de inadimplência para o dashboard.
- **Reservas:** Motor de agendamento de áreas comuns (com bloqueio de horários conflitantes).
- **Ouvidoria (Chamados):** Abertura, atualização de status e escalonamento de tickets de manutenção/reclamações.
- **Portaria:** Registro de visitantes e fluxo de encomendas (Aguardando Retirada -> Entregue).
- **Perfil do Morador:** CRUD seguro para pets, veículos, moradores adicionais e controle detalhado de notificações e sessões ativas.
- **WebSockets:** `/ws` - Canal em tempo real para sincronização instantânea.

## Como Executar o Servidor

### Pré-requisitos
- Java JDK 21
- Maven instalado
- Docker e Docker Compose (para subir o banco de dados)

### Passo a Passo

1. Suba o container do banco de dados (PostgreSQL):
```bash
docker-compose up -d
```

2. O sistema utilizará as configurações contidas em `application.properties` (porta 5433 no container).

3. Compile e rode o projeto via Maven:
```bash
mvn spring-boot:run
```

O servidor iniciará localmente na porta padrão **8080**.

---
*Back-end otimizado para escalabilidade, segurança e alta performance.*
