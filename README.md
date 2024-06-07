# OpenVADL

## Development

Before contributing, please read [OpenVADL's coding guidelines](https://ea.complang.tuwien.ac.at/vadl/vadl/issues/1573).

### Checkstyle

We are using Checkstyle to ensure a consistent format and documentation of the source code.

Install the Checkstyle plugin for your IDE and import our Checkstyle configuration.
The configuration is located under `config/checkstyle/checkstyle.xml`.

#### Using Intellij

To use the Checkstyle confirm IntelliJ code style follow these steps:

1. Go to `Settings > Editor > Code Style > Java`
2. For the `Scheme` select `Project`

If you still get formatting conflicts between Intellij and Checkstyle follow these steps:

1. Go to
   `Settings > Editor > Code Style > Java`
2. Click on the settings icon and select `Import Scheme > Checkstyle configuration`
3. Choose the Checkstyle config file under `config/checkstyle/checkstyle.xml`
4. Enable the Java code style setting `JavaDocs > Other > Indent continuation lines`
   This prevents a JavaDocs formatting conflict between IntelliJ and Checkstyle.

With this, IntelliJ uses the code style rules as specified in the Checkstyle config.
Note that Checkstyle and code style are not 100% compatible,
so IntelliJ will eventually generate some invalid formatted code (such as Java docs
paragraph separation).

### Git Hooks Installation

To install the Git hooks in `config/git/hooks` run

```bash
python3 config/git/install_hooks.py
```

This will create symlinks in the `.git/hooks` directory of your local repository
and forces Git to call those hooks during certain operations.
E.g. it will abort a commit if the commit message violates
[OpenVADL's coding guidelines](https://ea.complang.tuwien.ac.at/vadl/vadl/issues/1573).

Note: If you re-clone the repository, you will have to reinstall the hooks.




