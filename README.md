⚠️ **Work in progress** — this README is still being finalized and may change as Graphite evolves.

# Graphite

**A tiny and relaxed dependency injection container for Kotlin.**

**Graphite** is a simple and lightweight dependency injection library for Kotlin that takes inspiration from both Guice and Spring. It combines the **tiny, explicit injector architecture of Guice** with the **ergonomic, relaxed wiring style of Spring**. You get a small, predictable core where dependencies are resolved via a topologically sorted graph, but with minimal boilerplate: concrete classes are automatically bound to all their supertypes, collections of dependencies are injected natively, and optional classpath scanning lets you wire up components with Spring-style annotations if you want.

In other words, Graphite is **small, fast, and easy to use**, while still supporting the kinds of convenient autowiring patterns that make Spring popular — all without forcing you into verbose module definitions.

---
## Features
- Tiny core DI container - simple, predictable and fast.
- Constructor and factory method injection.
- Automatic binding to all supertypes of a class.
- Optional classpath scanning for Spring-style autowiring.
- `@Primary` support for resolving ambiguity.
- Fully Kotlin-idiomatic API.
- Topologically sorted dependency graph ensures order-independent bindings.

## Roadmap
- Automatic collection injection (`List<T>` / `Set<T>`).
---
