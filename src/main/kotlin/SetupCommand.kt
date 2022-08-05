import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object SetupCommand : SlashCommand {
    override val name = "setup"
    override val description = "Setup a channel for the bot"

    override fun onCommand(event: SlashCommandInteractionEvent) {
        if (!event.isFromGuild) {
            event.reply("This command can only be used in a server").setEphemeral(true).queue()
            return
        }

        if (!event.member!!.hasPermission(Permission.MANAGE_CHANNEL)) {
            event.reply("You don't have the permission to use this command").setEphemeral(true).queue()
            return
        }

        ChannelManager.createChannel(event)
    }
}
