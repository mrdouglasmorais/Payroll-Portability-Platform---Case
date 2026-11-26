# Idempotência e integridade de mensagem

## Duas responsabilidades distintas

| Mecanismo | Responsabilidade | Implementação atual |
| --- | --- | --- |
| HMAC canônico | Integridade da mensagem **efetivamente transmitida** (bytes + headers) | `HmacCanonical` request/response |
| Fingerprint de idempotência | Identidade da intenção financeira | Plataforma: hash canônico de campos; Central: SHA-256 do **corpo bruto** |

## Contrato da Central (lab)

A Central calcula `request_hash = sha256(rawBody UTF-8)`.

Consequência: dois JSONs semanticamente iguais com espaços/ordem de chaves diferentes geram hashes distintos e **não** fazem replay automático — podem criar conflito ou nova operação se a chave também diferir.

**Política explícita do laboratório:** reenvios usam o mesmo corpo **byte a byte** (o worker da plataforma serializa de forma estável via `LinkedHashMap` + Jackson). Produção deveria migrar o fingerprint da Central para um hash canônico de campos, separado do HMAC.

## HMAC em respostas de erro

- Se a resposta de erro trouxer `X-Signature` (+ headers), a plataforma **verifica** a assinatura.
- Se a resposta de erro **não** for assinada, a plataforma confia apenas na **classe HTTP** (4xx/5xx/429) e não no corpo — política de laboratório documentada; produção deve exigir assinatura também em erros.
