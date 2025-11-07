# Contributing to Grove

Thank you for your interest in contributing to Grove! We welcome contributions from the community and are grateful for your help making this project better.

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

## Getting Started

### Prerequisites

- Java 21+
- Gradle 8.14.3+ (included via `gradlew`)
- Kotlin 1.9.25+
- Git

### Building the Project

```bash
./gradlew build
```

### Running Locally

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080` with:
- npm registry: `/npm`
- PyPI registry: `/pypi`

## How to Contribute

### Reporting Issues

We use GitHub Issues for tracking bugs and feature requests. Before opening an issue:

1. **Check existing issues** - Avoid duplicates by searching open and closed issues
2. **Provide context** - Include:
   - Your environment (Java version, OS, etc.)
   - Steps to reproduce the issue
   - Expected vs. actual behavior
   - Relevant logs or error messages

### Submitting Pull Requests

1. **Fork and create a branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes**
   - Keep commits atomic and well-documented
   - Write clear commit messages
   - Add tests for new functionality

3. **Test thoroughly**
   ```bash
   ./gradlew test
   ./gradlew check
   ```

4. **Follow code conventions**
   - Use Kotlin idioms and style conventions
   - Keep functions focused and testable
   - Document public APIs with comments
   - Format code consistently (Gradle's Kotlin plugin helps)

5. **Ensure license compliance**
   - All contributions must be licensed under Apache 2.0
   - Include SPDX license headers in new files:
     ```kotlin
     // SPDX-License-Identifier: Apache-2.0
     // Copyright (c) 2024 Grove Contributors
     ```

6. **Push and open a PR**
   ```bash
   git push origin feature/your-feature-name
   ```

### Pull Request Guidelines

- **Title**: Clear, descriptive summary (e.g., "Add support for npm scoped packages")
- **Description**: Explain the problem, solution, and why this change is needed
- **Scope**: Keep PRs focused on a single concern
- **Tests**: Include tests for bug fixes and new features
- **Documentation**: Update README or inline docs if functionality is user-facing

## Legal Requirements

### Contributor License Agreement

By contributing to Grove, you agree that:

1. **Your contributions are licensed under Apache 2.0** - You grant Grove and its users the rights to use your contribution under the Apache License 2.0
2. **You own or have rights to the code** - You warrant that your contribution is your own original work or you have the necessary rights to grant the above license
3. **You are not bound by conflicting licenses** - Your contribution does not violate any third-party rights

### Copyright

- Individual contributions retain the copyright of their authors
- The Grove project collectively benefits from all contributions under the Apache 2.0 license
- Contributors are acknowledged in the NOTICE file and commit history

## Development Guidelines

### Code Quality

- **Maintainability**: Write code that is easy to understand and modify
- **Testing**: Aim for reasonable test coverage; new features should have corresponding tests
- **Documentation**: Comment non-obvious logic; keep docs current
- **Performance**: Consider performance implications of changes

### Project Structure

```
src/
├── main/
│   ├── kotlin/ltd/gsio/grove/
│   │   ├── GroveApplication.kt          # Main Spring Boot application
│   │   ├── config/                      # Configuration classes
│   │   ├── npm/                         # npm registry implementation
│   │   ├── pypi/                        # PyPI registry implementation
│   │   └── util/                        # Shared utilities
│   └── resources/
│       └── application.properties       # Default configuration
└── test/
    └── kotlin/ltd/gsio/grove/           # Unit and integration tests
```

### Known Limitations & Future Work

See [README.md](README.md) for known limitations. Areas for contribution:

- Authentication and authorization mechanisms
- Additional package metadata support (npm scoped packages, PyPI metadata API)
- Improved error handling and validation
- Performance optimizations for large registries
- Security hardening
- Enhanced testing coverage
- Documentation improvements

## Communication

- **Issues**: Use GitHub Issues for bugs and features
- **Discussions**: Use GitHub Discussions for questions and design discussions
- **Community**: Be respectful and constructive in all interactions

## Recognition

Contributors are recognized in:
- Commit history
- [NOTICE](NOTICE) file
- Release notes
- Project documentation

## Questions?

- Review existing [issues](https://github.com/gsio-ltd/grove/issues) and [discussions](https://github.com/gsio-ltd/grove/discussions)
- Check the [README](README.md) for usage documentation
- Ask in GitHub Discussions for general questions

---

**Thank you for contributing to Grove!** We appreciate your time and effort in making this project better for everyone.
