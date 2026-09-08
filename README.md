# CondoEconomy API

O coração da plataforma CondoEconomy. Esta é uma API RESTful robusta construída em Java com Spring Boot, responsável por gerenciar toda a lógica de negócios, segurança e comunicação em tempo real do ecossistema condominial.

## Tecnologias Utilizadas

- **Java 21**
- **Spring Boot 3.3.x**
- **Spring Security** com JWT (JSON Web Tokens) para Autenticação
- **Spring Data JPA** e Hibernate para Persistência de Dados
- **PostgreSQL** como Banco de Dados Relacional
- **Spring WebSockets (STOMP)** para comunicação e notificações em Tempo Real
- **Maven** para gerenciamento de dependências

## Segurança e Arquitetura

O sistema implementa autenticação via token JWT, garantindo que rotas protegidas só sejam acessadas por usuários autorizados. O Spring Security foi configurado com permissões estritas baseadas em perfis (Roles):
- `ROLE_SINDICO`: Acesso total à gestão financeira, aprovação de reservas e relatórios.
- `ROLE_PORTEIRO`: Permissão para manipular chegadas/saídas de encomendas e visitantes.
- `ROLE_MORADOR`: Acesso restrito aos próprios dados (seus boletos, seus chamados, suas reservas).

## Funcionalidades Principais (Endpoints)

- **Auth:** `/api/v1/auth/login` - Validação de credenciais e geração do JWT.
- **Finanças:** Emissão e gestão de status de Boletos, além de métricas de inadimplência para o dashboard.
- **Reservas:** Motor de agendamento de áreas comuns (com bloqueio de horários conflitantes).
- **Ouvidoria (Chamados):** Abertura, atualização de status e escalonamento de tickets de manutenção/reclamações.
- **Portaria:** Registro de visitantes e fluxo de encomendas (Aguardando Retirada -> Entregue).
- **WebSockets:** `/ws` - Canal em tempo real para sincronização instantânea de novos chamados e aprovações de reservas entre a tela do morador e a tela do síndico/portaria.

## Como Executar o Servidor

### Pré-requisitos
- Java JDK 21
- Maven instalado
- Banco de Dados PostgreSQL rodando localmente (porta padrão `5432` ou conforme configurado)

### Passo a Passo

1. Crie o banco de dados PostgreSQL:
```sql
CREATE DATABASE condoeconomy;
```

2. Se necessário, ajuste as credenciais do banco de dados no arquivo `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/condoeconomy
spring.datasource.username=postgres
spring.datasource.password=sua_senha
```

3. Compile e rode o projeto via Maven:
```bash
mvn clean install
mvn spring-boot:run
```

O servidor iniciará localmente na porta padrão **8080**.

---
*Back-end otimizado para escalabilidade, segurança e alta performance.*
