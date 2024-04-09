# OpenVADL

## Development

Before contributing, please read [OpenVADL's coding guidelines](https://ea.complang.tuwien.ac.at/vadl/vadl/issues/1573).

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




