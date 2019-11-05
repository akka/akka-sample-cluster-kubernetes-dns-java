/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.cluster.bootstrap.demo;

import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.cluster.ClusterEvent;

public class ClusterWatcher extends AbstractBehavior<ClusterEvent.MemberEvent> {

    public static Behavior<ClusterEvent.MemberEvent> create() {
        return Behaviors.setup(ClusterWatcher::new);
    }

    private ClusterWatcher(ActorContext<ClusterEvent.MemberEvent> context) {
        super(context);
    }

    @Override
    public Receive<ClusterEvent.MemberEvent> createReceive() {
        return newReceiveBuilder()
                .onAnyMessage(m -> {
                    getContext().getLog().info("Member event: {}", m);
                    return this;
                }).build();
    }
}
