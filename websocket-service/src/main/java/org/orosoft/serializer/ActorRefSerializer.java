package org.orosoft.serializer;

import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.ActorSystem;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;

public class ActorRefSerializer implements StreamSerializer<ActorRef> {
    private final ActorSystem actorSystem;

    public ActorRefSerializer() {
        this.actorSystem = ActorSystem.create();
    }

    @Override
    public int getTypeId() {
        return 12345; // Unique type ID for ActorRef
    }

    @Override
    public void write(@Nonnull ObjectDataOutput out, @Nonnull ActorRef actorRef) throws IOException {
        String actorPath = actorRef.path().toString();
        out.writeString(actorPath);
    }

    @Nonnull
    @Override
    public ActorRef read(@Nonnull ObjectDataInput in) throws IOException {
        // Reading the serialized form and reconstructing the ActorRef
        String actorPath = in.readString();
        ActorSelection actorSelection = actorSystem.actorSelection(actorPath);

        // Resolve the ActorRef using resolveOne() (asynchronously)
        ActorRef result = actorSelection.resolveOne(Duration.ZERO).toCompletableFuture().join();

        return result;
    }

    @Override
    public void destroy() {
        StreamSerializer.super.destroy();
    }
}
