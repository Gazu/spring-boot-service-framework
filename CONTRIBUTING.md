# Contributing

Thank you for your interest in contributing to this project.

This repository is a Spring Boot service framework. Contributions should keep the
framework reusable, maintainable, and focused on shared backend infrastructure.

## How to Contribute

1. Fork the repository.
2. Create a branch from the default branch.
3. Make focused changes.
4. Run the relevant tests and checks.
5. Open a pull request with a clear description.

Use descriptive branch names:

```text
feat/rest-client-timeouts
fix/logging-correlation-id
docs/contributing-guidelines
```

## Pull Request Guidelines

- Keep pull requests small and focused.
- Explain what changed and why.
- Link related issues when available.
- Add or update tests for behavior changes.
- Update documentation when public APIs, configuration, or usage changes.
- Do not include unrelated refactors in the same pull request.

## Commit Message Rules

All commit messages must be written in English.

This project follows Conventional Commits. Use this format:

```text
<type>(optional-scope): <description>
```

Examples:

```text
feat(rest-client): add configurable connection timeout
fix(logging): preserve correlation id across filters
docs(readme): add local publishing instructions
test(mock): cover missing response file handling
refactor(commons): simplify notification factory
```

Common commit types:

- `feat`: a new feature
- `fix`: a bug fix
- `docs`: documentation-only changes
- `test`: adding or updating tests
- `refactor`: code changes that do not add features or fix bugs
- `build`: build system or dependency changes
- `ci`: CI/CD configuration changes
- `chore`: maintenance tasks

Breaking changes must be clearly marked:

```text
feat(rest-client)!: rename client configuration properties
```

Or include a footer:

```text
BREAKING CHANGE: rest client timeout properties were renamed.
```

## Versioning

This project follows Semantic Versioning:

```text
MAJOR.MINOR.PATCH
```

Version changes must respect the following rules:

- Increment `MAJOR` for breaking changes.
- Increment `MINOR` for backward-compatible features.
- Increment `PATCH` for backward-compatible bug fixes.
- Pre-release or snapshot versions may be used for development builds.

Every released version must have a Git tag.

Tags must use this format:

```text
vMAJOR.MINOR.PATCH
```

Examples:

```text
v1.0.0
v1.1.0
v1.1.1
```

A release pull request should include:

- the version update;
- release notes or changelog updates, when available;
- a tag created for the final released version.

## Code Style

- Follow the existing project structure and naming conventions.
- Keep core modules independent from Spring and infrastructure libraries unless
  the module is explicitly a Spring Boot starter.
- Prefer clear, small APIs over broad abstractions.
- Keep business-specific logic out of the framework.

## Tests

Before opening a pull request, run the relevant checks:

```bash
./gradlew test
```

For broader changes, run the full build:

```bash
./gradlew build
```

## Code of Conduct

All contributors are expected to follow the
[Code of Conduct](CODE_OF_CONDUCT.md).
