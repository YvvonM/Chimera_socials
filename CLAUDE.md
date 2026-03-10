# Project Chimera - AI Agent Rules

## Project Context
This is Project Chimera - an autonomous AI influencers' platform built in Java 21+. Read specs/ before doing anything.

## Prime Directive
NEVER generate code without checking specs/ first.
Order of reading:
1. specs/_meta.md
2. specs/functional.md
3. specs/technical.md

## Java Directives
- Java 21+ only
- Use Records for ALL DTOs: AgentTask, WorkerResult, JudgeVerdict
- Use JUnit 5 for ALL tests
- Use Virtual Threads for concurrency
- No mutable POJOs
- No raw Maps for agent payloads

## Traceability
Before writing ANY code:
1. State which User Story you are implementing
2. Reference the exact User_Story ID
3. Reference the Contract ID from technical.md
4. Explain your plan
5. Then write the code


## Architecture Rules
- ALL external calls go through MCP only
- Direct API calls are strictly prohibited
- Workers are stateless. Never store state
- Workers never communicate with each other
- SOUL.md is read-only. Never modify it
- Every output must include confidence_score