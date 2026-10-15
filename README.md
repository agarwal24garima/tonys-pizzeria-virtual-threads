git commit -m "first commit"# Tony's Pizzeria: virtual threads at scale

One small Spring Boot app with seven short demos, one for each topic of the talk. Every demo is a controller, a service and a model record, nothing else. You drive the load demos with JMeter, the rest with a browser link or a terminal command.

How to present each demo, with expected numbers and what to say, is in [`docs/DEMO-GUIDE.md`](docs/DEMO-GUIDE.md).

## What it covers

| Talk topic | Demo | Run it with |
|---|---|---|
| Spring Boot with virtual threads; performance under high concurrency | 1. Halftime rush: traditional threads vs CompletableFuture vs virtual threads | `jmeter/halftime-rush.jmx` |
| What a thread costs at 10,000 | 2. Ten thousand tickets | a browser link |
| CPU-bound vs I/O-bound workloads | 3. Dough station: CPU-heavy orders | `jmeter/cpu-bound.jmx` |
| Thread pinning and the Java 24/25 fix (JEP 491) | 4. The stuck waiter: the same `synchronized` code on Java 21 and Java 25, caught with JFR | `jmeter/pinning.jmx`, then `jfr` |
| Scoped values (final in Java 25, JEP 506) | 5. The coupon left on the counter: `ThreadLocal` vs `ScopedValue` on one reused thread | four browser links |
| Structured concurrency (preview in Java 25, JEP 505) | 6. The oven nobody switched off: `CompletableFuture` vs `StructuredTaskScope` when one task fails | browser links, and the console |
| Java Flight Recorder | 7. CCTV: hot methods, Java 25 method timing, virtual threads in a thread dump | `jcmd` and `jfr` |

The demos are numbered in the order of the talk. Demos 1, 2, 3, 5 and 6 are shown live; demos 4 and 7 are explained on the slides and are here to try at home. All of them are linked from the start page, http://localhost:8080.

## What's inside

```
tonys-pizzeria-simple/
├── pom.xml                      Spring Boot 4.1, Java 25 with --enable-preview (a java21 profile for demo 4)
├── jmeter/                      one JMeter plan per load demo
├── recordings/                  JFR recordings and thread dumps land here
├── docs/DEMO-GUIDE.md           how to present each demo
└── src/main/java/dev/pizzeria/
    ├── controller/   OrderController    ThreadsController   CpuController
    │                 PinningController  ScopedValueController  StructuredController
    ├── service/      OrderService + KitchenService   ThreadLabService   CpuService
    │                 PinningService     ScopedValueService     StructuredService
    └── model/        Order  Receipt  Result  ThreadReport  Bill  KitchenReport
```

`application.properties` turns on virtual threads for every request (`spring.threads.virtual.enabled=true`). The "traditional" endpoints hand their work to a fixed pool of 20 platform threads instead, so you can compare both in one app. There is no database and nothing else to install.

**Why `--enable-preview`?** `StructuredTaskScope` (demo 6) is still a preview API in Java 25, so the compiler, the tests and the app all run with `--enable-preview`. Classes compiled that way run on Java 25 only, not on 21 and not on 26. `ScopedValue` (demo 5) is final in Java 25 and needs no flag.

## Set up in IntelliJ IDEA

About 15 minutes the first time, mostly downloads. Do it at home, not on conference Wi-Fi.

You need:
- **IntelliJ IDEA** 2025.2 or newer. Any edition works, including the free one.
- **JDK 25** for the app.
- **JDK 21**, only if you want to try demo 4 at home, which compares Java 21 with Java 25.
- **Apache JMeter 5.6.3** for demos 1, 3 and 4.

1. **Open the project.** Unzip `tonys-pizzeria-simple.zip`. Choose **File → Open…**, select the `tonys-pizzeria-simple` folder (the one with `pom.xml`) and click **Trust Project**. Wait for Maven to finish downloading.
2. **Pick JDK 25.** **File → Project Structure… → Project → SDK**: choose JDK 25, or **Download JDK…** with version 25 and any vendor.
3. **Check the language level.** In the same window, **Project → Language level** should read *25 (Preview)*. IntelliJ takes it from the `--enable-preview` in `pom.xml`. If it reads *25* without "(Preview)", pick *25 (Preview)* by hand.
4. **Optional, for demo 4: add JDK 21.** In the SDK list choose **Download JDK…** with version 21, then switch the project SDK back to 25. Then open **Run → Edit Configurations… → Tony's Pizzeria on Java 21** and pick that JDK 21 in the **JRE** field (on the *Runner* tab).
5. **Run the tests.** Right-click `src/test/java` and choose **Run 'All Tests'**. Fourteen tests pass in a few seconds. `PinningServiceTest` is JEP 491 as a test: it expects no pinning on Java 25.
6. **Start the app.** In the run-configuration dropdown choose **Tony's Pizzeria** and click **Run**. Wait for `Started PizzeriaApplication`.
7. **Open http://localhost:8080.** Click a link in sections 1 to 3 once. It checks everything works and warms the app up. Leave demo 5 for the stage: its ThreadLocal leak needs Rossi first, then Bianchi, on a fresh start.

For demo 4, start **Both kitchens (Java 25 and Java 21)** instead. That runs the app twice: Java 25 on port 8080 and Java 21 on port 8081. The Java 21 copy is built by Maven with the `java21` profile, which leaves out demos 5 and 6 (they need Java 25) and builds into `target/java21`.

### Run configurations

| Name | Java | Port | Also |
|---|---|---|---|
| Tony's Pizzeria | 25 (project SDK) | 8080 | `--enable-preview`. Records with JFR and times every `KitchenService` method (JEP 520). |
| Tony's Pizzeria on Java 21 | 21 | 8081 | A Maven configuration: `spring-boot:run` with the `java21` profile. Records with JFR. Only for demo 4. |
| Both kitchens (Java 25 and Java 21) | both | both | Starts the two above. |

If the dropdown is empty, create an *Application* configuration with main class `dev.pizzeria.PizzeriaApplication` and the VM options from the end of the troubleshooting section.

### Install JMeter

Download **Apache JMeter 5.6.3** (the *Binaries* zip or tgz) from https://jmeter.apache.org/download_jmeter.cgi and unzip it. Start it with `bin/jmeter` (macOS, Linux) or `bin\jmeter.bat` (Windows), then **File → Open…** a plan from the `jmeter/` folder. JMeter 5.6.3 is tested up to Java 21. If its window doesn't open on Java 25, set `JAVA_HOME` to a JDK 21 before you start it.

## Without IntelliJ

```bash
mvn package
mkdir -p recordings
# Java 25, port 8080
java --enable-preview -XX:StartFlightRecording:filename=recordings/java25.jfr,settings=profile,dumponexit=true,method-timing=dev.pizzeria.service.KitchenService \
     -jar target/tonys-pizzeria-1.0.0.jar
# Java 21, port 8081 (demo 4 only), in a second terminal
JAVA_HOME=/path/to/jdk-21 mvn -Pjava21 spring-boot:run
```

## Troubleshooting

| Problem | Fix |
|---|---|
| `Unknown argument 'method-timing'` when starting **Tony's Pizzeria** | The project SDK is older than Java 25. Pick JDK 25 (setup step 2). |
| `StructuredTaskScope is a preview API and is disabled by default` | Set the language level to *25 (Preview)* (setup step 3), and check that the VM options start with `--enable-preview`. |
| `UnsupportedClassVersionError ... compiled with preview features that are unsupported` | The app was compiled on Java 25 with preview features and must run on Java 25 exactly. Pick JDK 25 for the run configuration. |
| `Port 8080 was already in use` | Stop the other app, or add `--server.port=9090` to the program arguments and change `PORT` in JMeter. |
| **Tony's Pizzeria on Java 21** shows *JDK '21' not found* | Do setup step 4. |
| Demo 4: the Java 21 groups fail with *Connection refused* | Start **Both kitchens**, not just Tony's Pizzeria. |
| Red imports in IntelliJ | **Maven** tool window → **Reload All Maven Projects**. |
| Dependencies don't download | Behind a corporate proxy: **Settings → Build, Execution, Deployment → Build Tools → Maven** and point *User settings file* at a `settings.xml` with your proxy or mirror. |
| JMeter shows `Too many open files` | Run `ulimit -n 10240` in the terminal, then start JMeter from that terminal. |
| *10,000 platform threads* stops early on macOS | Expected. macOS caps threads per process; the result shows when the operating system refused. That is the point of the demo. |
| The first run is slower than the guide | The JVM warms up on its first requests. Click through the start page once first. |

Run configuration VM options, if you create them by hand:
- Tony's Pizzeria: `--enable-preview -XX:StartFlightRecording:filename=recordings/java25.jfr,settings=profile,dumponexit=true,method-timing=dev.pizzeria.service.KitchenService`
- Tony's Pizzeria on Java 21: a *Maven* configuration with the command `spring-boot:run -Pjava21` and JRE set to JDK 21. The port and the JFR recording come from the profile in `pom.xml`.
