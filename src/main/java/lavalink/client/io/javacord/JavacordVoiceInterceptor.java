package lavalink.client.io.javacord;

import lavalink.client.io.Link;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.event.server.VoiceServerUpdateEvent;
import org.javacord.api.event.server.VoiceStateUpdateEvent;
import org.javacord.api.listener.server.VoiceServerUpdateListener;
import org.javacord.api.listener.server.VoiceStateUpdateListener;
import org.javacord.core.audio.AudioConnectionImpl;
import org.json.JSONObject;

public class JavacordVoiceInterceptor implements VoiceStateUpdateListener, VoiceServerUpdateListener {

    private final JavacordLavaLink lavalink;

    public JavacordVoiceInterceptor(JavacordLavaLink lavalink){
        this.lavalink = lavalink;
    }

    @Override
    public void onVoiceServerUpdate(VoiceServerUpdateEvent event) {
        event.getServer().getAudioConnection().map(audioConnection -> (AudioConnectionImpl) audioConnection)
                .ifPresent(audioConnection -> lavalink.getLink(event.getServer()).onVoiceServerUpdate(new JSONObject().put("token", event.getToken())
                        .put("endpoint", event.getEndpoint()).put("guild_id", event.getServer().getIdAsString()), audioConnection.getSessionId()));
    }

    @Override
    public void onVoiceStateUpdate(VoiceStateUpdateEvent event) {
        ServerVoiceChannel channel = event.getChannel();
        JavacordLink link = lavalink.getLink(event.getServer());

        if(channel == null){
            // Null channel is equals to disconnected.
            if(link.getState() != Link.State.DESTROYED){
                link.onDisconnected();
            }
        } else {
            link.setChannel(channel.getIdAsString());
        }

        // Unlike JDA, Javacord doesn't return a boolean for this, somehow.
        // return link.getState() == Link.State.CONNECTED;
    }
}
