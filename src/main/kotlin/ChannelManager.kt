import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.ThreadChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.Button
import java.util.*

object ChannelManager : ListenerAdapter() {
    private const val PLAY_BUTTON_ID = "play"
    private const val CHANNEL_NAME = "2048"
    private const val THREAD_NAME = "2048 game"

    private val threads = HashMap<Guild, HashMap<User, ThreadChannel>>()
    private val games = HashMap<ThreadChannel, Pair<Game, User>>()

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        val channel = event.channel

        if (event.button.id == PLAY_BUTTON_ID && channel is TextChannel) {
            if (!threads.contains(event.guild!!)) threads[event.guild!!] = HashMap()

            if (threads[event.guild!!]!!.contains(event.user)) {
                val thread = threads[event.guild!!]!![event.user]!!

                if (!thread.isArchived && !games[thread]!!.first.isOver) {
                    if (event.channel.asTextChannel().threadChannels.contains(thread)) {
                        event
                            .reply("You already have a game in progress! <#${thread.id}>")
                            .setEphemeral(true)
                            .queue()
                        return
                    }
                } else {
                    thread.delete().queue()
                }
            }

            val shouldCreatePrivateThread = event.guild!!.features.contains("PRIVATE_THREADS")
            channel.createThreadChannel(THREAD_NAME, shouldCreatePrivateThread).queue {
                threads[event.guild!!]!![event.user] = it
                event.reply("Created <#${it.id}> for you").setEphemeral(true).queue()
                games[it] = Pair(Game(it, event.user), event.user)
            }
        }

        if (event.channel is ThreadChannel && games.contains(event.channel as ThreadChannel)) {
            val game = games[event.channel as ThreadChannel]!!.first
            val user = games[event.channel as ThreadChannel]!!.second

            if (event.user != user) {
                event.reply("This game doesn't belong to you!").setEphemeral(true).queue()
                return
            }

            when (event.button.id) {
                "up" -> game.move(Game.Direction.UP)
                "down" -> game.move(Game.Direction.DOWN)
                "left" -> game.move(Game.Direction.LEFT)
                "right" -> game.move(Game.Direction.RIGHT)
                "exit" -> event.reply(createAskToExitMessage()).setEphemeral(true).queue()
                "confirm_exit" -> event.channel.delete().queue()
            }

            if (!event.isAcknowledged) event.deferEdit().queue()
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.channel.name == CHANNEL_NAME) {
            if (event.author != event.jda.selfUser || (event.message.contentRaw == THREAD_NAME && !event.isFromThread)) {
                event.message.delete().queue()
            }
        }
    }

    override fun onGuildReady(event: GuildReadyEvent) {
        event.guild.getTextChannelsByName(CHANNEL_NAME, true).forEach {
            it.threadChannels.forEach { thread ->
                thread.delete().queue()
            }
        }
    }

    fun <T : IReplyCallback> createChannel(event: T) {
        val channels = event.guild!!.getTextChannelsByName(CHANNEL_NAME, true)
        if (channels.isEmpty()) {
            event.guild!!.createTextChannel(CHANNEL_NAME).addPermissionOverride(
                event.guild!!.publicRole,
                EnumSet.noneOf(Permission::class.java),
                EnumSet.of(Permission.MESSAGE_SEND, Permission.CREATE_PUBLIC_THREADS, Permission.CREATE_PRIVATE_THREADS)
            ).addMemberPermissionOverride(
                event.jda.selfUser.idLong,
                EnumSet.of(Permission.MESSAGE_SEND, Permission.CREATE_PUBLIC_THREADS, Permission.CREATE_PRIVATE_THREADS),
                EnumSet.noneOf(Permission::class.java)
            ).queue {
                event.reply(
                    "Created channel <#${it.id}>.\n" +
                    "Feel free to move the channel around, but don't rename it or change its permissions."
                ).queue()

                val message = MessageBuilder()
                    .append("Welcome to the $CHANNEL_NAME channel!\n")
                    .append("To play, press the button below.\n")
                    .setActionRows(ActionRow.of(Button.success(PLAY_BUTTON_ID, "\u25B6 Play")))
                    .build()

                it.sendMessage(message).queue()
            }
        } else if (channels.size == 1){
            event.reply("There's already <#${channels[0].id}> in this server").queue()
        } else {
            event.reply("There are already multiple channels named \"$CHANNEL_NAME\" in this server").queue()
        }
    }

    private fun createAskToExitMessage() = MessageBuilder()
        .append("Click the button below if you really wanna to exit the game")
        .setActionRows(ActionRow.of(Button.danger("confirm_exit", "Exit")))
        .build()
}
