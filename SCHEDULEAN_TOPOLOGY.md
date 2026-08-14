# Schedulean — Canonical Runtime Topology

This file is the authoritative description of the intended runtime topology and role semantics.

## 1. Core Model

A Schedulean node can perform normal Worker responsibilities and can also execute Schedulean's own internal housekeeping jobs.

External Business Executors receive business job orders from Schedulean through NATS and do not need Schedulean installed.

## 2. Schedulean-Worker

A Worker:

- determines when business jobs should execute;
- creates/sends business job orders;
- sends orders through NATS JetStream;
- monitors progress;
- handles retries/recovery;
- participates in the cluster;
- can become a Master.

Conceptually:

    Schedulean-Worker
          |
          v
    NATS JetStream
          |
          v
    Business Executor

The Worker coordinates business execution; it does not execute the business-domain handler.

## 3. Schedulean-Master

A Master is a Worker plus the Master role:

    Schedulean-Master
        =
    Schedulean-Worker
        +
    Master capability

A Master continues normal Worker work.

Additional responsibilities include:

- orphan detection/removal;
- zombie detection;
- recovery;
- cleanup;
- other internal housekeeping.

A Master is not a dedicated maintenance-only server.

## 4. Schedulean Executor

A Schedulean node has execution capability for Schedulean internal jobs.

Housekeeping flow:

    Schedulean-Master
          |
          | housekeeping becomes due
          v
       ShedLock
          |
          v
    create housekeeping Job Order
          |
          v
    NATS JetStream
          |
          v
    Schedulean Executor
          |
          v
    housekeeping operation

The producing Master may consume and execute the job itself.

Producer and executor may be the same node.

## 5. Business Executor

Business Executors are external applications.

They listen to NATS using the Schedulean listener library.

They may run:

- in the same infrastructure as Schedulean;
- in another cluster;
- with no Schedulean nodes at all.

The executor registers/advertises handlers such as:

    invoice.generate
    payment.process
    report.generate

The exact listener API is an open design decision.

## 6. Business Job Flow

    Schedulean-Worker
          |
          | determine job is due
          v
    JobRun / JobAttempt
          |
          v
    NATS JetStream
          |
          v
    Business Executor
          |
          v
    business logic
          |
          v
    progress/status
          |
          v
    Schedulean

Schedulean remains responsible for durable scheduling/job state, retries, and recovery.

## 7. Housekeeping Job Flow

    Schedulean-Master
          |
          | housekeeping becomes due
          v
      ShedLock / required coordination
          |
          v
    produce housekeeping Job Order
          |
          v
    NATS JetStream
          |
          v
    Schedulean Executor
          |
          v
    execute housekeeping
          |
          v
    durable completion/update

Do NOT replace this with a dedicated housekeeping server.

## 8. Why Multiple Masters Exist

The main purpose of multiple Masters is to avoid all Schedulean nodes independently performing expensive periodic control-plane database work.

Example:

    100 Schedulean nodes
    masterCount = 3

means approximately:

    3 Master-capable Workers
    97 normal Workers

The three Masters still perform normal Worker work.

Multiple Masters primarily reduce/distribute control-plane housekeeping responsibility and remove the single-master housekeeping availability gap.

## 9. Master Failure

Suppose:

    Master A
    Master B
    Master C

and B dies.

Desired behavior:

    A = Master
    B = dead
    C = Master

During replacement:

- A and C continue normal Worker work;
- A and C continue housekeeping;
- housekeeping does not wait for a replacement Master;
- an eligible Worker can later acquire the vacant Master role.

For example:

    Worker D
       |
       v
    Master D

Result:

    Master A
    Master C
    Master D

The cluster converges back to desired masterCount.

## 10. Dynamic Master Count

`masterCount` is persistent runtime desired state.

Examples:

    3 -> 5
    5 -> 2
    2 -> 10

No application restart should be required merely to change the desired count.

Desired vs observed:

    desiredMasterCount = 5
    activeMasters = 4

The cluster reconciles 4 -> 5.

Likewise:

    desiredMasterCount = 3
    activeMasters = 4

should eventually gracefully demote one Master.

Do not abruptly terminate active work solely because desired count decreased.

## 11. Multi-Cluster Example

NATS may connect Schedulean clusters:

                         NATS
                    (in/out clusters)
                           |
        +------------------+------------------+
        |                  |                  |
        v                  v                  v

    Cluster 1          Cluster 2          Cluster 3

    Master 1           Master 3           no Masters
    Master 2           Worker 4           Worker 6
    Worker 1           Worker 5           Worker 7
    Worker 2
    Worker 3

Each Schedulean cluster may also contain Business Executors.

## 12. Executor-Only Cluster

A deployment may contain no Schedulean at all:

    Cluster 4

    no Schedulean

    Executor 1
    Executor 2
    ...
    Executor N

Executors listen to NATS using the Schedulean listener library.

Therefore Business Executor deployment is independent from Schedulean deployment.

## 13. Canonical Example

    NATS
      |
      +------------------------------------------------+
      |                                                |
      v                                                v

    Schedulean Cluster 1                         Schedulean Cluster 2
    --------------------                         --------------------
    Master/Worker A                              Master/Worker C
    Master/Worker B                              Worker D
    Worker E                                     Worker F
    Worker G

    Business Executors                           Business Executors
    Executor 1                                   Executor 1
    Executor 2                                   Executor 2
    Executor N                                   Executor N

      |
      +----------------------+
                             |
                             v

                    Executor-only Cluster
                    ---------------------
                    no Schedulean
                    Executor X
                    Executor Y
                    Executor Z

## 14. Role Summary

| Role | Business scheduling | Business-domain execution | Housekeeping initiation | Schedulean housekeeping execution |
|---|---:|---:|---:|---:|
| Schedulean-Worker | Yes | No | No | Not normally |
| Schedulean-Master | Yes | No | Yes | Yes |
| Business Executor | Receives orders | Yes | No | No |
| Schedulean internal Executor capability | N/A | N/A | N/A | Yes |

Important: a Schedulean-Master is still a Schedulean-Worker.

## 15. Architectural Invariants

1. A Master is also a Worker.
2. A Worker can become a Master.
3. Master role is dynamic.
4. Master count is persistent desired state.
5. Multiple Masters exist primarily to distribute/control Schedulean housekeeping.
6. Normal business scheduling continues independently of Master election state.
7. Housekeeping is represented as jobs/orders and uses the NATS execution path.
8. A Master may execute a housekeeping job it produced itself.
9. ShedLock coordinates DB mutual exclusion where needed; it is not the primary work-distribution mechanism.
10. NATS JetStream provides durable transport.
11. Durable job state remains in the persistence layer.
12. Business Executors are external and may exist without Schedulean.
13. UI is separate from Schedulean core.
14. Do not introduce a dedicated Master server unless explicitly requested.
15. Do not introduce a dedicated Housekeeping Executor service unless explicitly requested.

## 16. Open Design Decisions

These are intentionally not fixed:

- exact NATS subject hierarchy;
- exact JetStream stream/consumer configuration;
- whether housekeeping consumers are restricted to current Masters;
- Master role acquisition/release mechanism;
- exact election algorithm;
- exact longest-lived-node definition;
- lease/fencing implementation;
- propagation of dynamic masterCount;
- cluster configuration schema;
- Business Executor registration protocol;
- listener annotation/lambda API;
- cross-cluster routing;
- executor affinity/tags/priority routing;
- NATS acknowledgement ordering versus durable state transitions.

When these are discussed, treat them as design decisions rather than existing facts.
