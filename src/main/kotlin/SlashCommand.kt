import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.requests.RestAction

private val commands: ArrayList<SlashCommand> by lazy {
    arrayListOf(
        SetupCommand
    )
}

interface SlashCommand {
    val name: String
    val description: String
    fun onCommand(event: SlashCommandInteractionEvent)
}

object SlashCommands : ListenerAdapter() {
    private fun reloadCommands(jda: JDA) {
        println("Reloading commands...")

        jda.retrieveCommands().queue { commandsToDelete ->
            whenAllDone(commandsToDelete, Command::delete, { }, {
                whenAllDone(commands, {
                    jda.upsertCommand(it.name, it.description)
                }, { }, {
                    println("Reloaded commands!")
                })
            })
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val channel = event.channel
        if (channel is PrivateChannel && channel.user?.id == Config.adminId) {
            if (event.message.contentRaw == "reload") reloadCommands(event.jda)
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        for (command in commands) {
            if (command.name == event.name) {
                command.onCommand(event)
                return
            }
        }
    }
}

private fun <I, R> whenAllDone(
    iterable: Iterable<I>,
    forEach: (I) -> RestAction<R>,
    callback: (R) -> Unit,
    whenDone: () -> Unit
) {
    if (iterable.none { true }) {
        whenDone()
        return
    }

    var ready = false
    var waiting = 0

    iterable.forEach { i ->
        waiting++

        forEach(i).queue { r ->
            callback(r)
            waiting--

            if (ready && waiting == 0) {
                ready = false

                whenDone()
            }
        }
    }

    ready = true
}
