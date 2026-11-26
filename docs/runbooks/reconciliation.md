# Runbook — reconciliação operacional

Quando `processing.status = REQUIRES_REVIEW` e o status de negócio permanece `PENDENTE`.

## Sinais
- `errorCode` tipado (`CENTRAL_UNAVAILABLE`, `POLL_DEADLINE_EXCEEDED`, `HMAC_FAILURE`, …)
- `centralProtocol` presente ou ausente na GET da operação
- Eventos `INTEGRATION_FAILURE` na timeline

## Procedimento seguro
1. Correlacionar `correlationId` nos logs da API e da Central.
2. Se houver `centralProtocol`, consultar a Central (HMAC) sem reenviar portabilidade.
3. Se a Central estiver `CONFIRMADA`/`REJEITADA`, acionar reconciliação que aplica o resultado **uma única vez** (fencing / `saveOutcomeIfPending`).
4. Se a Central permanecer `PENDENTE`, aguardar ou escalar — **não** rejeitar localmente por timeout.
5. Só reabrir job após confirmar ownership do lease; nunca forçar `CONFIRMADA` sem evidência externa.
