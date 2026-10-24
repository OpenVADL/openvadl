# OpenVADL CI server setup and operations

A practical reference reconstructed from **OpenVADL Devlog #8 – CI Server Setup Tour [GERMAN]** ([source video](https://www.youtube.com/watch?v=daa7o7f-8Os), 21:40, uploaded 27 August 2026).

Prepared 24 October 2026. This describes the configuration recorded in the video; the live servers were not accessed or changed.

## How to read this guide

The German automatic captions supply the spoken discussion, translated and condensed into English. The downloaded 1080p video was inspected through frame overviews across its full duration, with two-second sampling around key demonstrations and full-resolution inspection of commands and configuration. Screenshots below are crops of source frames.

**Shown** means the command, value, or output is visible on screen. **Discussed** means it comes from the spoken discussion as represented in the captions. **Suggested** marks an additional operational step, rather than something demonstrated. Commands have not been tested against the servers. Where the discussion is uncertain, that uncertainty is preserved.

## 1. Server overview

Sources: [00:06–01:25](https://www.youtube.com/watch?v=daa7o7f-8Os&t=6s), [05:12–06:48](https://www.youtube.com/watch?v=daa7o7f-8Os&t=312s).

| Machine | Role | Login used in the demonstration | Runner labels shown |
|---|---|---|---|
| MIPS | Public-facing access/proxy server; the intended entry point to the internal machines | Personal institute account through `mips.complang` | Not shown as a CI runner |
| `h0` | Debian Linux, x86-64; slower CI runner | `root@h0`, from MIPS | `self-hosted`, `Linux`, `X64`, `self-hosted-x64` |
| `x06` | macOS, ARM64; faster CI runner, using OrbStack for containers | `vadl@x06`, from MIPS | `self-hosted`, `macOS`, `ARM64`, `self-hosted-arm64` |

The demonstrated connection sequence is:

```text
Your computer → MIPS → h0 (root)
                    → x06 (vadl)
```

Both runners are registered with the **OpenVADL organization**, allowing organization repositories to use them. The recording shows them in **OpenVADL → Settings → Actions → Runners**, in the `Default` runner group, both idle at that moment.

![Runner names and exact labels at approximately 05:25](server-setup-assets/04-runner-labels.png)

## 2. Access and SSH configuration

### Get access provisioned

**Discussed, [01:42–02:42](https://www.youtube.com/watch?v=daa7o7f-8Os&t=102s):** First obtain access to MIPS. The procedure described is then to create a key on MIPS and give its **public key** to the server maintainer, who installs it for the Linux root account and the Mac account. Key generation and installation commands are not demonstrated, and no credentials are supplied in the recording.

Using MIPS as a direct SSH jump host is raised in the conversation. The presenter gives no technical reason against it, but does not configure it during the tour. The procedures below follow the demonstrated two-stage login.

### Configure the MIPS alias on your computer

**Shown, [00:31–01:03](https://www.youtube.com/watch?v=daa7o7f-8Os&t=31s):** The presenter opens `~/.ssh/config` with:

```sh
vim ~/.ssh/config
```

The relevant entry is transcribed below with the presenter's personal account replaced by a placeholder. Replace `<your-mips-user>` and use the appropriate local private-key path:

```sshconfig
Host mips.complang
    HostName mips.complang.tuwien.ac.at
    User <your-mips-user>
    HostKeyAlgorithms +ssh-rsa
    PubkeyAcceptedKeyTypes +ssh-rsa
    IdentityFile ~/.ssh/id_rsa
    ForwardAgent yes
```

The two `ssh-rsa` lines are highlighted on screen and described as important for this connection. These are the recorded host-specific compatibility settings, not a general requirement for every SSH server. Agent forwarding is also present in the configuration; the later access discussion separately describes provisioning a key on MIPS.

![The MIPS configuration at approximately 00:45](server-setup-assets/01-mips-ssh-config.png)

### Log into Linux

**Shown, [00:09](https://www.youtube.com/watch?v=daa7o7f-8Os&t=9s), [01:37–01:43](https://www.youtube.com/watch?v=daa7o7f-8Os&t=97s):**

```sh
# On your own computer:
ssh mips.complang

# In the resulting MIPS shell:
ssh root@h0
```

A successful second login produces a Debian banner and a `root@h0` prompt. The hostname is **h followed by the digit zero**.

![Successful Linux login, visible at approximately 01:55](server-setup-assets/02-linux-login.png)

### Log into the Mac

**Shown, [14:15–14:35](https://www.youtube.com/watch?v=daa7o7f-8Os&t=855s):** From MIPS:

```sh
ssh vadl@x06
```

The hostname is **x followed by zero and six**. If coming from the Linux walkthrough, leave the `github-runner` subshell first, then leave the root SSH session to return to MIPS:

```sh
exit    # github-runner shell → root shell, if that subshell is open
exit    # h0 → MIPS
ssh vadl@x06
```

Only run the exits appropriate to your current shell. The recording confirms the return to MIPS with `Connection to h0 closed.` before the Mac login.

![Mac login from MIPS at approximately 14:45](server-setup-assets/08-mac-login.png)

## 3. Linux runner: service and working directories

### Locate and check the service

**Shown and discussed, [03:11–04:00](https://www.youtube.com/watch?v=daa7o7f-8Os&t=191s):** The runner runs under the dedicated **`github-runner`** user rather than root. Its installation directory is:

```text
/home/github-runner/actions-runner
```

The tour uses the root shell to inspect and manage the installed service:

```sh
cd /home/github-runner/actions-runner
ls
./svc.sh help
./svc.sh status
```

The help output lists `install`, `start`, `stop`, `status`, and `uninstall`. The presenter explicitly says installation is already done and does not need to be repeated. For existing-service management, the listed commands are:

```sh
./svc.sh start
./svc.sh stop
./svc.sh status
```

`start` and `stop` are shown in help and discussed; only the status check is executed in this demonstration.

The status output identifies:

```text
/etc/systemd/system/actions.runner.OpenVADL.h0.service
```

At the time of recording it is **loaded, enabled, and active (running)**. The output also shows recent job results and connection/listening messages.

![Linux service status at approximately 03:55](server-setup-assets/03-linux-runner-service.png)

**Discussed, [11:54–12:18](https://www.youtube.com/watch?v=daa7o7f-8Os&t=714s):** Docker and the Linux runner are expected to start automatically after a reboot.

### Find a CI checkout

**Shown, [04:14–04:27](https://www.youtube.com/watch?v=daa7o7f-8Os&t=254s), [09:40–10:15](https://www.youtube.com/watch?v=daa7o7f-8Os&t=580s):** The `_work` directory is inside the runner installation, not directly inside the user's home directory.

```sh
cd /home/github-runner/actions-runner
ls _work/
cd _work/openvadl/
ls
ls openvadl/
```

The actual OpenVADL repository checkout is nested one level further:

```text
/home/github-runner/actions-runner/_work/openvadl/openvadl
```

The directory listing also shows work areas such as `doxygen` and `resselpark`, together with runner-managed directories such as `_actions`, `_temp`, and `_tool`.

The presenter believes the repository directory is cleared and cloned again for jobs, but does not verify that lifecycle. Treat the directory as CI-managed workspace, rather than assuming manual edits will persist.

### Work as the runner user

**Shown, [12:36–13:10](https://www.youtube.com/watch?v=daa7o7f-8Os&t=756s), discussed through [13:58](https://www.youtube.com/watch?v=daa7o7f-8Os&t=838s):** From the Linux administrative shell, the working command is:

```sh
sudo -u github-runner bash
```

The prompt changes to `github-runner@h0`. This user is described as having no password, so the demonstrated administrative user switch is the intended access method. The command keeps the current working directory; the recording runs it from the CI workspace.

An earlier attempt, `bash -u github-runner`, fails with `No such file or directory`. Do not copy that failed attempt as the user-switch procedure.

![The failed attempt followed by the successful user switch, visible around 13:15](server-setup-assets/07-switch-runner-user.png)

### Job concurrency and triggering

**Discussed, [10:17–11:25](https://www.youtube.com/watch?v=daa7o7f-8Os&t=617s), [13:07–13:48](https://www.youtube.com/watch?v=daa7o7f-8Os&t=787s):**

- Each runner processes **one job at a time**.
- Multiple runner installations could exist on one host, even under the same user in separate directories. That is described as possible, not part of this setup.
- The heavier CI tests already use multiple threads and make good use of the machine, limiting the benefit of adding competing runners.
- Jobs normally arrive through the runner's connection to GitHub. The speakers do not establish a supported way to trigger a GitHub workflow locally through the runner installation.

## 4. Choose the appropriate runner

Source: [05:18–06:48](https://www.youtube.com/watch?v=daa7o7f-8Os&t=318s).

The team generally uses the custom architecture labels shown at the end of each runner's label list:

| Need | Label | Guidance from the discussion |
|---|---|---|
| Either machine is acceptable | `self-hosted` | Architecture does not matter |
| Linux x86-64 specifically | `self-hosted-x64` | Targets `h0` |
| ARM64 Mac specifically | `self-hosted-arm64` | Targets `x06` |

**Illustrative YAML reconstructed from the visible labels; no workflow file is shown in the video:**

```yaml
# Choose the relevant value for a job:
runs-on: self-hosted
```

```yaml
runs-on: self-hosted-x64
```

```yaml
runs-on: self-hosted-arm64
```

The Mac is described as substantially faster. For CI turnaround, the presenter prefers putting the relevant tasks on the Mac, because a task assigned to slow `h0` can delay the whole run. Use `h0` for work whose elapsed time is less important, preserving Mac capacity. Benchmarks are described as running on `h0`; for tracking performance changes over time, keep the runner consistent.

## 5. Linux storage and the recurring disk-space problem

### Docker data is on a separate disk

**Discussed, [06:51–07:34](https://www.youtube.com/watch?v=daa7o7f-8Os&t=411s), [09:07–09:31](https://www.youtube.com/watch?v=daa7o7f-8Os&t=547s), [11:28–11:47](https://www.youtube.com/watch?v=daa7o7f-8Os&t=688s):** Running Docker's `overlay2` storage on the previous ZFS arrangement caused layer cleanup problems and rapid disk growth. The workaround was to move Docker data to a separate disk. This is the team's account of their configuration and failure, not a general claim about all Docker/ZFS configurations.

**Shown, around [06:55](https://www.youtube.com/watch?v=daa7o7f-8Os&t=415s):**

```sh
ls -al /var/run/docker.sock
df -h
```

The output distinguishes these locations:

| Location | Recorded storage arrangement |
|---|---|
| `/var/lib/docker` | Separate device `/dev/sda1`; shown as 1.8T total, 335G used, 1.4T available |
| `/home` | ZFS dataset `rpool/home`; includes the CI workspace |
| `/bu` | ZFS dataset `rpool/bu`; contains the backup directory |
| `/var/run/docker.sock` | Linux Docker socket; shown owned by `root:docker` |

The Docker disk and the ZFS pool are different capacity domains. Free space on the Docker disk does not prevent backups from filling the storage used by the runner workspace.

![Linux mounts and available space at approximately 06:55](server-setup-assets/05-linux-storage.png)

### Inspect backups when ZFS space runs low

**Shown and discussed, [07:35–09:05](https://www.youtube.com/watch?v=daa7o7f-8Os&t=455s):**

```sh
df -h
cd /bu/backup/
ls
ls -alh
```

The directory contains large `.tgz` archives. The listing shows **545G total** and recent home archives of roughly **26–27G each**. The displayed available space for several `rpool` datasets is **241G** at that moment; these are historical readings, not current capacity.

The presenter identifies accumulated backups as the recurring reason CI can run out of space and fail. They describe previously deleting many old archives while keeping some early backups from each year. However, they do not know exactly what all the backups contain, and do not establish a formal retention policy. No deletion command is demonstrated.

For an incident, use the displayed commands to identify the affected filesystem and inspect the archive sizes. Confirm which archives may be removed with the backup owner before deleting them. The video supplies a diagnosis and location, not an approved blanket cleanup command.

![Backup archive sizes at approximately 08:35](server-setup-assets/06-backup-archives.png)

## 6. Mac runner and OrbStack

### Check the runner

**Shown, [14:45–15:15](https://www.youtube.com/watch?v=daa7o7f-8Os&t=885s):** After logging into `x06` as `vadl`:

```sh
cd ~/actions-runner
ls
./svc.sh status
```

The absolute runner directory is:

```text
/Users/vadl/actions-runner
```

The status command reports the LaunchAgent file and a started service:

```text
/Users/vadl/Library/LaunchAgents/actions.runner.OpenVADL.x06.plist
```

![Mac runner service status at approximately 15:15](server-setup-assets/09-mac-runner-status.png)

### Start OrbStack when needed

**Shown and discussed, [15:22–15:40](https://www.youtube.com/watch?v=daa7o7f-8Os&t=922s):** OrbStack supplies the container environment on the Mac. The tour displays help and types the startup command:

```sh
orb -h
orb start
```

No explicit startup result is shown. Later, `docker ps` does successfully list a running container. The help distinguishes `orb`, used for OrbStack and Linux-machine operations, from `docker`, used for Docker containers.

### Understand the boot/login dependency

**Discussed, [15:40–18:10](https://www.youtube.com/watch?v=daa7o7f-8Os&t=940s):** In this setup, **`vadl` must log in automatically to a graphical macOS session** after boot so the required user-session privileges and services work. An SSH connection alone was not sufficient for the setup the presenter had tried. The machine is described as currently booting successfully without a display or mouse attached, though a display was used during setup.

OrbStack had recently failed to start automatically. Adding it through macOS **Login Items** is proposed, but the settings are not opened or changed during the recording. Do not assume the autostart issue was resolved in the video.

The runner has its own LaunchAgent, described below. Its presence does not demonstrate that OrbStack itself is configured to launch at login.

### Check BuildKit

**Shown and discussed, [18:19–18:48](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1099s), [19:45–20:25](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1185s):**

```sh
docker ps
```

The recording shows:

| Field | Recorded value |
|---|---|
| Image | `moby/buildkit:latest` |
| Container name | `buildkitd` |
| Status | `Up 2 days` |
| Published ports | `0.0.0.0:1334->1334/tcp`, `[::]:1334->1334/tcp` |

![BuildKit container at approximately 18:25](server-setup-assets/10-buildkit-container.png)

The discussion says the BuildKit image needs to have been pulled and suggests startup happens automatically, but the exact creation/start command and restart configuration are not shown. The project uses BuildKit directly for image builds rather than the previous normal Docker-build route. The speakers are unsure of some implementation details; this is not a complete recipe for recreating the BuildKit service.

**Suggested diagnostic if BuildKit is absent:** inspect stopped containers with `docker ps -a`. The recording does not supply enough configuration to invent a replacement `docker run` command.

## 7. Mac LaunchAgent and exact Docker environment

**Shown and discussed, [19:00–21:39](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1140s):** Inspect the existing configuration with:

```sh
cat /Users/vadl/Library/LaunchAgents/actions.runner.OpenVADL.x06.plist
```

The file launches `/Users/vadl/actions-runner/runsvc.sh` as `vadl`, with `/Users/vadl/actions-runner` as its working directory. It sets `RunAtLoad` to true, `ProcessType` to `Interactive`, and `SessionCreate` to true.

The environment in the displayed file is:

```text
ACTIONS_RUNNER_SVC=1
TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock
DOCKER_HOST=unix:///Users/vadl/.orbstack/run/docker.sock
```

These two socket values intentionally differ. The presenter identifies both the Testcontainers override and the OrbStack-specific `DOCKER_HOST` as important. Previously, CI selected another container environment, recalled as Docker Desktop or Colima; explicitly setting the runner environment fixed that selection. Processes started by the runner inherit these values.

![Full LaunchAgent configuration at approximately 19:35](server-setup-assets/11-mac-launch-agent.png)

### Configuration transcribed from the screen

Whitespace has been normalized. This is a reference copy of the existing file, not an instruction to overwrite a live configuration:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>actions.runner.OpenVADL.x06</string>
    <key>ProgramArguments</key>
    <array>
        <string>/Users/vadl/actions-runner/runsvc.sh</string>
    </array>
    <key>UserName</key>
    <string>vadl</string>
    <key>WorkingDirectory</key>
    <string>/Users/vadl/actions-runner</string>
    <key>RunAtLoad</key>
    <true/>
    <key>StandardOutPath</key>
    <string>/Users/vadl/Library/Logs/actions.runner.OpenVADL.x06/stdout.log</string>
    <key>StandardErrorPath</key>
    <string>/Users/vadl/Library/Logs/actions.runner.OpenVADL.x06/stderr.log</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>ACTIONS_RUNNER_SVC</key>
        <string>1</string>
        <key>TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE</key>
        <string>/var/run/docker.sock</string>
        <key>DOCKER_HOST</key>
        <string>unix:///Users/vadl/.orbstack/run/docker.sock</string>
    </dict>
    <key>ProcessType</key>
    <string>Interactive</string>
    <key>SessionCreate</key>
    <true/>
</dict>
</plist>
```

The file identifies the log locations:

```text
/Users/vadl/Library/Logs/actions.runner.OpenVADL.x06/stdout.log
/Users/vadl/Library/Logs/actions.runner.OpenVADL.x06/stderr.log
```

**Suggested diagnostic, not executed in the video:**

```sh
tail -n 100 /Users/vadl/Library/Logs/actions.runner.OpenVADL.x06/stderr.log
```

### Check Docker context selection

**Shown, [20:45–21:39](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1245s):**

```sh
docker context ls
```

The output lists `colima`, `default`, `desktop-linux`, and `orbstack`. Both the active `default` entry and `orbstack` point at:

```text
unix:///Users/vadl/.orbstack/run/docker.sock
```

The terminal explicitly warns that **`DOCKER_HOST` overrides the active context**. Therefore, changing the selected Docker context alone does not resolve an incorrect runner-level `DOCKER_HOST` value. The recording does not demonstrate editing or reloading the LaunchAgent.

The earlier `docker ls` attempt fails as an unknown command. Use `docker context ls` for contexts and `docker ps` for running containers.

![Docker contexts and the environment-variable warning at approximately 21:05](server-setup-assets/12-docker-contexts.png)

## 8. Incident reference

The checks below assemble the demonstrated commands into a troubleshooting order. This order is an editorial reconstruction, not a diagnostic procedure executed end to end in the recording.

| Symptom | First checks | Relevant conclusion from the tour |
|---|---|---|
| Cannot reach an internal machine | Log into MIPS, then use `ssh root@h0` or `ssh vadl@x06` | MIPS and target-account key provisioning are separate access steps |
| Linux runner unavailable | In `/home/github-runner/actions-runner`, run `./svc.sh status` | Inspect service state and recent connection/job messages |
| Linux CI fails because storage is full | `df -h`, then inspect `/bu/backup/` with `ls -alh` | Large backup archives can fill ZFS space used by the workspace |
| Mac runner unavailable after reboot | Confirm the `vadl` graphical login, then check `./svc.sh status` | The recorded setup depends on the user session and LaunchAgent |
| Mac containers unavailable | `orb start`, then `docker ps` | OrbStack autostart was a known unresolved configuration item |
| CI uses the wrong Docker backend | Read the LaunchAgent environment and run `docker context ls` | The runner's explicit `DOCKER_HOST` must point at OrbStack |
| Image building fails | Check `docker ps` for `buildkitd` | The demonstrated image is `moby/buildkit:latest`; creation details are absent |
| CI uploads fail despite the usual services running | No established fix in the tour | An upload problem is mentioned around 12:20, but its cause is unresolved |

## 9. Details the video does not establish

- The precise backup contents, backup scheduler, owner-approved retention policy, and deletion procedure.
- The final state of OrbStack Login Items and the exact macOS automatic-login settings.
- The complete BuildKit creation command, persistent configuration, restart policy, and provisioning workflow.
- The exact SSH key-generation/installation commands or an implemented `ProxyJump` configuration.
- A proven local method for triggering workflows through the runner.
- The cause or fix for the upload failure mentioned during the Linux discussion.
- Whether job checkouts are always removed and freshly cloned; the presenter explicitly expresses uncertainty.

## 10. Video navigation

| Time | Topic |
|---|---|
| [00:06](https://www.youtube.com/watch?v=daa7o7f-8Os&t=6s) | MIPS entry point and SSH configuration |
| [01:37](https://www.youtube.com/watch?v=daa7o7f-8Os&t=97s) | Login to `h0`; access provisioning |
| [03:11](https://www.youtube.com/watch?v=daa7o7f-8Os&t=191s) | Linux runner user, directory, and service |
| [04:14](https://www.youtube.com/watch?v=daa7o7f-8Os&t=254s) | Runner `_work` directory |
| [05:12](https://www.youtube.com/watch?v=daa7o7f-8Os&t=312s) | Organization runners, labels, and scheduling choices |
| [06:51](https://www.youtube.com/watch?v=daa7o7f-8Os&t=411s) | Docker, ZFS, and separate-disk arrangement |
| [07:35](https://www.youtube.com/watch?v=daa7o7f-8Os&t=455s) | Backup archives and space exhaustion |
| [09:40](https://www.youtube.com/watch?v=daa7o7f-8Os&t=580s) | Exact repository workspace path |
| [10:17](https://www.youtube.com/watch?v=daa7o7f-8Os&t=617s) | Runner concurrency and resource utilization |
| [11:48](https://www.youtube.com/watch?v=daa7o7f-8Os&t=708s) | Routine failures and automatic service startup |
| [12:36](https://www.youtube.com/watch?v=daa7o7f-8Os&t=756s) | Switch to the runner user; job triggering discussion |
| [14:15](https://www.youtube.com/watch?v=daa7o7f-8Os&t=855s) | Login to `x06` and inspect its runner |
| [15:22](https://www.youtube.com/watch?v=daa7o7f-8Os&t=922s) | OrbStack startup and graphical login requirement |
| [17:35](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1055s) | Proposed OrbStack Login Items configuration |
| [18:19](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1099s) | BuildKit container |
| [19:00](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1140s) | LaunchAgent file and environment variables |
| [20:45](https://www.youtube.com/watch?v=daa7o7f-8Os&t=1245s) | Docker contexts and `DOCKER_HOST` override |
