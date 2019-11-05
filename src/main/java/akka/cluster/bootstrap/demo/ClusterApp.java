/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.cluster.bootstrap.demo;

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.ClusterEvent;
import akka.cluster.MemberStatus;
import akka.cluster.typed.Cluster;
import akka.cluster.typed.Subscribe;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import akka.stream.Materializer;
import akka.stream.javadsl.Flow;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletionStage;

public class ClusterApp extends AllDirectives {

  private ClusterApp() {
    ActorSystem system = ActorSystem.create(Behaviors.setup(context -> {
        akka.actor.ActorSystem classicSystem = Adapter.toClassic(context.getSystem());
        Cluster cluster = Cluster.get(context.getSystem());
        context.getLog().info("Started [" + context.getSystem() + "], cluster.selfAddress = " + cluster.selfMember().address() + ")");
        final Http http = Http.get(classicSystem);
        Materializer materializer = Materializer.createMaterializer(classicSystem);
        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = createRoutes(classicSystem, cluster).flow(classicSystem, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost("0.0.0.0", 8080), materializer);

        context.spawn(NoisyActor.create(), "NoisyActor");
        ActorRef<ClusterEvent.MemberEvent> clusterWatcher = context.spawn(ClusterWatcher.create(), "ClusterWatcher");

        cluster.subscriptions().tell(new Subscribe<>(clusterWatcher, ClusterEvent.MemberEvent.class));

        AkkaManagement.get(classicSystem).start();
        ClusterBootstrap.get(classicSystem).start();
        return Behaviors.empty();
    }),"ClusterApp");
  }


  private Route createRoutes(akka.actor.ActorSystem system, Cluster cluster) {
    Set<MemberStatus> readyStates = new HashSet<MemberStatus>();
    readyStates.add(MemberStatus.up());
    readyStates.add(MemberStatus.weaklyUp());

    return
        // only handle GET requests
        get(() -> concat(
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

