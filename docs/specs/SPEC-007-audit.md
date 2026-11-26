# SPEC-007-audit

Status: Proposed

Source mapping: RF5/RNF5. Identifiers assigned locally, not in original challenge.

## Behavior
Append events for creation, protocol receipt, failure and state transitions, with ordered sequence, correlation, UTC timestamp and redacted details.

## Acceptance
State and event commit together; prior events remain unchanged.

## Evidence
`operation_events` append-only; create atomic tests; dual-stack `/eventos`; fenced failure append (`appendEventsIfNonTerminal`). Re-verify under AI-012.
