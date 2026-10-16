# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- `CLAUDE.md`: sibling repositories referenced by relative path (`../docs/`) instead of an absolute WSL path

## [0.1.0] - 2026-05-08
### Added

- Initial implementation of the MVVM (Model-View-ViewModel) and Clean Architecture
- Home screen displaying the calculated current balance
- List of transactions sorted in descending order by date
- Screen for recording new transactions (income and expenses)
- Screen navigation using Navigation Compose
- Local transaction persistence using Room
- Synchronization of pending local transactions with REST API
- Visual indicator of server connection status (green/yellow)
- Automatic health check upon app launch
- Calculation of total balance by adding income and subtracting expenses
- Dependency injection with Hilt
- MVVM + Clean Architecture with separate use cases
- Unit test for `getTotalAmount` with MockK
