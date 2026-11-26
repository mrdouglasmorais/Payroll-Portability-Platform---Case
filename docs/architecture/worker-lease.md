# Lease e fencing do worker

## Semântica do lease

- `claimNext` promove o job a `RUNNING`, define `lease_owner`, `lease_until` e incrementa `fence_token`.
- **Expiração do lease não invalida sozinha o worker em memória** — o processo antigo ainda pode estar executando HTTP.
- Expiração **habilita nova reivindicação** por outro worker (`RUNNING` + `lease_until < now`).
- Proteção financeira: o worker antigo é barrado por `lockOwnedFence` / `fence_token` na mesma transação das mutações; se o mark de sucesso falhar, a transação faz rollback (`StaleFenceException`).

## Implicação

Timeout de lease ≠ rejeição de negócio. Timeout de poll ≠ rejeição. Apenas confirmação/rejeição verificadas da Central alteram efeito financeiro.

## Janela crítica pós-aceite da Central

Entre a resposta `202`+protocolo da Central e o `attachProtocol` local existe uma janela em que a Central já aceitou e a plataforma ainda não persistiu o protocolo.

- `process-kill-recovery.sh` cobre SIGKILL **depois** da persistência local.
- `timeout-recovery.sh` simula perda do protocolo local e recuperação via mesma `Idempotency-Key`.
- Interromper o processo exatamente nessa janela de forma determinística exigiria hooks de teste em produção — fora de escopo. Mitigação: replay idempotente na Central (mesmo protocolo) + [runbook de reconciliação](../runbooks/reconciliation.md).
