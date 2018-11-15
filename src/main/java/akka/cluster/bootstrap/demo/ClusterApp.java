/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.cluster.bootstrap.demo;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;

import akka.actor.PoisonPill;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.MemberStatus;
import akka.management.AkkaManagement;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.stream.Materializer;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public class ClusterApp extends AllDirectives {

  ClusterApp() {
    ActorSystem system = ActorSystem.create();

    final Http http = Http.get(system);
    Materializer materializer = ActorMaterializer.create(system);
    Cluster cluster = Cluster.get(system);

    system.log().info("Started [" + system + "], cluster.selfAddress = " + cluster.selfAddress() + ")");

    AkkaManagement.get(system).start();
    ClusterBootstrap.get(system).start();

    cluster
      .subscribe(system.actorOf(Props.create(ClusterWatcher.class)), ClusterEvent.initialStateAsEvents(), ClusterEvent.ClusterDomainEvent.class);

    // create actors
    ActorRef noisyActor = createNoisyActor(system);

    final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createRoutes(system, cluster).flow(system, materializer);
    final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
            ConnectHttp.toHost("0.0.0.0", 8080), materializer);

    cluster.registerOnMemberUp(() -> {
      system.log().info("Cluster member is up!");
      noisyActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    });

  }

  private ActorRef createNoisyActor(ActorSystem system) {
      return system.actorOf(NoisyActor.props(), "NoisyActor");
  }

  private Route createRoutes(ActorSystem system, Cluster cluster) {
    Set<MemberStatus> readyStates = new HashSet<MemberStatus>();
    readyStates.add(MemberStatus.up());
    readyStates.add(MemberStatus.weaklyUp());

    return
        // only handle GET requests
        get(() -> route(
            path("ready", () -> {
                        MemberStatus selfState = cluster.selfMember().status();
                        system.log().debug("ready? clusterState:" + selfState);
                        if (readyStates.contains(cluster.selfMember().status()))
                            return complete(StatusCodes.OK);
                        else
                            return complete(StatusCodes.INTERNAL_SERVER_ERROR);
                    }
            ),
            path("alive", () ->
                    // When Akka HTTP can respond to requests, that is sufficient
                    // to consider ourselves 'live': we don't want K8s to kill us even
                    // when we're in the process of shutting down (only stop sending
                    // us traffic, which is done due to the readyness check then failing)
                    complete(StatusCodes.OK)
            ),
            path("hello", () ->
                    complete("<h1>Say hello to akka-http</h1>"))
        ));
    }

  public static void main(String[] args) {
    new ClusterApp();
  }
}

