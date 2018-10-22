/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.cluster.bootstrap.demo;

import akka.actor.AbstractLoggingActor;
import akka.actor.Props;

public class NoisyActor extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(NoisyActor.class, NoisyActor::new);
    }

    @Override
    public void preStart() {
        log().info("Noisy singleton started");
    }

    @Override
    public void postStop() {
        log().info("Noisy singleton stopped");
    }

    @Override
    public AbstractLoggingActor.Receive createReceive() {
        return receiveBuilder()
            .matchAny(msg -> log().info("Msg: {}" + msg))
                .build();
    }

}
