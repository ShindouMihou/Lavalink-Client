package lavalink.client.io.javacord;

import edu.umd.cs.findbugs.annotations.NonNull;
import lavalink.client.io.GuildUnavailableException;
import lavalink.client.io.Link;
import org.javacord.api.DiscordApi;
import org.javacord.api.audio.AudioConnection;
import org.javacord.api.entity.channel.ServerVoiceChannel;
import org.javacord.api.entity.permission.PermissionType;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavacordLink extends Link {


    private static final Logger log = LoggerFactory.getLogger(JavacordLink.class);
    private final JavacordLavaLink lavalink;

    JavacordLink(JavacordLavaLink lavaLink, String guildId){
        super(lavaLink, guildId);
        this.lavalink = lavaLink;
    }

    public void connect(@NonNull ServerVoiceChannel voiceChannel) {
        connect(voiceChannel, true);
    }

    @SuppressWarnings("WeakerAccess")
    void connect(@NonNull ServerVoiceChannel channel, boolean checkChannel) {
        if(channel.getServer().getId() != guild)
            throw new IllegalArgumentException("The provided server voice channel is not part of the server this AudioManager handles. " +
                    "Please provide a server voice channel from the proper guild.");

        if(channel.getApi().getUnavailableServers().stream().anyMatch(aLong -> aLong == guild))
            throw new GuildUnavailableException("Unable to open an audio connection with an unavailable server. " +
                    "Please wait until this guild is available to open a connection.");

        final User self = channel.getApi().getYourself();
        if(!channel.hasPermissions(self, PermissionType.CONNECT, PermissionType.MOVE_MEMBERS))
            throw new IllegalStateException("Missing Permission: " + PermissionType.CONNECT + " on channel: " + channel.getId());

        if(!channel.getServer().getAudioConnection().isPresent()) return;

        AudioConnection voiceState = channel.getServer().getAudioConnection().get();
        if(checkChannel && channel.getId() == voiceState.getChannel().getId())
            return;

        if(voiceState.getChannel().isConnected(self)){
            final int userLimit = channel.getUserLimit().orElse(0);
            if(!channel.getServer().isOwner(self) && !channel.getServer().hasPermission(self, PermissionType.ADMINISTRATOR)){
                if(userLimit > 0 && userLimit <= channel.getConnectedUserIds().size() && !channel.hasPermission(self, PermissionType.MOVE_MEMBERS))
                    throw new IllegalStateException("Unable to connect to voice channel due to user limit! Missing permission: " + PermissionType.MOVE_MEMBERS + " on channel: " + channel.getId() + " which is used for bypassing user limit!");
            }
        }

        setState(State.CONNECTING);
        queueAudioConnect(channel.getId());
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    public DiscordApi getApi() {
        return lavalink.getApiFromSnowflake(String.valueOf(guild));
    }

    @Override
    protected void removeConnection() {
    }

    @Override
    protected void queueAudioDisconnect() {
        if(getApi().getServerById(guild).isPresent()){
            getApi().getServerById(guild).get().getAudioConnection().ifPresent(AudioConnection::close);
        } else {
            log.warn("Attempted to disconnect, but guild {} was not found.", guild);
        }
    }

    @Override
    protected void queueAudioConnect(long channelId) {
        if(getApi().getServerVoiceChannelById(channelId).isPresent()){
            getApi().getServerVoiceChannelById(channelId).get().connect();
        } else {
            log.warn("Attempted ot connect, but voice channel {} was not found.", channelId);
        }
    }
}
