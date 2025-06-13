# GitLaberFX: Branch Manager for GitLab

**GitLaberFX** is an open-source desktop application created for developers and team leads to effectively manage repositories in GitLab. It provides powerful tools for bulk cleaning and organizing branches, helping to maintain order in projects and simplify workflows.

## ✨ Key Features

- **Projects and Branches Overview:** Easily browse all projects and branches available to you.

- **Merge Analysis:** Automatically check which branches have already been merged into your target branch (e.g., `master` or `develop`).

- **Safe Archiving:** Instead of irreversible deletion, move outdated branches to an archive (e.g., `archive/feature/task-123`), preserving their history. The archive prefix can be customized.

- **Flexible Deletion:** Completely remove unnecessary branches.

- **Powerful Filtering:** Use **regular expressions (RegExp)** for instant bulk selection or exclusion of branches by mask (e.g., `feature/*`, `hotfix-*`).

- **Batch Operations:** Perform operations (deletion/archiving) for:

   - Manually selected branches.

   - Old merged branches (older than a specified date).

   - Old unmerged branches.

- **Interactive UI:** The interface instantly responds to your actions, updating the state locally without unnecessary server requests.

- **Multilingual Support:** English and Ukrainian languages with dynamic switching capability.


## 🚀 Getting Started

### Prerequisites

- **Java 17** or newer version.


### Launch

The project can be launched in two ways:

#### From source code via Maven

- **Normal launch:** `mvn clean antrun:run@run-without-debug`
- **Launch with debugging:** `mvn clean antrun:run@run-with-debug`

#### From ready-made distribution

1. Create a distribution with the command: `mvn clean package -Ppackage-app`
2. Unpack the resulting archive `target/gitlaberfx-1.0.zip`
3. Launch the program:
   - **Windows:** `bin/gitlaberfx.bat`
   - **Linux/macOS:** `bin/gitlaberfx`


### First Setup

On first launch, go to the `File > Settings` menu and specify:

1. **Your GitLab URL** (e.g., `https://gitlab.com`).

2. **Your personal API key** with `api` access rights.


Click **"Test Connection"** to verify the settings, and save them. After this, the program is ready to use.

## 📜 Documentation

Project documentation is available in the `src/main/docs/` directory:

- **User Guide** — detailed description of all functions and use cases.

- **Technical Requirements** — description of functional and technical requirements for the project.

- **Technical Documentation** — in-depth overview of architecture, patterns, and key components for developers.


We hope GitLaberFX will become your reliable assistant in working with GitLab!
