---
name: accessibility
description: Support accessible UI. Use when the task touches this engineering concern.
---
# accessibility

## Objective
Support accessible UI.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Semantic labels, focus management, keyboard navigation, live status announcements and contrast.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Automated and manual accessibility checks. Do not claim success without execution evidence.
