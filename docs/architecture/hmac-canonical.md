# HMAC canonical format (M3)

Algorithm: HMAC-SHA256 over UTF-8 bytes. Signature transmitted as lowercase hex in `X-Signature`.

## Request canonical string

```text
{METHOD}\n
{PATH}\n
{QUERY}\n
{sha256Hex(rawBody)}\n
{X-Timestamp}\n
{X-Nonce}\n
{X-Key-Id}\n
{X-Correlation-Id}\n
{Idempotency-Key or empty}
```

- `PATH` is the raw path without query (e.g. `/operacoes-portabilidade`).
- `QUERY` is the raw query string without `?`, or empty.
- Empty body uses SHA-256 of zero-length bytes.
- Timestamp is epoch milliseconds. Max skew: 5 minutes.
- Nonces are single-use within the skew window (replay rejected).

## Response canonical string

```text
{METHOD}\n
{PATH}\n
{QUERY}\n
{sha256Hex(rawResponseBody)}\n
{X-Timestamp}\n
{X-Nonce}\n
{X-Key-Id}\n
{X-Correlation-Id}\n
{requestNonce}\n
{HTTP status code}
```

Responses bind the original request nonce so a swapped response cannot be accepted as belonging to another call.
