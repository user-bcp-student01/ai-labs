---
name: feature-intake-spec-checklist
description: Estandariza la definición de funcionalidades antes de la implementación generando un paquete mínimo trazable que cubra alcance, decisiones, riesgos, criterios de aceptación y planes de validación.
---

# Skill: Feature Intake -> Spec Checklist

## 1) Metadata
- **Skill ID:** feature-intake-spec-checklist
- **Version:** 0.1.0
- **Purpose:** Estandarizar la definición de funcionalidades antes de la implementación generando un paquete mínimo trazable que cubra alcance, decisiones, riesgos, aceptación y validación.
- **Audience (roles):** Chapter/PO, Tech Lead, Backend, Frontend, QA, Cyber
- **When to use:** Antes de crear tareas de implementación o abrir un PR significativo.
- **When NOT to use:** Incidentes P1 de producción, cambios triviales como typos o tareas sin impacto funcional.
- **Sensitive inputs:** No incluyas datos reales de clientes, credenciales, secretos, tokens ni URLs internas no autorizadas.
- **Expected output:** Markdown estructurado mediante la sección Output a continuación.

## 2) Inputs
Proporciona la información en este formato:

- **Business context (brief):**
- **Feature request / user story:**
- **Expected impact (metric or outcome):**
- **Scope (in/out):**
- **Known technical constraints:** (stack, standards, performance, availability)
- **Known risks:** (security, compliance, stability)
- **Dependencies:** (services, teams, data)
- **Team Definition of Done (DoD):** (if available)

## 3) Output
Devuelve **solo** un documento Markdown con estas secciones y bullets:

1. **Summary**
2. **Scope**
    - Dentro del alcance
    - Fuera del alcance
3. **Supuestos**
4. **Decisiones técnicas por tomar (pendientes o resueltas)**
5. **Riesgos y mitigaciones**
6. **Criterios de aceptación (medibles)**
7. **Plan de validación**
    - Pruebas unitarias
    - Pruebas funcionales
    - Seguridad (comprobaciones mínimas)
    - Evidencia requerida (qué adjuntar)
8. **Tareas sugeridas por rol**
    - Chapter/PO
    - Tech Lead
    - Backend
    - Frontend
    - QA
    - Cyber
9. **Preguntas abiertas**

## 4) Constraints
- No inventes requisitos: si falta información, inclúyela bajo **Preguntas abiertas**.
- No propongas herramientas fuera del stack permitido.
- No incluyas datos sensibles ni secretos.
- Los criterios de aceptación deben ser verificables; no escribas “mejorar el rendimiento” sin una métrica.
- El plan de validación debe incluir al menos un elemento de evidencia concreto, como salida de pruebas, un checklist o un archivo actualizado.
- Mantén la salida en un máximo de 1-2 páginas para que siga siendo usable.

## 5) Validation Rules
La salida es válida si:
- Incluye todas las secciones definidas en Output.
- Tiene al menos 3 criterios de aceptación medibles.
- Tiene al menos 3 tareas para cada rol central: Tech Lead, Backend y QA, o explica por qué un rol no aplica.
- Incluye al menos 2 riesgos con mitigaciones.
- No contiene datos sensibles ni secretos.
- No contiene supuestos no declarados.

## 6) Evidence
Adjunta o referencia estos elementos en el PR o entrega:
- `SKILL.md` versionado.
- `examples/input.sample.md`.
- `examples/output.sample.md`.
- Tag de versión para el Skill, como `skill-feature-intake-v0.1.0`.