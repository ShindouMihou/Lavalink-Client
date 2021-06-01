package lavalink.client.io.javacord;

import edu.umd.cs.findbugs.annotations.NonNull;
import lavalink.client.LavalinkUtil;
import lavalink.client.io.Lavalink;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.channel.server.ServerChannelDeleteEvent;
import org.javacord.api.event.connection.ReconnectEvent;
import org.javacord.api.event.server.ServerLeaveEvent;
import org.javacord.api.listener.channel.server.ServerChannelDeleteListener;
import org.javacord.api.listener.connection.ReconnectListener;
import org.javacord.api.listener.server.ServerLeaveListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.function.Function;

public class JavacordLavaLink extends Lavalink<JavacordLink> implements ReconnectListener, ServerLeaveListener, ServerChannelDeleteListener {

    private static final Logger log = LoggerFactory.getLogger(JavacordLavaLink.class);

    /** Provider may be set at a later time. */
    @Nullable
    private Function<Integer, DiscordApi> apiProvider;
    private boolean autoReconnect = true;
    private final JavacordVoiceInterceptor voiceInterceptor;

    public JavacordLavaLink(String userId, int numShards, @Nullable Function<Integer, DiscordApi> apiProvider){
        super(userId, numShards);
        this.apiProvider = apiProvider;
        this.voiceInterceptor = new JavacordVoiceInterceptor(this);
    }

    /**
     * Creates a Lavalink instance.
     * N.B: You must set the user ID before adding a node
     */
    public JavacordLavaLink(int numShards, @Nullable Function<Integer, DiscordApi> apiProvider){
        this(null, numShards, apiProvider);
    }

    public JavacordLavaLink(int numShards){
        this( numShards, null);
    }

    @SuppressWarnings("unused")
    public boolean getAutoReconnect() {
        return autoReconnect;
    }

    @SuppressWarnings("unused")
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public JavacordLink getLink(Server server){
        return getLink(server.getIdAsString());
    }

    @SuppressWarnings({"WeakerAccess", "unused"})
    @Nullable
    public JavacordLink getExistingLink(Server server){
        return getExistingLink(server.getIdAsString());
    }

    /**
     * Returns the Discord API instance with the {@code shardId shard ID}
     *
     * @param shardId the ID of the shard
     * @return the Discord API instance with the specified shard ID
     *
     * @throws IllegalStateException if the Discord API provider
     * has not been initialized or if the shard is null.
     */
    @SuppressWarnings({"WeakerAccess", "unused"})
    @NonNull
    public DiscordApi getApi(int shardId) {
        if (apiProvider == null)
            throw new IllegalStateException("The Discord API Provider is not initialized!");

        DiscordApi result = apiProvider.apply(shardId);
        if (result == null) throw new IllegalStateException("Discord API Provider returned null for shard " + shardId);

        return result;
    }
    /**
     * Returns the Discord API instance with the {@code snowflake snowflake}
     *
     * @param snowflake the snowflake of the shard.
     * @return the Discord API instance with the specified shard ID
     *
     * @throws IllegalStateException if the Discord API provider
     * has not been initialized or if the shard is null.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public DiscordApi getApiFromSnowflake(String snowflake){
        return getApi(LavalinkUtil.getShardFromSnowflake(snowflake, numShards));
    }

    @SuppressWarnings("unused")
    public void setApiProvider(@Nullable Function<Integer, DiscordApi> apiProvider){
        this.apiProvider = apiProvider;
    }

    @SuppressWarnings("unused")
    @NonNull
    public JavacordVoiceInterceptor getVoiceInterceptor() {
        return voiceInterceptor;
    }



    @Override
    protected JavacordLink buildNewLink(String guildId) {
        return new JavacordLink(this, guildId);
    }

    @Override
    public void onServerChannelDelete(ServerChannelDeleteEvent event) {
        // Unlike JDA, we don't have a dedicated event for voice channels.
        if(event.getChannel().asServerVoiceChannel().isPresent()){
            JavacordLink link = getLinksMap().get(event.getServer().getIdAsString());
            if(link == null) return;

            link.removeConnection();
        }
    }

    @Override
    public void onReconnect(ReconnectEvent event) {
        // Irony, as of Javacord 3.3.0, the bot will automatically reconnect :kokoLaugh:
        if(autoReconnect){
            getLinksMap().forEach((id, link) -> {
                try {
                    if(link.getLastChannel() != null && event.getApi().getServerById(id).isPresent()){
                        event.getApi().getServerVoiceChannelById(link.getLastChannel())
                                .ifPresent(voiceChannel -> link.connect(voiceChannel, false));
                    }
                } catch(Exception e){
                    log.error("An exception was caught while trying to reconnect link " + link, e);
                }
            });
        }
    }

    @Override
    public void onServerLeave(ServerLeaveEvent event) {
        JavacordLink link = getLinksMap().get(event.getServer().getIdAsString());
        if(link == null) return;

        link.removeConnection();
    }
}
