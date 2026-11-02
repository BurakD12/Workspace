# Course 09 — Docker & Containerization Fundamentals: Images, Layers, Compose, and a Spring Boot App That Runs Anywhere

**Prerequisites:** a finished StockFlow (Part 1 of `LAB_09_DOCKER_STOCKFLOW.md`) — or any Spring Boot
application that builds to a jar and talks to PostgreSQL. This course assumes **zero** prior Docker
knowledge. It does assume you know what a JVM, a jar, a port, and an environment variable are.

**Stack:** Docker Engine 28.x via Docker Desktop 4.44, Compose v2.39 · Spring Boot 4.0.6 · Java 21 ·
PostgreSQL 16.
Every command, size, and timing below was run on the reference StockFlow image; where a number
appears, it is a measurement, not an estimate.

---

## What you'll learn

Until now, StockFlow runs in exactly one place: your machine, with your JDK, your Maven cache, your
PostgreSQL on port 5432 with your password. Hand the project to a colleague and the first hour is
spent making their machine look like yours. Deploy it to a server and it is the same hour again, with
higher stakes. This course is about ending that.

By the end you will be able to:

1. Explain what a **container** is, how it differs from a virtual machine, and what the Docker
   **daemon**, **client**, and **registry** each do.
2. Tell an **image** from a **container**, and drive the core lifecycle with the CLI: `pull`, `images`,
   `run`, `ps`, `logs`, `exec`, `stop`, `rm`.
3. Read and write a **Dockerfile**, and explain the **layer cache** well enough to order instructions
   so a source-code change does not re-download 65 MB of dependencies.
4. Write a **multi-stage build** that compiles with a JDK and ships only a JRE, splits a Spring Boot
   fat jar into cache-friendly **layers** with the Boot 4 `tools` jarmode, and runs as a **non-root**
   user.
5. Wire a running container to the outside world: **ports**, **volumes**, and **environment variables**
   — and say why configuration must never be baked into an image.
6. Describe a multi-container application in **`compose.yaml`**: service-name DNS, a
   **healthcheck**, `depends_on` that waits for *healthy*, and a **named volume** that makes your
   inventory survive a restart.
7. Prove the last point: record stock, tear the stack down, bring it back, and read the same number.

---

## 1. Why containers: the problem, and the machine that solves it

### 1.1 "Works on my machine"

Run StockFlow today and it needs, in this order: a JDK 21 on the PATH, a Maven cache with the ~90
runtime jars (and their test and plugin cousins) already resolved, PostgreSQL listening on `localhost:5432` with user `postgres` / password `123456`
and a database called `stockflow`, and nothing else already sitting on port 8080. Every one of those
is *implicit* — it lives in your head, or in a README nobody reads.

A **container** makes all of it explicit and portable. It packages the application **together with
everything it needs to run** — the JRE, the jars, the exact command line — into one artifact that
runs identically on any machine with Docker: your laptop, a colleague's, a CI runner, a server.

### 1.2 What a container actually is (and is not)

The common description — "a lightweight virtual machine" — is wrong in a way that matters.

A **virtual machine** emulates a whole computer: it boots its own operating-system kernel on
virtualised hardware. That is why a VM takes tens of seconds to start and needs hundreds of megabytes
of RAM before your application has done anything.

A **container** does not boot anything. It is an ordinary **process** on the host's operating system
— your JVM, with your PID — that the kernel has been told to *isolate*: it sees its own filesystem
(the image), its own process tree, its own network interface, and its own hostname, and it cannot see
the host's. Two Linux kernel features do this: **namespaces** (what the process can *see*) and
**cgroups** (how much CPU and memory it may *use*).

| | Virtual machine | Container |
| --- | --- | --- |
| Runs its own kernel? | yes | **no** — shares the host kernel |
| Start-up | seconds to minutes | **milliseconds** (it is just a process) |
| Size | gigabytes | tens to hundreds of megabytes |
| Isolation | hardware-level | kernel-level (namespaces + cgroups) |
| Can it run a different OS kernel? | yes | no — a Linux container needs a Linux kernel |

That last row explains Docker Desktop on Windows and macOS: it runs one small Linux VM (WSL 2 on
Windows) and every container is a process *inside* that VM. So you *do* have a VM — exactly one, shared
by all your containers, started once.

### 1.3 The three parts of Docker

When you type `docker run`, three things cooperate:

- **The client** — the `docker` command. It does nothing itself; it sends requests to…
- **The daemon (engine)** — `dockerd`, the long-running service that builds images, starts and stops
  containers, and manages networks and volumes. This is what "Docker Desktop is starting…" is waiting
  for. If it is not running, every `docker` command fails with a *cannot connect* error — not a
  Docker-is-broken error, a nobody-is-listening error.
- **The registry** — a server that stores images. **Docker Hub** is the default public one;
  `postgres:16` and `eclipse-temurin:21-jre-alpine` come from there. Companies run private ones, and
  Lab 25 pushes StockFlow to GitHub's.

```
docker CLI  ──── REST over a socket ────▶  dockerd  ──── pulls from ────▶  Docker Hub
 (client)                                  (daemon)                        (registry)
```

---

## 2. Images vs. containers, and the CLI

### 2.1 The one distinction everything else depends on

An **image** is a **read-only template**: a filesystem snapshot plus metadata (which command to run,
which port to document, which user to run as). It is what you build, tag, push, and pull. It never
changes; you make a *new* one.

A **container** is a **running instance** of an image — the image's filesystem plus a thin writable
layer on top, plus a process. You can start ten containers from one image; each has its own writable
layer and its own process, and none of them modifies the image.

The class/object analogy is exact: image is to container as class is to instance.

### 2.2 Naming: repository and tag

`postgres:16` is `repository:tag`. The repository (`postgres`) identifies *what* it is; the tag
(`16`) identifies *which version*. Omit the tag and Docker assumes `latest` — which is not "the newest",
it is just a tag someone chose to name `latest`, and it can change under you. **Always pin a tag.**
StockFlow's own image is `stockflow:1.0`.

### 2.3 The lifecycle commands

Try each of these now; they take seconds.

```bash
docker pull postgres:16              # download an image from the registry (no container yet)
docker images                        # list local images: repository, tag, id, size
docker run -d --name pg -e POSTGRES_PASSWORD=x postgres:16
                                     # create + start a container from the image, detached (-d)
docker ps                            # running containers (add -a to include stopped ones)
docker logs pg                       # its stdout/stderr — your first stop when something is wrong
docker logs -f pg                    # …and keep following (Ctrl-C to stop following, not the container)
docker exec -it pg psql -U postgres  # run ANOTHER process inside the running container — here the
                                     # psql client (-i interactive, -t terminal). Type \q to leave.
docker exec -it pg sh                # …or a shell, to look around the container's filesystem
docker stop pg                       # SIGTERM, then SIGKILL after 10s — the process ends
docker rm pg                         # delete the (stopped) container and its writable layer
docker rmi postgres:16               # delete the image (only once nothing references it)
```

Two mental-model points:

- `docker run` = `docker create` + `docker start`. Running it twice makes **two** containers.
- `docker stop` does not delete anything; `docker rm` deletes the container but not the image. Data
  written inside the container's writable layer dies with `docker rm` — which is the whole reason
  volumes exist (§5.2).

---

## 3. The Dockerfile and the layer cache

### 3.1 A recipe, one instruction per line

A **Dockerfile** is the script that builds an image. Each line is an instruction; `docker build`
executes them top to bottom. The instructions you need:

| Instruction | Meaning |
| --- | --- |
| `FROM image:tag` | start from this **base image** (there is always one; `scratch` means empty) |
| `WORKDIR /path` | `cd` here for every following instruction (creates it if needed) |
| `COPY src dest` | copy files **from the build context** (your project directory) into the image |
| `RUN cmd` | run a command *inside* the image at build time, and keep the result |
| `ENV KEY=value` | set an environment variable that every later instruction and the running container see |
| `EXPOSE 8080` | **documentation**: "this app listens on 8080". It publishes nothing — that is `-p` |
| `USER name` | run every following instruction and the container's process as this user |
| `HEALTHCHECK cmd` | a command Docker runs periodically to decide whether the container is healthy |
| `ENTRYPOINT ["exe", "arg"]` | the process to start when the container starts |
| `CMD ["arg"]` | default arguments to `ENTRYPOINT` (or the whole command if no ENTRYPOINT) |

Use the JSON-array ("exec") form for `ENTRYPOINT` and `CMD`. The string form wraps your command in
`/bin/sh -c`, which means signals like `docker stop`'s SIGTERM reach the shell, not the JVM, and your
application is killed without a graceful shutdown.

### 3.2 Every instruction is a layer, and layers are cached

This is the concept that separates a Dockerfile that rebuilds in 12 seconds from one that rebuilds in
4 minutes.

Each `COPY` and `RUN` produces a **layer**: a filesystem diff stacked on the layers before it. An image
is that stack. When you rebuild, Docker walks the Dockerfile from the top and, for each instruction,
asks: *have I built this exact instruction, on top of this exact previous layer, with these exact input
files before?* If yes, it reuses the cached layer — instantly. **The first instruction whose inputs
changed, and every instruction after it, are rebuilt.**

So order matters enormously. Consider the two most expensive things in a Spring Boot build:

- resolving dependencies (`mvn dependency:go-offline` — the 64.7 MB of runtime jars plus the
  test-scope starters and every build plugin) — changes only when `pom.xml` changes, i.e. rarely;
- compiling and packaging your code — changes every time you edit a source file.

Naive ordering:

```dockerfile
COPY . .                       # pom.xml AND src in one layer
RUN mvn package                # any source change invalidates this → re-downloads everything
```

Cache-aware ordering — what the reference Dockerfile does:

```dockerfile
COPY pom.xml .                 # layer A: changes rarely
RUN mvn -q -B dependency:go-offline   # layer B: cached as long as pom.xml is unchanged
COPY src ./src                 # layer C: changes constantly
RUN mvn -q -B package -DskipTests     # layer D: rebuilt — compiles; no dependency re-download
                                      # (go-offline is not perfect: a few plugin artefacts may still be fetched)
```

Here is the measured effect on StockFlow. After a **one-line change to a source file**, the rebuild
log shows exactly which steps were reused:

```
CACHED  [build 3/6] COPY pom.xml .
CACHED  [build 4/6] RUN mvn -q -B dependency:go-offline     ← the dependency download: skipped
RE-RUN  [build 5/6] COPY src ./src
RE-RUN  [build 6/6] RUN mvn -q -B package -DskipTests
…
CACHED  [stage-2 4/7] COPY … /extracted/dependencies/ ./     ← the 65 MB image layer: reused
RE-RUN  [stage-2 7/7] COPY … /extracted/application/ ./      ← 115 KB: the only new layer
```

Cold build: minutes. Rebuild after that edit: **12 seconds**. Same Dockerfile, same result — the only
difference is that the expensive layers' inputs did not change. (`CACHED` is BuildKit's word;
`RE-RUN` is this document's annotation — BuildKit shows a re-executed step as `DONE <seconds>`.)

### 3.3 `.dockerignore`: what the daemon never sees

`docker build .` begins by bundling the **build context** — the whole directory — and sending it to
the daemon. Two problems: a local `target/` alone is ~60 MB of jars you are uploading for nothing, and
a stale local `target/` could be `COPY`ed into the image in place of a fresh build. `.dockerignore`
lists what to leave out; anything in it is invisible to every `COPY`. The reference excludes
`target/`, `.git/`, IDE files, the docs, the Postman collection, the Maven wrapper (the build image
has its own `mvn`), and `.env` — because a secrets file must never end up inside an image.

---

## 4. Multi-stage builds and the Spring Boot layered jar

### 4.1 The problem with the obvious Dockerfile

The obvious approach — start from a Maven image, copy the source, `mvn package`, run the jar — works,
and produces an image that carries a full JDK, all of Maven, the entire `~/.m2` download cache, your
source code, and your test classes. None of that is needed to *run* the application. Measured on
StockFlow:

| Image | Contents | Size |
| --- | --- | --- |
| naive single-stage (`maven:3.9-eclipse-temurin-21` + `mvn package` + `java -jar`) | JDK, Maven, Maven cache, sources, fat jar | **395 MB** (377 MiB) |
| reference three-stage (§4.2) | Alpine JRE + 89 jars + a 115 kB thin jar | **132 MB** (126 MiB) |

Both figures are `docker image inspect --format '{{.Size}}'` on Docker Desktop, in decimal megabytes
(the unit `docker history` also uses). Three times smaller — and, more importantly, an attacker who
gets into the runtime container finds a JRE and your application, not a compiler, a build tool, and
your source.

> **What "size" means depends on where you ask.** Docker Desktop stores images with the containerd
> image store, and there `inspect`'s `Size` is the **compressed** content — the bytes that would be
> pushed to a registry. The layers *unpack* to more: `docker history stockflow:1.0` sums to ≈277 MB
> (≈212 MB of that is the JRE base), and that unpacked figure is what a classic Linux engine (a CI
> runner, a server) reports as the image size. The **ratio** is the portable fact; so are the
> per-layer numbers below.

### 4.2 Multi-stage: build in one image, ship another

A Dockerfile may contain several `FROM` lines. Each starts a new **stage**; a later stage can
`COPY --from=` an earlier one; and **only the last stage becomes the image**. Everything the earlier
stages downloaded or built is thrown away unless copied forward.

The reference Dockerfile has three stages:

```dockerfile
# ---- Stage 1: build the fat jar (a JDK + Maven; never shipped) -------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

# ---- Stage 2: explode the fat jar into layers -------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS extract
WORKDIR /extract
COPY --from=build /build/target/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# ---- Stage 3: the runtime image ---------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S stockflow && adduser -S -G stockflow stockflow
WORKDIR /app
COPY --from=extract /extract/extracted/dependencies/ ./
COPY --from=extract /extract/extracted/spring-boot-loader/ ./
COPY --from=extract /extract/extracted/snapshot-dependencies/ ./
COPY --from=extract /extract/extracted/application/ ./
USER stockflow
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "application.jar"]
```

Why the official **Maven image** rather than the project's own `./mvnw`: the wrapper is a shell
script, and a shell script with CRLF line endings fails inside a Linux build stage with the baffling
`/bin/sh^M: bad interpreter`. A Spring Initializr project guards against this with a `.gitattributes`
that pins `/mvnw text eol=lf` — StockFlow has that line — but not every project does, and one editor
save or one copy through a Windows tool undoes it. The Maven image has `mvn` already; the wrapper
never enters the picture, so the failure mode cannot occur.

Why `-DskipTests`: the image build is not the test pipeline. Tests already ran locally (and will run
in CI in Lab 25); running them again inside every image build would turn a 12-second rebuild into
minutes for no new information.

### 4.3 The Spring Boot layered jar — and what Boot 4 actually produces

A Spring Boot fat jar is one ~60 MB file. Copied into an image as one `COPY`, it is one 60 MB layer,
and *any* change to your code produces a whole new 60 MB layer. That defeats the cache for the runtime
image even after §3.2 fixed the build stage.

Spring Boot solves this with the **`tools` jarmode** (Boot 3.3+; it supersedes the older
`layertools` mode, which is gone in Boot 4):

```bash
java -Djarmode=tools -jar application.jar extract --layers --destination extracted
```

This splits the jar into four folders, ordered from least to most likely to change:

| Folder | Contents | Measured on StockFlow |
| --- | --- | --- |
| `dependencies/` | `lib/*.jar` — every third-party jar | **64.7 MB**, 89 jars |
| `spring-boot-loader/` | *(empty — see below)* | 0 |
| `snapshot-dependencies/` | any `-SNAPSHOT` dependencies | 0 |
| `application/` | **a thin jar of your own classes** | **115 KB** |

Each folder becomes one `COPY` — one layer. After a code change, only the last, 115 KB layer is new.

> **Boot 4 detail that changes the entrypoint.** In the old `layertools` era, `application/` held
> exploded classes and you launched with `java org.springframework.boot.loader.launch.JarLauncher`.
> The `tools` mode is different: `application/application.jar` is a **thin jar** whose manifest
> carries `Main-Class: com.example._9_docker.Application` and a *relative* `Class-Path: lib/…` naming
> all 89 jars. Because the four folders are copied into the same directory, `lib/` sits next to the
> thin jar, and the launch is simply `java -jar application.jar`. That is also why
> `spring-boot-loader/` is empty — no loader is needed. (The reference copies it anyway, for fidelity
> with Spring's documented Dockerfile; an empty `COPY` costs nothing.) If you follow a tutorial that
> uses `JarLauncher`, it predates Boot 3.3.

### 4.4 A JRE, Alpine, and a non-root user

Three choices in stage 3, each with a reason:

- **JRE, not JDK.** Nothing in the runtime compiles. `eclipse-temurin:21-jre-alpine` is 74 MB
  compressed (≈212 MB unpacked); the JDK-bearing Maven image is 243 MB compressed.
- **Alpine.** A minimal Linux; its `busybox` provides `wget`, which the healthcheck uses, so nothing
  extra has to be installed. (Temurin's Alpine build is official; Spring Boot runs on it without
  ceremony.)
- **A dedicated user.** By default a container's process runs as **root** — root *inside* the
  container, which still means: if the JVM is ever compromised, the attacker holds uid 0 in a
  filesystem that can be shared with the host. `addgroup -S` / `adduser -S` create a system account
  and `USER stockflow` switches to it before `ENTRYPOINT`. Verified on the reference image:
  `docker run --rm --entrypoint sh stockflow:1.0 -c id` → `uid=100(stockflow)`.

---

## 5. Runtime wiring: ports, volumes, environment variables

An image is inert. Three switches on `docker run` (and the equivalent keys in Compose) connect a
container to the world.

### 5.1 Ports: `-p host:container`

A container has its own network namespace; StockFlow listening on 8080 *inside* it is invisible from
your machine. `-p 8080:8080` **publishes** it: connections to the host's 8080 are forwarded to the
container's 8080. The two numbers may differ — `-p 9090:8080` serves the same app on host port 9090 —
which is how the reference Compose file publishes PostgreSQL on **host 5433** (a locally installed
Postgres usually owns 5432) while every container still talks to it on 5432.

`EXPOSE` in the Dockerfile does *not* do this. It is documentation.

### 5.2 Volumes: where data goes to survive

A container's writable layer is deleted with the container. A database inside a container with no
volume loses everything on `docker rm`. Two ways to keep data:

- **Named volume** — `-v stockflow-pgdata:/var/lib/postgresql/data`. Docker creates and manages a
  storage area called `stockflow-pgdata` and mounts it at that path. It outlives any container;
  `docker volume ls` shows it; only `docker volume rm` (or `docker compose down -v`) deletes it.
  **This is what you use for a database.**
- **Bind mount** — `-v /home/me/data:/var/lib/postgresql/data`. Mounts a specific host directory. Good
  for development (edit a config file on the host, see it in the container); worse for a database,
  because the host path is now part of your deployment.

### 5.3 Environment variables: configuration lives outside the image

The single most important rule in this course: **an image must contain no environment-specific
configuration.** Not a database URL, not a password, not a signing secret. The same `stockflow:1.0`
must run against your laptop's Postgres, a CI container, and production — differing *only* in what
you inject at start-up.

Spring Boot makes this nearly free through **relaxed binding**: any property can be supplied as an
environment variable. The documented canonical form uppercases the name, replaces dots with
underscores, and *removes* dashes (`spring.jpa.show-sql` → `SPRING_JPA_SHOWSQL`). Boot also accepts
the spelling most people actually write, with the dash as an underscore (`SPRING_JPA_SHOW_SQL`) — the
reference `compose.yaml` uses that form, and it was verified to bind (no SQL in the container's log).

| Property | Environment variable (canonical) | Also accepted |
| --- | --- | --- |
| `spring.datasource.url` | `SPRING_DATASOURCE_URL` | — |
| `spring.jpa.show-sql` | `SPRING_JPA_SHOWSQL` | `SPRING_JPA_SHOW_SQL` |
| `app.security.jwt.secret` | `APP_SECURITY_JWT_SECRET` | — |
| `app.security.admin-password` | `APP_SECURITY_ADMINPASSWORD` | `APP_SECURITY_ADMIN_PASSWORD` |

An environment variable **overrides** the value in `application.properties`, so the file keeps its
developer defaults and the container gets production values with `-e`:

```bash
docker run -d -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/stockflow \
  -e SPRING_DATASOURCE_PASSWORD=123456 \
  stockflow:1.0
```

(`host.docker.internal` is Docker Desktop's name for "the machine running Docker" — needed here
because, from inside the container, `localhost` is the container itself. On a plain Linux engine the
name does not exist unless you add `--add-host=host.docker.internal:host-gateway`. Compose removes the
need for it entirely.)

Lab 13 makes this rigorous with profiles and `@ConfigurationProperties`; the rule itself starts here.

---

## 6. Docker Compose: the whole stack, declared

### 6.1 Why

StockFlow is two processes — the app and PostgreSQL — that must start in the right order, find each
other on a network, and share configuration. Doing that with `docker run` means a script of six
commands with `--network` flags and careful ordering. **Compose** replaces the script with a file:
`compose.yaml` declares the services, and `docker compose up` makes reality match it.

Compose is not a different technology. It drives the same daemon, creates the same containers,
networks and volumes; it just does so from a declaration instead of a sequence of commands. Every
later lab that needs infrastructure (Redis in 17, RabbitMQ in 20, Prometheus and Grafana in 24) *adds
a service to this file*. Compose is taught once, here.

### 6.2 The reference `compose.yaml`, annotated

```yaml
services:

  postgres:
    image: postgres:16
    environment:
      POSTGRES_DB: stockflow          # the official image creates DB + role on FIRST start
      POSTGRES_USER: stockflow
      POSTGRES_PASSWORD: stockflow
    volumes:
      - stockflow-pgdata:/var/lib/postgresql/data     # named volume → survives `down`
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U stockflow -d stockflow"]
      interval: 5s
      timeout: 3s
      retries: 10
    ports:
      - "5433:5432"                   # host 5433 → container 5432 (optional; for psql/GUIs)

  app:
    build: .                          # build ./Dockerfile…
    image: stockflow:1.0              # …and tag the result
    depends_on:
      postgres:
        condition: service_healthy    # wait for the healthcheck, not just for "started"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/stockflow   # ← service name as host
      SPRING_DATASOURCE_USERNAME: stockflow
      SPRING_DATASOURCE_PASSWORD: stockflow
      SPRING_JPA_SHOW_SQL: "false"
      APP_SECURITY_JWT_SECRET: ${STOCKFLOW_JWT_SECRET:-<dev default>}
      APP_SECURITY_ADMIN_PASSWORD: ${STOCKFLOW_ADMIN_PASSWORD:-admin12345}
    ports:
      - "8080:8080"

volumes:
  stockflow-pgdata:
```

Four ideas, each worth a paragraph.

**Service-name DNS.** Compose puts every service on one private network and registers each service's
name in that network's DNS. The app reaches the database as `postgres:5432` — the literal service
name. No IP addresses, no `localhost` (which, inside the app's container, would be the app), no
`host.docker.internal`. Rename the service and the URL is the only thing to change.

**A healthcheck, and `depends_on` that means it.** Plain `depends_on: [postgres]` only guarantees
the Postgres *container has started* — which is several seconds before PostgreSQL is accepting
connections. The app would start, fail to connect, and crash. `condition: service_healthy` makes
Compose wait until Postgres's own `healthcheck` passes; `pg_isready` ships with Postgres and answers
exactly that question. The app's own healthcheck is inherited from the image's `HEALTHCHECK`
instruction, so `docker compose ps` shows `app=healthy` once `/actuator/health` answers — measured at
**12 seconds** after `up`.

**The named volume.** `stockflow-pgdata:` under the top-level `volumes:` key declares it; the `postgres`
service mounts it. `docker compose down` removes the containers and the network but **leaves the
volume**; `docker compose down -v` removes it too. The lab proves the difference with real stock.

**Variable interpolation for secrets.** `${STOCKFLOW_JWT_SECRET:-…}` reads a variable from your shell
(or a `.env` file next to `compose.yaml`) and falls back to the value after `:-` if it is unset. The
fallback is a *development* convenience; in any real deployment the variable is set, the `.env` file
is in `.gitignore` and `.dockerignore`, and `docker compose config` shows you exactly what will be
injected.

### 6.3 The commands

```bash
docker compose up --build       # build the app image if needed, create network+volume, start all
docker compose up -d            # same, detached
docker compose ps               # services, their state, and their HEALTH
docker compose logs -f app      # follow one service's logs
docker compose exec postgres psql -U stockflow -d stockflow    # a shell into a running service
docker compose down             # stop + remove containers and network; KEEP volumes
docker compose down -v          # …and remove the volumes (data is gone)
```

### 6.4 The proof that matters

The reference run, verbatim:

```
docker compose up -d           → app=healthy after 12s
POST /api/products/1/movements {"type":"IN","quantity":120}   → 201, quantityAfter 120
docker compose down            → volumes still present: 1
docker compose up -d           → healthy after 8s
GET  /api/products/1           → quantityOnHand=120        ← survived
docker compose down -v         → volumes left: 0
```

Two containers were destroyed and recreated in between. The number came back because it was never in
a container — it was in the volume.

---

## 7. A one-endpoint preview of Actuator: `/actuator/health`

Every "is it alive?" question in this course — the Dockerfile's `HEALTHCHECK`, Compose's health
column, and later Kubernetes probes and CI rollout gates — needs one thing from the application: an
HTTP endpoint that answers *yes* or *no* with no credentials. Spring Boot Actuator provides it, and Lab
9 adds Actuator for that one endpoint only. **Lab 15 teaches Actuator properly** — exposure rules,
custom health indicators, liveness vs. readiness, metrics. Here is the minimum you need now.

- Dependency: `spring-boot-starter-actuator`.
- Configuration: `management.endpoints.web.exposure.include=health` (health is also the default, but
  say so).
- Security: `.requestMatchers("/actuator/health").permitAll()` in the filter chain — an orchestrator
  polls it with no token.
- Response, measured on Boot 4.0.6: `{"groups":["liveness","readiness"],"status":"UP"}` with HTTP
  200, or `"DOWN"` with 503. Under the default `show-details=never` it reveals nothing else, so
  exposing it publicly leaks nothing.
- What it checks: the app is up **and** its `DataSource` can obtain a connection. So `UP` from inside
  Compose also means the app reached `postgres:5432` — which is why the Postman collection's first
  request is this endpoint with no token.

The `groups` field is Boot 4 showing you the liveness/readiness probe groups exist; what they mean, and
why an orchestrator treats them differently, is Lab 15.

---

## 8. Reading the evidence: what a good image looks like

When you have built yours, compare it against these reference measurements.

| Check | Command | Reference |
| --- | --- | --- |
| Size | `docker image inspect stockflow:1.0 --format '{{.Size}}'` | ≈132 MB compressed on Docker Desktop (≈277 MB unpacked on a Linux engine); the naive image is 395 MB by the same measure |
| Layers | `docker history stockflow:1.0` | one 64.7 MB `dependencies/` layer, one 115 KB `application/` layer |
| User | `docker run --rm --entrypoint sh stockflow:1.0 -c id` | `uid=100(stockflow)` |
| Health | `docker ps` (STATUS column) | `(healthy)` typically 10–20 s after `docker run`; the 40 s `start-period` is only the grace window before failures count |
| Cache | edit one source line, `docker build` again | `CACHED` for `dependency:go-offline` and the `dependencies/` COPY; ~12 s |
| Config | `docker compose config` | every `SPRING_*` / `APP_*` value resolved, nothing hard-coded in the image |

> **On the size column.** Three commands, three numbers, all correct: `docker image inspect` gives the
> compressed content size (≈132 MB here); `docker history` sums to the unpacked size (≈277 MB); and
> `docker images` on Docker Desktop's containerd image store lists compressed blobs *plus* the unpacked
> snapshot (≈408 MB). Compare like with like — and compare the per-layer figures, which do not move.

---

## Checklist: you are ready to build Lab 9's Part 2 when you can...

- [ ] Explain, without the phrase "lightweight VM", what a container is and what it shares with the host.
- [ ] Say what the daemon does and what a *cannot connect to the Docker daemon* error means.
- [ ] Distinguish an image from a container, and `stop` from `rm`.
- [ ] Order a Dockerfile's `COPY`/`RUN` instructions so a source change does not re-download
      dependencies — and read a build log to confirm which steps were `CACHED`.
- [ ] Explain why a multi-stage build ships a 132 MB image where a single stage ships 395 MB — and
      why the same image shows ≈277 MB on a Linux engine.
- [ ] Run the Boot 4 `tools` extract and say what is in `application/`, why `spring-boot-loader/` is
      empty, and why the entrypoint is `java -jar application.jar`.
- [ ] Justify `USER stockflow`, and verify it.
- [ ] Explain `-p 5433:5432`, why a database needs a named volume, and why `EXPOSE` publishes nothing.
- [ ] Convert `spring.datasource.url` to its environment-variable name without looking it up.
- [ ] Explain service-name DNS, why `depends_on` needs `condition: service_healthy`, and the
      difference between `down` and `down -v`.
- [ ] Say what `/actuator/health` checks and why it is public.

Now build it: **`LAB_09_DOCKER_STOCKFLOW.md`**, Part 2.
