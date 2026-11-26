# Visão geral da arquitetura

**Documento balizador:** [system-design.md](system-design.md) — abra o Miro pelo [frame 00 · Mapa de navegação](https://miro.com/app/board/uXjVEJ1cEDw=/?share_link_id=843643210573&moveToWidget=3458764691629361954).

Este overview é o resumo executável no repositório; decisões devem rastrear o Miro. Idioma: português (termos técnicos: C4, HMAC, etc.).

```mermaid
flowchart LR
  U[Operador] --> W[React web]
  W --> A[Portability API]
  A --> D[(PostgreSQL plataforma)]
  J[Worker persistente na API] --> D
  J -->|HTTP assinado| C[Central simulada]
  A -->|Consentimento e agenda assinados| C
  C --> E[(PostgreSQL Central)]
```

Módulos: consentimento, agenda, portabilidade, liquidação, auditoria; integração e shared são suporte. Empacotar por feature; domínio/aplicação/adaptadores só onde necessário. Sem acesso cruzado direto à persistência entre módulos. Liquidação pode permanecer dentro de portabilidade enquanto não houver ciclo de vida independente.

```mermaid
sequenceDiagram
  participant W as Web
  participant A as API
  participant D as DB plataforma
  participant J as Worker
  participant C as Central
  W->>A: POST com chave de idempotência
  A->>D: Operação + reserva + evento + job (atômico)
  A-->>W: 202 + operationId
  J->>C: POST assinado com chave externa estável
  C-->>J: 202 + protocolo
  J->>C: GET status
  C-->>J: PENDENTE ou terminal
  J->>D: Confirma ou rejeita + auditoria
```

## Fronteiras

- A UI só fala com a API da plataforma.
- A Central é um processo e um banco separados.
- Autorização e agenda entram pela API da plataforma (HMAC para a Central).
- O worker envia e consulta portabilidades; a UI nunca chama a Central.

## Decisões que exigem ADR

- Bancos separados para plataforma e Central.
- Jobs persistentes no PostgreSQL (sem broker no lab).
- Polling do status na Central (sem webhook no contrato do desafio).
- Conta efetiva só muda após confirmação verificada.
