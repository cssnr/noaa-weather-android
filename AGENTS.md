# Agent Guide

Android client for [NOAA Weather](https://www.weather.gov/).

- `app/` - Android app source (Kotlin, Gradle)
- `Taskfile.yml` - task commands (go-task/task)

## Commands

ALWAYS use the `task *` commands

| Command        | Purpose                                             |
| -------------- | --------------------------------------------------- |
| `task compile` | Compile Kotlin (quick check)                        |
| `task build`   | Build all variants (APKs)                           |
| `task release` | Build release variant (APK)                         |
| `task lint`    | Prettier check + yamllint + actionlint + shellcheck |
| `task format`  | Prettier write (format non-kotlin files)            |

Do NOT use `-q` or pipe Gradle output through `Select-Object` — both hide progress and make long builds look hung.

# Rules

Do NOT run task compile/build/release every turn unless it is REQUIRED!!!
Do NOT run task compile/build/release every turn unless it is REQUIRED!!!
Do NOT run task compile/build/release every turn unless it is REQUIRED!!!
