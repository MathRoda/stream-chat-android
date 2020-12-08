package io.getstream.chat.docs.java;

import io.getstream.chat.android.client.channel.ChannelClient;
import io.getstream.chat.android.client.events.TypingStartEvent;
import io.getstream.chat.android.client.events.TypingStopEvent;
import kotlin.Unit;

public class TypingIndicators {
    private ChannelClient channelController;

    /**
     * @see <a href="https://getstream.io/chat/docs/typing_indicators/?language=java#sending-start-and-stop-typing-events">Sending Start and Stop Typing</a>
     */
    public void sendingStartAndStopTypingEvents() {
        // Sends a typing.start event at most once every two seconds
        channelController.keystroke().enqueue(result -> Unit.INSTANCE);

        // Sends the typing.stop event
        channelController.stopTyping().enqueue(result -> Unit.INSTANCE);
    }

    /**
     * @see <a href="https://getstream.io/chat/docs/typing_indicators/?language=java#receiving-typing-indicator-events">Receiving Typing Events</a>
     */
    public void receivingTypingEvents() {
        // Add typing start event handling
        channelController.subscribeFor(
                new Class[]{TypingStartEvent.class},
                event -> {
                    // Handle change
                    return Unit.INSTANCE;
                }
        );

        // Add typing stop event handling
        channelController.subscribeFor(
                new Class[]{TypingStopEvent.class},
                event -> {
                    // Handle change
                    return Unit.INSTANCE;
                }
        );
    }
}