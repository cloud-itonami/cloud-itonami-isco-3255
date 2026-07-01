# cloud-itonami-isco-3255

Open Occupation Blueprint for **ISCO-08 3255**: Physiotherapy Technicians and Assistants.

This repository designs a forkable OSS business for an independent physiotherapy technician/assistant: a mobility-assist robot supports exercise-equipment setup under a governor-gated actor, so the practice keeps its own session and consent records instead of renting a closed clinic-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a mobility-assist robot supports exercise-equipment setup and supervised movement-assist tasks under an actor that proposes
actions and an independent **Physiotherapy Support Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
direct physical manipulation of a patient's body, or exercises involving fall risk) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client consent + treatment plan + clinical protocol
        |
        v
Physiotherapy Support Advisor -> Physiotherapy Support Governor -> assist/monitor, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3255`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
