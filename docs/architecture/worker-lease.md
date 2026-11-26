# Lease e fencing do worker

## Semântica do lease

- `claimNext` promove o job a `RUNNING`, define `lease_owner`, `lease_until` e incrementa `fence_token`.
- **Expiração do lease não invalida sozinha o worker em memória** — o processo antigo ainda pode estar executando HTTP.
- Expiração **habilita nova reivindicação** por outro worker (`RUNNING` + `lease_until < now`).
- Proteção financeira: o worker antigo é barrado por `lockOwnedFence` / `fence_token` na mesma transação das mutações; se o mark de sucesso falhar, a transação faz rollback (`StaleFenceException`).

## Implicação

Timeout de lease ≠ rejeição de negócio. Timeout de poll ≠ rejeição. Apenas confirmação/rejeição verificadas da Central alteram efeito financeiro.
