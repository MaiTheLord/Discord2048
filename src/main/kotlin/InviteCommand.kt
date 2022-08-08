import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object InviteCommand : SlashCommand {
    override val name = "invite"
    override val description = "add me to your server!"

    override fun onCommand(event: SlashCommandInteractionEvent) {
        event
            .reply(
                if (Config.inviteLink != null) "Click the link to add me to your server!\n<${Config.inviteLink}>"
                else "Unfortunately, I don't have an invite link. Please contact the bot owner."
            )
            .setEphemeral(true)
            .queue()
    }
}
