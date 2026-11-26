# Runbook — reconciliação operacional

Quando `processing.status = REQUIRES_REVIEW` e o status de negócio permanece `PENDENTE`.

## O que existe hoje (executável)

| Capacidade | Como |
| --- | --- |
| Diagnóstico | `GET /api/v1/operacoes-portabilidade/{id}` → `processing`, `centralProtocol`, `correlationId` |
| Timeline | `GET .../eventos` (eventos `INTEGRATION_FAILURE` sanitizados) |
| Consulta Central | Worker `POLL_CENTRAL` / gateway HMAC (mesmo protocolo; **não** recria portabilidade) |
| Aplicação única do resultado | `confirmWithFence` / `rejectWithFence` + `saveOutcomeIfPending` (somente se fence válido) |
| Reset de lab (demo) | `./scripts/lab-reset-demo-parcels.sh` — **não** é reconciliação financeira |

## Procedimento proposto (ainda sem ferramenta dedicada de ops)

1. Correlacionar `correlationId` nos logs da API e da Central.
2. Se houver `centralProtocol`, consultar a Central (HMAC) sem reenviar a operação.
3. Se **não** houver `centralProtocol` mas a intenção já foi enviada (job `SUBMIT_CENTRAL` / falha após HTTP), reenviar **somente** com a mesma `Idempotency-Key` e o mesmo corpo — a Central devolve o protocolo original (fingerprint). Nunca gerar nova chave para “destravar”.
4. Se a Central estiver `CONFIRMADA`/`REJEITADA`, o caminho seguro é um job/poll que reaplique o resultado **uma vez** sob fencing — hoje isso ocorre ao rearmar o job com lease válido, não via endpoint admin.
5. Se a Central permanecer `PENDENTE`, aguardar ou escalar.

## Lab reset vs reconciliação

`lab-reset-demo-parcels.sh` and `docker compose down -v` are **disposable-lab only**. They are not recovery for persistent financial environments. Flyway `V8` historically reopened demo parcels on migrate; `V9` is a no-op policy marker and **does not undo** V8 effects on databases that already applied V8.

## Intervenção humana permitida
- Inspecionar evidências (`correlationId`, protocolo, timeline, logs).
- Reabrir **apenas** jobs de laboratório após confirmar que o lease expirou e não há worker ativo.
- Executar `lab-reset-demo-parcels.sh` em Compose local consciente do risco.

## Operações proibidas
- Forçar `CONFIRMADA`/`REJEITADA` no banco da plataforma sem evidência da Central.
- Reenviar portabilidade com nova chave de idempotência para “destravar” a mesma intenção financeira.
- Usar migrations Flyway para reabrir parcelas em ambientes com histórico real.
- Tratar timeout / `REQUIRES_REVIEW` como rejeição de negócio.

## Evidências mínimas para retomar processamento
- Protocolo Central e status terminal verificados **ou** confirmação de que a Central ainda está pendente.
- Job em `EXHAUSTED`/`FAILED` sem lease ativo (ou lease expirado).
- Nenhum outro worker claimando o mesmo `fence_token`.
