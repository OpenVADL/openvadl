# Contribution Guidelines

With the beginning of the public implementation of **VADL**, it is important to define and follow consistent coding
guidelines, including static analysis practices. This ensures a uniform codebase, reduces formatting issues, and helps
prevent common runtime errors, such as `NullPointerException`.

We also define contribution guidelines, including Git conventions and workflows, to streamline development.

## Code style and formatting

Instead of defining our own Java coding style, we adopt the
well-established [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html), one of the two most
widely used standards. We apply a slightly adapted version.

To automatically format the code in the editor and enforce the style in the CI
pipeline, [Checkstyle](https://checkstyle.org/) is used. It allows to set configuration for Java code style definition
and has good support for IntelliJ and VSCode. The configuration `xml` file is part of the OpenVADL repository and is
located under `config/checkstyle/checkstyle.xml`.

## Null checking and bug prevention

To avoid NullPointerExceptions, we use static analysis tools to check nullability at compile time. This approach
introduces no runtime overhead and helps ensure correctness.

We use [NullAway](https://github.com/uber/NullAway) by Uber, a fast and widely used tool. It treats all values as
non-null by default, unless explicitly annotated with @Nullable from the javax.annotation package.

NullAway is a plugin for [Google’s Error Prone](https://github.com/google/error-prone), which detects common error
patterns through static analysis.

> [!NOTE]
> Use of `Optional` should be avoided.

## Documentation

The documentation in JavaDocs format and also enforced by Checkstyle, which guarantees that all public
methods and classes are documented in the source code.
Documentation should focus on what a class or method _means_ in the context of the project—its purpose and role—not how
it works. Avoid repeating what the code already states unless the logic is complex and needs clarification.

Also write handbook documentation in `docs/handbook`. It contains content relevant for OpenVADL users, such as examples,
usage instructions, and more.
It will be published at [https://openvadl.github.io/open-vadl/](https://openvadl.github.io/open-vadl/).

## Git

The following outlines the Git conventions and workflow used in the OpenVADL project.

### Commit Messages

To keep the commit messages uniform, the messages are written in the following format:

```
<scope>: <description>

[optional body]
```

The description is written in English, imperative mood (https://en.wikipedia.org/wiki/Imperative_mood). The first letter
of the description is capitalized. Do not end the description with a period.
Positive example: `hexagon: Extend instruction format`
Negative example: `hexagon: extends instruction format.`

The optional body text can contain more detailed information. If the commit belongs to an issue, it can also be
referenced in the body with `#<issue-id>`. The optional `scope` provides additional orientation by indicating to which
_subproject_ the commit belongs. Examples for the scope are `frontend, viam, iss, gcb, ...`.

> [!IMPORTANT]
> Avoid commits that only fixes formatting and checkstyle issues. Just amend the checkstyle fixes to your previous commit. Avoid repeating commit messages like abc: Fix xyz when retrying a bug fix after a failed CI run. Ammend new changes to the previous commit, or rewrite commit history, by squashing such commits before requesting a review.

### Branch Naming

Branches should represent the work performed on the branch. It must therefore have the form

```
<prefix>/<descriptive-name>
```

The prefix can be one of `wip, feature, bugfix, test, docs, chore, progess`. In special cases it can also be something
else.

The name is written in lower case, with alphanumeric characters and hyphens `-` as space separators.

> [!IMPORTANT]
> Changelog categories and PR labels are generated automatically from branch name prefixes. If you’re working on a
> feature that isn’t going to be finished when merging, prefix the branch with `wip`.

### Pull Requests

To merge changes into `master`, a pull request is required.
The PR title follows the commit message format but is usually more descriptive.
The body should clearly explain what was changed and how it impacts the project.

Make sure your PR has the correct change-scope. For features (`enhancement`) or bug fixes (`bug`), each PR should focus
on a single topic. If the title combines multiple changes, like `iss: Add GDB support and fix SRLIW RISC-V instruction`,
it’s a sign the work should be split into separate PRs.

> [!IMPORTANT]
> The Changelog entries are created from PR titles. To ensure a meaningful Changelog, it is important that the PR titles
> are descriptive, with a clear change-scope.

### Workflow

To keep the Git history relatively clean, developers should *rebase* the master branch onto their feature branches to
update their branch to master. **Do this regulary (at start of each work-day/session).**
If this would involve fixing complicated conflicts while rewriting the history during the rebase, a merge is preferable.
Note that *rebase* requires your branch to be pushed to the remote branch.
By using the `--force-with-lease` flag, you prevent accidentally overwriting changes in the remote branch.

When working on a change, open a PR in **draft** mode—even early on. This lets others see what you’re working on and
provide input sooner, which is often more helpful than after the work is finished.

Once you think your PR is ready to merge **and** the CI passes, request a review from someone best suited to provide
constructive feedback.

> [!IMPORTANT]
> Keep your PR size reasonable. It’s fine if it doesn’t finalize a feature—just prefix the branch with wip/ to indicate
> it’s a preparatory step for a feature or bugfix.

> [!NOTE]
> If your PR has the wrong label, you can change it. If it’s labeled `enhancement` or `bug` but shouldn’t appear in the
> changelog, add the `skip-changelog` label.
