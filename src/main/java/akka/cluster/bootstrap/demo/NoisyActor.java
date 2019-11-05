/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.cluster.bootstrap.demo;

import akka.actor.typed.Behavior;
import akka.actor.typed.PostStop;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class NoisyActor extends AbstractBehavior<String> {

    public static Behavior<String> create() {
        return Behaviors.setup(ctx -> {
            ctx.getLog().info("Noisy actor started");
            return new NoisyActor(ctx);
        });
    }

    private NoisyActor(ActorContext<String> context) {
        super(context);
    }

    @Override
    public Receive<String> createReceive() {
        return newReceiveBuilder()
                .onAnyMessage(msg -> {
                    getContext().getLog().info("Msg {}", msg);
                   return this;
                })
                .onSignal(PostStop.class, ps -> {
                    getContext().getLog().info("Noisy actor stopped");
                    return this;
                }).build();
    }


}
